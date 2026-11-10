package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.BookingDto;
import com.springboot.simplyfly.dto.request.TravellerDto;
import com.springboot.simplyfly.dto.response.AdminBookingDto;
import com.springboot.simplyfly.dto.response.AirlineBookingDto;
import com.springboot.simplyfly.dto.response.BookingRespDto;
import com.springboot.simplyfly.dto.response.FlightPassengerDto;
import com.springboot.simplyfly.dto.response.JourneyRespDto;
import com.springboot.simplyfly.enums.*;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.mapper.BookingMapper;
import com.springboot.simplyfly.model.*;
import com.springboot.simplyfly.repository.*;
import com.springboot.simplyfly.utility.FareUtility;
import com.springboot.simplyfly.utility.GovtIdUtility;
import com.springboot.simplyfly.utility.PnrUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    public static final int MAX_TRAVELLERS = 9;
    public static final int SEAT_HOLD_MINUTES = 10;
    private static final String JOURNEY = " journey";

    private final BookingRepository bookingRepository;
    private final BookingJourneyRepository bookingJourneyRepository;
    private final TravellerRepository travellerRepository;
    private final TravellerSeatRepository travellerSeatRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final PassengerService passengerService;

    /*
     * Create booking:
     * seats are HELD and booking is PENDING_PAYMENT until payment succeeds.
     * */
    @Transactional(rollbackFor = Exception.class)
    public BookingRespDto add(BookingDto dto, String passengerUsername) {
        // Step 1: Fetch logged-in passenger
        Passenger passenger = passengerService.getByUsername(passengerUsername);

        // Step 2: Validate departure (and return) flight
        FlightInstance departure = getBookableInstance(dto.departureInstanceId());
        FlightInstance returnInstance = null;
        if (dto.tripType() == TripType.ROUND_TRIP) {
            if (dto.returnInstanceId() == null)
                throw new InvalidCallException("Return flight is required for round trip");
            returnInstance = getBookableInstance(dto.returnInstanceId());
            validateRoundTrip(departure, returnInstance);
        }

        // Step 3: Validate travellers and derive category from age
        validateTravellerCount(dto.travellers(), departure.getTravelDate());
        validateGovtIds(dto.travellers(), departure.getTravelDate());
        long seatedCount = dto.travellers().stream()
                .filter(t -> FareUtility.getCategory(t.dateOfBirth(), departure.getTravelDate()) != PassengerCategory.INFANT)
                .count();
        validateSeatCount(dto.departureSeatIds(), seatedCount, "departure");
        if (returnInstance != null)
            validateSeatCount(dto.returnSeatIds(), seatedCount, "return");

        // Step 4: Create booking with unique PNR
        Booking booking = new Booking();
        booking.setPnr(generateUniquePnr());
        booking.setPassenger(passenger);
        booking.setTripType(dto.tripType());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking = bookingRepository.save(booking);

        // Step 5: Save travellers
        List<Traveller> travellers = new ArrayList<>();
        for (TravellerDto t : dto.travellers()) {
            Traveller traveller = new Traveller();
            traveller.setBooking(booking);
            traveller.setName(t.name());
            traveller.setDateOfBirth(t.dateOfBirth());
            traveller.setGender(t.gender());
            traveller.setCategory(FareUtility.getCategory(t.dateOfBirth(), departure.getTravelDate()));

            // Infant has no government ID and cannot take meal or extra baggage
            if (traveller.getCategory() == PassengerCategory.INFANT) {
                traveller.setMealOption(MealOption.NONE);
                traveller.setExtraBaggageKg(0);
            } else {
                traveller.setGovtIdType(t.govtIdType());
                traveller.setGovtIdNumber(t.govtIdNumber().trim().toUpperCase());
                traveller.setMealOption(t.mealOption() == null ? MealOption.NONE : t.mealOption());
                traveller.setExtraBaggageKg(t.extraBaggageKg() == null ? 0 : t.extraBaggageKg());
            }
            travellers.add(traveller);
        }
        travellers = travellerRepository.saveAll(travellers);

        // Step 6: Create journeys - hold seats and calculate fare
        double total = createJourney(booking, departure, JourneyType.DEPARTURE, dto.seatClass(),
                travellers, dto.departureSeatIds());
        if (returnInstance != null)
            total += createJourney(booking, returnInstance, JourneyType.RETURN, dto.seatClass(),
                    travellers, dto.returnSeatIds());

        // Step 7: Update total amount
        booking.setTotalAmount(Math.round(total * 100.0) / 100.0);
        booking = bookingRepository.save(booking);
        log.info("Booking {} created with PNR {}", booking.getId(), booking.getPnr());

        return getBookingResp(booking);
    }

    private FlightInstance getBookableInstance(Long instanceId) {
        FlightInstance instance = flightInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        if (!FlightService.BOOKABLE_STATUSES.contains(instance.getStatus()) || !instance.getFlight().isActive())
            throw new InvalidCallException("Flight is not open for booking");
        if (instance.getFlight().getAirline().getStatus() != AirlineStatus.APPROVED)
            throw new InvalidCallException("Airline is not operating currently");
        LocalDate today = LocalDate.now();
        if (instance.getTravelDate().isBefore(today)
                || (instance.getTravelDate().equals(today)
                && instance.getFlight().getDepartureTime().isBefore(LocalTime.now())))
            throw new InvalidCallException("Flight has already departed");
        return instance;
    }

    private void validateRoundTrip(FlightInstance departure, FlightInstance returnInstance) {
        if (!returnInstance.getTravelDate().isAfter(departure.getTravelDate()))
            throw new InvalidCallException("Return date must be later than departure date");
        Route out = departure.getFlight().getRoute();
        Route back = returnInstance.getFlight().getRoute();
        if (!out.getSource().getId().equals(back.getDestination().getId())
                || !out.getDestination().getId().equals(back.getSource().getId()))
            throw new InvalidCallException("Return flight must be on the reverse route");
    }

    private void validateTravellerCount(List<TravellerDto> travellers, LocalDate travelDate) {
        if (travellers.size() > MAX_TRAVELLERS)
            throw new InvalidCallException("Maximum " + MAX_TRAVELLERS + " travellers allowed per booking");
        long adults = travellers.stream()
                .filter(t -> FareUtility.getCategory(t.dateOfBirth(), travelDate) == PassengerCategory.ADULT)
                .count();
        long infants = travellers.stream()
                .filter(t -> FareUtility.getCategory(t.dateOfBirth(), travelDate) == PassengerCategory.INFANT)
                .count();
        if (adults == 0)
            throw new InvalidCallException("At least one adult is required");
        if (infants > adults)
            throw new InvalidCallException("Each infant must travel with an adult");
    }

    // Government ID is compulsory for every traveller aged 2 and above (infants are exempt).
    private void validateGovtIds(List<TravellerDto> travellers, LocalDate travelDate) {
        for (TravellerDto t : travellers) {
            if (FareUtility.getCategory(t.dateOfBirth(), travelDate) == PassengerCategory.INFANT)
                continue;

            if (t.govtIdType() == null || t.govtIdNumber() == null || t.govtIdNumber().isBlank())
                throw new InvalidCallException("Government ID is required for " + t.name() + " (age 2 and above)");

            if (!GovtIdUtility.isValid(t.govtIdType(), t.govtIdNumber()))
                throw new InvalidCallException("Invalid " + t.govtIdType() + " number for " + t.name());
        }
    }

    private void validateSeatCount(List<Long> seatIds, long required, String journey) {
        if (seatIds == null || seatIds.size() != required)
            throw new InvalidCallException("Select " + required + " seat(s) for " + journey + JOURNEY);
        if (new HashSet<>(seatIds).size() != seatIds.size())
            throw new InvalidCallException("Same seat selected twice for " + journey + JOURNEY);
    }

    private String generateUniquePnr() {
        String pnr = PnrUtility.generatePnr();
        while (bookingRepository.existsByPnr(pnr)) {
            pnr = PnrUtility.generatePnr();
        }
        return pnr;
    }

    /* Validates the selected seats, holds them and returns them in the same order as given */
    private List<FlightSeat> holdSeats(FlightInstance instance, JourneyType type,
                                       SeatClass seatClass, List<Long> seatIds) {
        // Step 1: Fetch selected seats
        Map<Long, FlightSeat> seatMap = new HashMap<>();
        flightSeatRepository.findAllById(seatIds).forEach(s -> seatMap.put(s.getId(), s));

        // Step 2: Every seat must belong to this flight, this class and be free
        List<FlightSeat> seats = new ArrayList<>();
        for (Long id : seatIds) {
            FlightSeat seat = seatMap.get(id);
            if (seat == null || !seat.getFlightInstance().getId().equals(instance.getId()))
                throw new InvalidCallException("Invalid seat selected for " + type + JOURNEY);
            if (seat.getSeatClass() != seatClass)
                throw new InvalidCallException("Seat " + seat.getSeatNumber() + " is not in " + seatClass);
            if (seat.getStatus() != SeatStatus.AVAILABLE)
                throw new InvalidCallException("Seat " + seat.getSeatNumber() + " is no longer available");
            // Step 3: Hold the seat (optimistic lock prevents double booking)
            seat.setStatus(SeatStatus.HELD);
            seats.add(seat);
        }
        flightSeatRepository.saveAll(seats);
        return seats;
    }

    /* Holds selected seats, allots them to travellers in order and returns journey amount */
    private double createJourney(Booking booking, FlightInstance instance, JourneyType type,
                                 SeatClass seatClass, List<Traveller> travellers, List<Long> seatIds) {
        // Step 1: Hold the selected seats
        List<FlightSeat> seats = holdSeats(instance, type, seatClass, seatIds);

        // Step 2: Save journey
        BookingJourney journey = new BookingJourney();
        journey.setBooking(booking);
        journey.setFlightInstance(instance);
        journey.setJourneyType(type);
        journey.setSeatClass(seatClass);
        // Journey is confirmed only after payment succeeds
        journey.setStatus(JourneyStatus.PENDING_PAYMENT);
        journey = bookingJourneyRepository.save(journey);

        // Step 3: Allot seats and calculate fare per traveller (infant gets no seat)
        Flight flight = instance.getFlight();
        Iterator<FlightSeat> seatIterator = seats.iterator();
        List<TravellerSeat> travellerSeats = new ArrayList<>();
        double amount = 0;
        for (Traveller traveller : travellers) {
            FlightSeat seat = traveller.getCategory() == PassengerCategory.INFANT ? null : seatIterator.next();
            TravellerSeat ts = new TravellerSeat();
            ts.setTraveller(traveller);
            ts.setBookingJourney(journey);
            ts.setFlightSeat(seat);
            double fare = FareUtility.calculateFare(flight, seatClass, traveller.getCategory(),
                    seat == null ? null : seat.getSeatPosition());

            // Meal and extra baggage are charged on every journey
            fare += FareUtility.calculateAddOns(traveller.getMealOption(), traveller.getExtraBaggageKg());
            ts.setFare(Math.round(fare * 100.0) / 100.0);

            // Save the fare details so the passenger can see how the fare was calculated
            ts.setBaseFare(FareUtility.getBaseFare(flight, seatClass, traveller.getCategory()));
            ts.setSeatCharge(FareUtility.getSeatCharge(flight, seat == null ? null : seat.getSeatPosition()));
            ts.setMealCharge(FareUtility.getMealPrice(traveller.getMealOption()));
            ts.setBaggageCharge(FareUtility.getBaggageCharge(traveller.getExtraBaggageKg()));
            amount += ts.getFare();
            travellerSeats.add(ts);
        }
        travellerSeatRepository.saveAll(travellerSeats);

        journey.setAmount(Math.round(amount * 100.0) / 100.0);
        bookingJourneyRepository.save(journey);
        return journey.getAmount();
    }

    public BookingRespDto getBookingResp(Booking booking) {
        List<JourneyRespDto> journeys = bookingJourneyRepository.findByBookingIdOrderByIdAsc(booking.getId())
                .stream()
                .map(j -> BookingMapper.convertJourneyToDto(j,
                        travellerSeatRepository.findByBookingJourneyIdOrderByIdAsc(j.getId())))
                .toList();
        return BookingMapper.convertEntityToDto(booking, journeys);
    }

    /* Fetch booking and make sure it belongs to logged-in passenger */
    public Booking getOwnedBooking(long bookingId, String passengerUsername) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getPassenger().getUser().getUsername().equals(passengerUsername))
            throw new OwnershipException("Operation not allowed! Booking belongs to another passenger");
        return booking;
    }

    public BookingRespDto getBookingById(long bookingId, String passengerUsername) {
        return getBookingResp(getOwnedBooking(bookingId, passengerUsername));
    }

    public List<BookingRespDto> getMyBookings(String passengerUsername) {
        return bookingRepository.getByPassengerUsername(passengerUsername)
                .stream()
                .map(this::getBookingResp)
                .toList();
    }

    /* Admin: booking history of a passenger */
    public List<BookingRespDto> getBookingsByPassengerId(long passengerId) {
        return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId)
                .stream()
                .map(this::getBookingResp)
                .toList();
    }

    public List<AirlineBookingDto> getBookingsForAirline(String airlineUsername) {
        return travellerSeatRepository.getBookingsForAirline(airlineUsername,
                List.of(BookingStatus.CONFIRMED, BookingStatus.PARTIALLY_CANCELLED, BookingStatus.CANCELLED));
    }

    // Airline views the passengers of one of its flight instances (meal, baggage and seat included).
    public List<FlightPassengerDto> getFlightPassengers(long instanceId, String airlineUsername) {
        FlightInstance instance = flightInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight instance not found"));

        if (!instance.getFlight().getAirline().getUser().getUsername().equals(airlineUsername))
            throw new OwnershipException("Operation not allowed! Flight belongs to another airline");

        return travellerSeatRepository.getPassengersByInstance(instanceId,
                List.of(JourneyStatus.CONFIRMED, JourneyStatus.CANCELLATION_REQUESTED));
    }

    public List<AdminBookingDto> getAllForAdmin(BookingStatus status, Integer page, Integer size) {
        return bookingJourneyRepository.getAllForAdmin(status, PageRequest.of(page, size));
    }

    /* Change status of all seats of a booking (used by payment) */
    public void updateSeatStatus(long bookingId, SeatStatus status) {
        List<FlightSeat> seats = travellerSeatRepository.getByBookingId(bookingId)
                .stream()
                .map(TravellerSeat::getFlightSeat)
                .filter(Objects::nonNull)
                .toList();
        seats.forEach(s -> s.setStatus(status));
        flightSeatRepository.saveAll(seats);
    }

    /* Keep every journey of a booking in step with the booking (used by payment and hold expiry) */
    public void updateJourneyStatus(long bookingId, JourneyStatus status) {
        List<BookingJourney> journeys = bookingJourneyRepository.findByBookingIdOrderByIdAsc(bookingId);
        journeys.forEach(j -> j.setStatus(status));
        bookingJourneyRepository.saveAll(journeys);
    }

    /* Change status of seats of one journey (used by refund) */
    public void updateJourneySeatStatus(long journeyId, SeatStatus status) {
        List<FlightSeat> seats = travellerSeatRepository.findByBookingJourneyIdOrderByIdAsc(journeyId)
                .stream()
                .map(TravellerSeat::getFlightSeat)
                .filter(Objects::nonNull)
                .toList();
        seats.forEach(s -> s.setStatus(status));
        flightSeatRepository.saveAll(seats);
    }

    /* Every minute: release seats of bookings not paid within SEAT_HOLD_MINUTES */
    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void releaseExpiredHolds() {
        Instant cutoff = Instant.now().minus(SEAT_HOLD_MINUTES, ChronoUnit.MINUTES);
        List<Booking> expired = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_PAYMENT, cutoff);
        for (Booking booking : expired) {
            updateSeatStatus(booking.getId(), SeatStatus.AVAILABLE);
            updateJourneyStatus(booking.getId(), JourneyStatus.EXPIRED);
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            log.info("Booking {} expired, seats released", booking.getPnr());
        }
    }
}