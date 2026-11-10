package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.BookingDto;
import com.springboot.simplyfly.dto.request.TravellerDto;
import com.springboot.simplyfly.dto.response.AdminBookingDto;
import com.springboot.simplyfly.dto.response.AirlineBookingDto;
import com.springboot.simplyfly.dto.response.BookingRespDto;
import com.springboot.simplyfly.dto.response.FlightPassengerDto;
import com.springboot.simplyfly.dto.response.TravellerSeatRespDto;
import com.springboot.simplyfly.enums.*;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.mapper.BookingMapper;
import com.springboot.simplyfly.model.*;
import com.springboot.simplyfly.repository.BookingJourneyRepository;
import com.springboot.simplyfly.repository.BookingRepository;
import com.springboot.simplyfly.repository.FlightInstanceRepository;
import com.springboot.simplyfly.repository.FlightSeatRepository;
import com.springboot.simplyfly.repository.TravellerRepository;
import com.springboot.simplyfly.repository.TravellerSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingJourneyRepository bookingJourneyRepository;

    @Mock
    private TravellerRepository travellerRepository;

    @Mock
    private TravellerSeatRepository travellerSeatRepository;

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private FlightSeatRepository flightSeatRepository;

    @Mock
    private PassengerService passengerService;

    private Passenger passenger;
    private Airport chennai;
    private Airport delhi;
    private Airline airline;
    private Flight flight;
    private Flight returnFlight;
    private FlightInstance departure;
    private FlightInstance returnInstance;
    private FlightSeat seat1;
    private FlightSeat seat2;
    private FlightSeat returnSeat;

    @BeforeEach
    void setUp() {
        User passengerUser = new User("harry", "encoded", Role.PASSENGER);
        passenger = new Passenger();
        passenger.setId(1L);
        passenger.setName("Harry");
        passenger.setUser(passengerUser);

        User airlineUser = new User("indigo", "encoded", Role.AIRLINE);
        airline = new Airline();
        airline.setId(1L);
        airline.setName("IndiGo");
        airline.setUser(airlineUser);
        airline.setStatus(AirlineStatus.APPROVED);

        chennai = new Airport();
        chennai.setId(1L);
        chennai.setCity("Chennai");
        delhi = new Airport();
        delhi.setId(2L);
        delhi.setCity("Delhi");

        // Chennai -> Delhi
        Route route = new Route();
        route.setId(1L);
        route.setSource(chennai);
        route.setDestination(delhi);
        route.setAirline(airline);
        flight = getFlight(route, "6E-201", LocalTime.of(13, 30), LocalTime.of(16, 30));

        // Delhi -> Chennai
        Route reverse = new Route();
        reverse.setId(2L);
        reverse.setSource(delhi);
        reverse.setDestination(chennai);
        reverse.setAirline(airline);
        returnFlight = getFlight(reverse, "6E-202", LocalTime.of(18, 0), LocalTime.of(21, 0));

        departure = getInstance(100L, flight, LocalDate.now().plusDays(5));
        returnInstance = getInstance(200L, returnFlight, LocalDate.now().plusDays(10));

        seat1 = getSeat(1L, "1A", SeatPosition.WINDOW, departure);
        seat2 = getSeat(2L, "1B", SeatPosition.MIDDLE, departure);
        returnSeat = getSeat(3L, "1A", SeatPosition.WINDOW, returnInstance);
    }

    private Flight getFlight(Route route, String number, LocalTime departureTime, LocalTime arrivalTime) {
        Flight f = new Flight();
        f.setFlightNumber(number);
        f.setRoute(route);
        f.setAirline(airline);
        f.setActive(true);
        f.setDepartureTime(departureTime);
        f.setArrivalTime(arrivalTime);
        f.setEconomyFare(4000);
        f.setWindowCharge(300);
        f.setAisleCharge(200);
        return f;
    }

    private FlightInstance getInstance(long id, Flight f, LocalDate date) {
        FlightInstance instance = new FlightInstance();
        instance.setId(id);
        instance.setFlight(f);
        instance.setTravelDate(date);
        instance.setStatus(FlightStatus.SCHEDULED);
        return instance;
    }

    private FlightSeat getSeat(long id, String number, SeatPosition position, FlightInstance instance) {
        FlightSeat seat = new FlightSeat();
        seat.setId(id);
        seat.setFlightInstance(instance);
        seat.setSeatNumber(number);
        seat.setSeatClass(SeatClass.ECONOMY);
        seat.setSeatPosition(position);
        seat.setStatus(SeatStatus.AVAILABLE);
        return seat;
    }

    private TravellerDto adult() {
        return new TravellerDto("Harry", LocalDate.of(1995, 5, 10), Gender.MALE, GovtIdType.AADHAAR, "123412341234", null, null);
    }

    private TravellerDto child() {
        // government ID is compulsory for a child too (age 2 and above)
        return new TravellerDto("Ginny", LocalDate.now().minusYears(5), Gender.FEMALE, GovtIdType.AADHAAR, "123412341235", null, null);
    }

    private TravellerDto infant() {
        return new TravellerDto("Baby", LocalDate.now().minusMonths(6), Gender.MALE, null, null, null, null);
    }

    private BookingDto oneWay(List<TravellerDto> travellers, List<Long> seatIds) {
        return new BookingDto(TripType.ONE_WAY, SeatClass.ECONOMY, 100L, null, travellers, seatIds, null);
    }

    private BookingDto roundTrip(List<TravellerDto> travellers, List<Long> seatIds, Long returnId, List<Long> returnSeatIds) {
        return new BookingDto(TripType.ROUND_TRIP, SeatClass.ECONOMY, 100L, returnId, travellers, seatIds, returnSeatIds);
    }

    /* Stubs needed by every booking that reaches the "create booking" step */
    private void stubBookingCreation() {
        when(bookingRepository.existsByPnr(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(travellerRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
    }

    private Booking getBooking() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setPnr("ABC123");
        booking.setPassenger(passenger);
        booking.setTripType(TripType.ONE_WAY);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(4515);
        return booking;
    }

    // ---------------------------------------------------------------- add

    @Test
    void addTest() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        stubBookingCreation();
        when(bookingJourneyRepository.save(any(BookingJourney.class))).thenAnswer(i -> i.getArgument(0));
        when(flightSeatRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(seat1, seat2));

        BookingRespDto resp = bookingService.add(oneWay(List.of(adult(), child()), List.of(1L, 2L)), "harry");

        assertEquals(BookingStatus.PENDING_PAYMENT, resp.status());
        assertEquals(TripType.ONE_WAY, resp.tripType());
        assertEquals(6, resp.pnr().length());
        // adult window (4000 + 300) * 1.05 + child middle (4000 * 0.75) * 1.05
        assertEquals(7665.0, resp.totalAmount());
        // seats are held till payment
        assertEquals(SeatStatus.HELD, seat1.getStatus());
        assertEquals(SeatStatus.HELD, seat2.getStatus());
        verify(travellerSeatRepository, times(1)).saveAll(argThat(list -> ((List<?>) list).size() == 2));
    }

    @Test
    void addTest_WithInfant() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        stubBookingCreation();
        when(bookingJourneyRepository.save(any(BookingJourney.class))).thenAnswer(i -> i.getArgument(0));
        // infant sits on lap, so only one seat is needed
        when(flightSeatRepository.findAllById(List.of(1L))).thenReturn(List.of(seat1));

        BookingRespDto resp = bookingService.add(oneWay(List.of(adult(), infant()), List.of(1L)), "harry");

        // adult window 4515 + infant (4000 * 0.10) * 1.05 = 420
        assertEquals(4935.0, resp.totalAmount());
        assertEquals(SeatStatus.HELD, seat1.getStatus());
        // both travellers are saved, even the one without a seat
        verify(travellerSeatRepository, times(1)).saveAll(argThat(list -> ((List<?>) list).size() == 2));
    }

    @Test
    void addTest_RoundTrip() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        when(flightInstanceRepository.findById(200L)).thenReturn(Optional.of(returnInstance));
        stubBookingCreation();
        when(bookingJourneyRepository.save(any(BookingJourney.class))).thenAnswer(i -> i.getArgument(0));
        when(flightSeatRepository.findAllById(List.of(1L))).thenReturn(List.of(seat1));
        when(flightSeatRepository.findAllById(List.of(3L))).thenReturn(List.of(returnSeat));

        BookingRespDto resp = bookingService.add(
                roundTrip(List.of(adult()), List.of(1L), 200L, List.of(3L)), "harry");

        // 4515 onward + 4515 return
        assertEquals(9030.0, resp.totalAmount());
        assertEquals(SeatStatus.HELD, seat1.getStatus());
        assertEquals(SeatStatus.HELD, returnSeat.getStatus());
        // one journey for each direction
        verify(bookingJourneyRepository, times(4)).save(any(BookingJourney.class));
    }

    @Test
    void addTest_DepartureFlightNotFound() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.empty());
        BookingDto dto = oneWay(List.of(adult()), List.of(1L));

        assertThrows(ResourceNotFoundException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_FlightNotOpenForBooking() {
        departure.setStatus(FlightStatus.CANCELLED);
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        BookingDto dto = oneWay(List.of(adult()), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_AirlineNotApproved() {
        airline.setStatus(AirlineStatus.SUSPENDED);
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        BookingDto dto = oneWay(List.of(adult()), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_FlightAlreadyDeparted() {
        departure.setTravelDate(LocalDate.now().minusDays(1));
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        BookingDto dto = oneWay(List.of(adult()), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_RoundTripWithoutReturnFlight() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        BookingDto dto = roundTrip(List.of(adult()), List.of(1L), null, null);

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_ReturnDateBeforeDeparture() {
        returnInstance.setTravelDate(LocalDate.now().plusDays(3));
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        when(flightInstanceRepository.findById(200L)).thenReturn(Optional.of(returnInstance));
        BookingDto dto = roundTrip(List.of(adult()), List.of(1L), 200L, List.of(3L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_ReturnFlightNotOnReverseRoute() {
        // return flight also goes Chennai -> Delhi
        FlightInstance sameDirection = getInstance(300L, flight, LocalDate.now().plusDays(10));
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        when(flightInstanceRepository.findById(300L)).thenReturn(Optional.of(sameDirection));
        BookingDto dto = roundTrip(List.of(adult()), List.of(1L), 300L, List.of(3L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_TooManyTravellers() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        // maximum is 9
        BookingDto dto = oneWay(Collections.nCopies(10, adult()), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_NoAdultTraveller() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        BookingDto dto = oneWay(List.of(child()), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_MoreInfantsThanAdults() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        BookingDto dto = oneWay(List.of(adult(), infant(), infant()), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_WrongNumberOfSeats() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        // 1 traveller but 2 seats selected
        BookingDto dto = oneWay(List.of(adult()), List.of(1L, 2L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_SameSeatSelectedTwice() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        BookingDto dto = oneWay(List.of(adult(), child()), List.of(1L, 1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_RoundTripReturnSeatsMissing() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        when(flightInstanceRepository.findById(200L)).thenReturn(Optional.of(returnInstance));
        BookingDto dto = roundTrip(List.of(adult()), List.of(1L), 200L, null);

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_SeatAlreadyBooked() {
        seat1.setStatus(SeatStatus.BOOKED);
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        stubBookingCreation();
        when(flightSeatRepository.findAllById(List.of(1L))).thenReturn(List.of(seat1));
        BookingDto dto = oneWay(List.of(adult()), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verify(bookingJourneyRepository, times(0)).save(any(BookingJourney.class));
    }

    @Test
    void addTest_SeatBelongsToOtherFlight() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        stubBookingCreation();
        // seat 3 is a seat of the return flight
        when(flightSeatRepository.findAllById(List.of(3L))).thenReturn(List.of(returnSeat));
        BookingDto dto = oneWay(List.of(adult()), List.of(3L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        assertEquals(SeatStatus.AVAILABLE, returnSeat.getStatus());
    }

    // ------------------------------------------------------- owned booking

    @Test
    void getOwnedBookingTest() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(getBooking()));

        Booking booking = bookingService.getOwnedBooking(1L, "harry");

        assertEquals("ABC123", booking.getPnr());
    }

    @Test
    void getOwnedBookingTest_NotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getOwnedBooking(99L, "harry"));
    }

    @Test
    void getOwnedBookingTest_OtherPassenger() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(getBooking()));

        assertThrows(OwnershipException.class, () -> bookingService.getOwnedBooking(1L, "ron"));
    }

    @Test
    void getBookingByIdTest() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(getBooking()));

        BookingRespDto resp = bookingService.getBookingById(1L, "harry");

        assertEquals(1L, resp.bookingId());
        assertEquals("ABC123", resp.pnr());
        assertEquals(BookingStatus.CONFIRMED, resp.status());
    }

    @Test
    void getBookingByIdTest_OtherPassenger() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(getBooking()));

        assertThrows(OwnershipException.class, () -> bookingService.getBookingById(1L, "ron"));

        verifyNoInteractions(bookingJourneyRepository);
    }

    // --------------------------------------------------------- listings

    @Test
    void getMyBookingsTest() {
        when(bookingRepository.getByPassengerUsername("harry")).thenReturn(List.of(getBooking()));

        List<BookingRespDto> list = bookingService.getMyBookings("harry");

        assertEquals(1, list.size());
        assertEquals("ABC123", list.get(0).pnr());
    }

    @Test
    void getMyBookingsTest_NoBookings() {
        when(bookingRepository.getByPassengerUsername("harry")).thenReturn(List.of());

        List<BookingRespDto> list = bookingService.getMyBookings("harry");

        assertTrue(list.isEmpty());
    }

    @Test
    void getBookingsByPassengerIdTest() {
        when(bookingRepository.findByPassengerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(getBooking()));

        List<BookingRespDto> list = bookingService.getBookingsByPassengerId(1L);

        assertEquals(1, list.size());
    }

    @Test
    void getBookingsByPassengerIdTest_NoBookings() {
        when(bookingRepository.findByPassengerIdOrderByCreatedAtDesc(99L)).thenReturn(List.of());

        List<BookingRespDto> list = bookingService.getBookingsByPassengerId(99L);

        assertTrue(list.isEmpty());
    }

    @Test
    void getBookingsForAirlineTest() {
        when(travellerSeatRepository.getBookingsForAirline(eq("indigo"), anyList()))
                .thenReturn(List.of(new AirlineBookingDto(1L, "ABC123", "Harry", PassengerCategory.ADULT,
                        LocalDate.now().plusDays(5), "6E-201", "1A", BookingStatus.CONFIRMED, JourneyStatus.CONFIRMED)));

        List<AirlineBookingDto> list = bookingService.getBookingsForAirline("indigo");

        assertEquals(1, list.size());
        assertEquals("Harry", list.get(0).travellerName());
        assertEquals("1A", list.get(0).seatNumber());
    }

    @Test
    void getBookingsForAirlineTest_NoBookings() {
        when(travellerSeatRepository.getBookingsForAirline(eq("indigo"), anyList())).thenReturn(List.of());

        List<AirlineBookingDto> list = bookingService.getBookingsForAirline("indigo");

        assertTrue(list.isEmpty());
    }

    @Test
    void getAllForAdminTest() {
        when(bookingJourneyRepository.getAllForAdmin(eq(BookingStatus.CONFIRMED), any(Pageable.class)))
                .thenReturn(List.of(new AdminBookingDto(1L, "ABC123", "Harry", "IndiGo", "6E-201", "Chennai",
                        "Delhi", LocalDate.now().plusDays(5), JourneyType.DEPARTURE, BookingStatus.CONFIRMED,
                        JourneyStatus.CONFIRMED, 4515.0)));

        List<AdminBookingDto> list = bookingService.getAllForAdmin(BookingStatus.CONFIRMED, 0, 50);

        assertEquals(1, list.size());
        assertEquals("ABC123", list.get(0).pnr());
    }

    @Test
    void getAllForAdminTest_NoMatch() {
        when(bookingJourneyRepository.getAllForAdmin(eq(BookingStatus.EXPIRED), any(Pageable.class)))
                .thenReturn(List.of());

        List<AdminBookingDto> list = bookingService.getAllForAdmin(BookingStatus.EXPIRED, 0, 50);

        assertTrue(list.isEmpty());
    }

    // ------------------------------------------------------ seat status

    @Test
    void updateSeatStatusTest() {
        TravellerSeat withSeat = new TravellerSeat();
        withSeat.setFlightSeat(seat1);
        // infant has no seat
        TravellerSeat infant = new TravellerSeat();
        infant.setFlightSeat(null);
        when(travellerSeatRepository.getByBookingId(1L)).thenReturn(List.of(withSeat, infant));

        bookingService.updateSeatStatus(1L, SeatStatus.BOOKED);

        assertEquals(SeatStatus.BOOKED, seat1.getStatus());
        verify(flightSeatRepository, times(1)).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }

    @Test
    void updateSeatStatusTest_OnlyInfants() {
        TravellerSeat infant = new TravellerSeat();
        infant.setFlightSeat(null);
        when(travellerSeatRepository.getByBookingId(1L)).thenReturn(List.of(infant));

        bookingService.updateSeatStatus(1L, SeatStatus.BOOKED);

        verify(flightSeatRepository, times(1)).saveAll(argThat(list -> ((List<?>) list).isEmpty()));
    }

    @Test
    void updateJourneySeatStatusTest() {
        seat1.setStatus(SeatStatus.BOOKED);
        TravellerSeat ts = new TravellerSeat();
        ts.setFlightSeat(seat1);
        when(travellerSeatRepository.findByBookingJourneyIdOrderByIdAsc(10L)).thenReturn(List.of(ts));

        bookingService.updateJourneySeatStatus(10L, SeatStatus.AVAILABLE);

        assertEquals(SeatStatus.AVAILABLE, seat1.getStatus());
        verify(flightSeatRepository, times(1)).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }

    @Test
    void updateJourneySeatStatusTest_NoTravellers() {
        when(travellerSeatRepository.findByBookingJourneyIdOrderByIdAsc(10L)).thenReturn(List.of());

        bookingService.updateJourneySeatStatus(10L, SeatStatus.AVAILABLE);

        verify(flightSeatRepository, times(1)).saveAll(argThat(list -> ((List<?>) list).isEmpty()));
    }

    // ------------------------------------------------- expired seat holds

    @Test
    void releaseExpiredHoldsTest() {
        Booking booking = getBooking();
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        seat1.setStatus(SeatStatus.HELD);
        TravellerSeat ts = new TravellerSeat();
        ts.setFlightSeat(seat1);
        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING_PAYMENT), any(Instant.class)))
                .thenReturn(List.of(booking));
        when(travellerSeatRepository.getByBookingId(1L)).thenReturn(List.of(ts));

        bookingService.releaseExpiredHolds();

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(SeatStatus.AVAILABLE, seat1.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void releaseExpiredHoldsTest_NothingExpired() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING_PAYMENT), any(Instant.class)))
                .thenReturn(List.of());

        bookingService.releaseExpiredHolds();

        verify(bookingRepository, times(0)).save(any(Booking.class));
        verifyNoInteractions(travellerSeatRepository);
        verifyNoInteractions(flightSeatRepository);
    }
    // ---------------------------------------------------------------- government id, add-ons

    @Test
    void addTest_ChildWithoutGovtId() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        TravellerDto childWithoutId = new TravellerDto("Ginny", LocalDate.now().minusYears(5), Gender.FEMALE, null, null, null, null);
        BookingDto dto = oneWay(List.of(adult(), childWithoutId), List.of(1L, 2L));

        InvalidCallException e = assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        assertTrue(e.getMessage().contains("Ginny"));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_InvalidGovtIdFormat() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        // Aadhaar must have 12 digits
        TravellerDto wrongId = new TravellerDto("Harry", LocalDate.of(1995, 5, 10), Gender.MALE, GovtIdType.AADHAAR, "1234", null, null);
        BookingDto dto = oneWay(List.of(wrongId), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void addTest_InfantNeedsNoGovtId() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        stubBookingCreation();
        when(bookingJourneyRepository.save(any(BookingJourney.class))).thenAnswer(i -> i.getArgument(0));
        when(flightSeatRepository.findAllById(List.of(1L))).thenReturn(List.of(seat1));

        BookingRespDto resp = bookingService.add(oneWay(List.of(adult(), infant()), List.of(1L)), "harry");

        assertEquals(BookingStatus.PENDING_PAYMENT, resp.status());
    }

    @Test
    void addTest_MealAndExtraBaggage() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        stubBookingCreation();
        when(bookingJourneyRepository.save(any(BookingJourney.class))).thenAnswer(i -> i.getArgument(0));
        when(flightSeatRepository.findAllById(List.of(1L))).thenReturn(List.of(seat1));
        TravellerDto withAddOns = new TravellerDto("Harry", LocalDate.of(1995, 5, 10), Gender.MALE,
                GovtIdType.AADHAAR, "123412341234", MealOption.VEG, 5);

        BookingRespDto resp = bookingService.add(oneWay(List.of(withAddOns), List.of(1L)), "harry");

        // seat fare (4000 + 300) * 1.05 = 4515, add-ons (300 veg meal + 5 kg * 400) * 1.05 = 2415
        assertEquals(6930.0, resp.totalAmount());
    }

    @Test
    void addTest_FareDetailsSaved() {
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        stubBookingCreation();
        when(bookingJourneyRepository.save(any(BookingJourney.class))).thenAnswer(i -> i.getArgument(0));
        when(flightSeatRepository.findAllById(List.of(1L))).thenReturn(List.of(seat1));
        TravellerDto withAddOns = new TravellerDto("Harry", LocalDate.of(1995, 5, 10), Gender.MALE,
                GovtIdType.AADHAAR, "123412341234", MealOption.VEG, 5);

        bookingService.add(oneWay(List.of(withAddOns), List.of(1L)), "harry");

        ArgumentCaptor<List<TravellerSeat>> captor = ArgumentCaptor.forClass(List.class);
        verify(travellerSeatRepository).saveAll(captor.capture());
        TravellerSeat saved = captor.getValue().get(0);

        // base 4000 + seat 300 + meal 300 + baggage 5 kg * 400 = 6600, tax 5% = 330, total 6930
        assertEquals(4000.0, saved.getBaseFare());
        assertEquals(300.0, saved.getSeatCharge());
        assertEquals(300.0, saved.getMealCharge());
        assertEquals(2000.0, saved.getBaggageCharge());

        TravellerSeatRespDto dto = BookingMapper.convertTravellerSeatToDto(saved);
        assertEquals(330.0, dto.tax());
        assertEquals(6930.0, dto.fare());
    }

    @Test
    void addTest_FlightDeactivatedByAirline() {
        flight.setActive(false);
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        BookingDto dto = oneWay(List.of(adult()), List.of(1L));

        assertThrows(InvalidCallException.class, () -> bookingService.add(dto, "harry"));

        verifyNoInteractions(bookingRepository);
    }

    // ---------------------------------------------------------------- flight passengers (airline)

    @Test
    void getFlightPassengersTest() {
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));
        when(travellerSeatRepository.getPassengersByInstance(eq(100L), anyList()))
                .thenReturn(List.of(new FlightPassengerDto("ABC123", "Harry", PassengerCategory.ADULT, Gender.MALE,
                        GovtIdType.AADHAAR, "1A", MealOption.VEG, 5, JourneyStatus.CONFIRMED)));

        List<FlightPassengerDto> list = bookingService.getFlightPassengers(100L, "indigo");

        assertEquals(1, list.size());
        assertEquals("Harry", list.get(0).travellerName());
        assertEquals(MealOption.VEG, list.get(0).mealOption());
    }

    @Test
    void getFlightPassengersTest_OtherAirline() {
        when(flightInstanceRepository.findById(100L)).thenReturn(Optional.of(departure));

        assertThrows(OwnershipException.class, () -> bookingService.getFlightPassengers(100L, "airindia"));

        verifyNoInteractions(travellerSeatRepository);
    }

    @Test
    void getFlightPassengersTest_InstanceNotFound() {
        when(flightInstanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getFlightPassengers(99L, "indigo"));
    }
}
