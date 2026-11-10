package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.ScheduleDto;
import com.springboot.simplyfly.dto.response.FlightInstanceRespDto;
import com.springboot.simplyfly.dto.response.ScheduleRespDto;
import com.springboot.simplyfly.enums.FlightStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Flight;
import com.springboot.simplyfly.model.FlightInstance;
import com.springboot.simplyfly.model.FlightSchedule;
import com.springboot.simplyfly.model.FlightSeat;
import com.springboot.simplyfly.repository.BookingJourneyRepository;
import com.springboot.simplyfly.repository.FlightInstanceRepository;
import com.springboot.simplyfly.repository.FlightScheduleRepository;
import com.springboot.simplyfly.repository.FlightSeatRepository;
import com.springboot.simplyfly.utility.SeatLayoutUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    // Maximum number of days allowed for one schedule.
    public static final int MAX_SCHEDULE_DAYS = 60;

    private final FlightScheduleRepository flightScheduleRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final FlightService flightService;
    private final RefundService refundService;
    private final NotificationService notificationService;
    private final BookingJourneyRepository bookingJourneyRepository;

    // Create a recurring schedule and generate flight instances with seats.
    @Transactional(rollbackFor = Exception.class)
    public ScheduleRespDto add(ScheduleDto dto, String airlineUsername) {

        // Get the flight owned by the logged-in airline.
        Flight flight = flightService.getOwnedFlight(dto.flightId(), airlineUsername);

        // A deactivated flight cannot be scheduled.
        if (!flight.isActive())
            throw new InvalidCallException("Flight is not active");

        // Validate the schedule dates.
        if (dto.startDate().isBefore(LocalDate.now()))
            throw new InvalidCallException("Start date cannot be in the past");

        if (dto.endDate().isBefore(dto.startDate()))
            throw new InvalidCallException("End date must be after start date");

        if (ChronoUnit.DAYS.between(dto.startDate(), dto.endDate()) >= MAX_SCHEDULE_DAYS)
            throw new InvalidCallException("Schedule can be created for max " + MAX_SCHEDULE_DAYS + " days at a time");

        boolean isDaily = dto.operatingDays() == null || dto.operatingDays().isEmpty();

        // Save the recurring schedule.
        FlightSchedule schedule = new FlightSchedule();
        schedule.setFlight(flight);
        schedule.setStartDate(dto.startDate());
        schedule.setEndDate(dto.endDate());
        schedule.setOperatingDays(isDaily ? "DAILY" :
                dto.operatingDays().stream().map(DayOfWeek::name).collect(Collectors.joining(",")));
        schedule = flightScheduleRepository.save(schedule);

        // Generate flight instances and seats for each operating date.
        List<FlightSeat> seats = new ArrayList<>();
        int count = 0;

        for (LocalDate date = dto.startDate(); !date.isAfter(dto.endDate()); date = date.plusDays(1)) {
            boolean operates = isDaily || dto.operatingDays().contains(date.getDayOfWeek());

            // Skip dates that do not operate or already have a flight instance.
            if (!operates || flightInstanceRepository.existsByFlightIdAndTravelDate(flight.getId(), date))
                continue;

            FlightInstance instance = new FlightInstance();
            instance.setFlight(flight);
            instance.setSchedule(schedule);
            instance.setTravelDate(date);
            instance.setStatus(FlightStatus.SCHEDULED);
            instance = flightInstanceRepository.save(instance);

            seats.addAll(SeatLayoutUtility.generateSeats(flight, instance));
            count++;
        }

        flightSeatRepository.saveAll(seats);

        log.info("Generated {} instances and {} seats for flight {}", count, seats.size(), flight.getFlightNumber());

        return new ScheduleRespDto(schedule.getId(), flight.getFlightNumber(), schedule.getStartDate(),
                schedule.getEndDate(), schedule.getOperatingDays(), count);
    }

    // Get flight instances belonging to the logged-in airline.
    public List<FlightInstanceRespDto> getInstances(String airlineUsername, LocalDate fromDate, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return flightInstanceRepository.getByAirlineUsername(airlineUsername,
                fromDate == null ? LocalDate.now() : fromDate, pageable);
    }

    // Get flight instances of all airlines for admin.
    public List<FlightInstanceRespDto> getAllInstances(LocalDate fromDate, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return flightInstanceRepository.getAllFromDate(fromDate == null ? LocalDate.now() : fromDate, pageable);
    }

    /*
     * Update the status of a flight instance.
     * CANCELLED -> every passenger is refunded in full automatically
     * DELAYED   -> passengers are notified (delayMinutes is optional)
     * */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(long instanceId, FlightStatus status, Integer delayMinutes, String airlineUsername) {

        FlightInstance instance = flightInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight instance not found"));

        // Check whether the flight belongs to the logged-in airline.
        if (!instance.getFlight().getAirline().getUser().getUsername().equals(airlineUsername))
            throw new OwnershipException("Operation not allowed! Flight belongs to another airline");

        // Prevent changes to arrived or cancelled flights.
        if (instance.getStatus() == FlightStatus.ARRIVED || instance.getStatus() == FlightStatus.CANCELLED)
            throw new InvalidCallException("Flight status is final and cannot be changed");

        if (delayMinutes != null && delayMinutes < 0)
            throw new InvalidCallException("Delay cannot be negative");

        instance.setStatus(status);
        instance.setDelayMinutes(status == FlightStatus.DELAYED ? delayMinutes : null);
        flightInstanceRepository.save(instance);

        // Refund everyone when cancelled, tell the passengers when delayed.
        if (status == FlightStatus.CANCELLED) {
            refundService.refundCancelledFlight(instance);
        } else if (status == FlightStatus.DELAYED) {
            String delayText = delayMinutes == null ? "." : " by " + delayMinutes + " minutes.";
            notifyPassengers(instance, "Flight delayed", "Flight " + instance.getFlight().getFlightNumber()
                    + " on " + instance.getTravelDate() + " is delayed" + delayText);
        }
    }

    /*
     * Moves the status of dated flights by time: SCHEDULED -> BOARDING -> DEPARTED -> ARRIVED.
     * Runs every minute. Old flights nobody updated become ARRIVED.
     * DELAYED and CANCELLED are never changed here, the airline sets them by hand.
     * */
    @Scheduled(fixedRate = 60000)
    public void updateStatusByTime() {
        updateStatusByTime(LocalDateTime.now());
    }

    public void updateStatusByTime(LocalDateTime now) {
        // Step 1: Flights up to today that are not finished
        List<FlightInstance> instances = flightInstanceRepository.findByTravelDateLessThanEqualAndStatusIn(
                now.toLocalDate(), List.of(FlightStatus.SCHEDULED, FlightStatus.BOARDING, FlightStatus.DEPARTED));

        // Step 2: Work out the status for the current time and save only the changed ones
        for (FlightInstance instance : instances) {
            FlightStatus newStatus = getStatusByTime(instance, now);
            if (newStatus != instance.getStatus()) {
                instance.setStatus(newStatus);
                flightInstanceRepository.save(instance);
            }
        }
    }

    private FlightStatus getStatusByTime(FlightInstance instance, LocalDateTime now) {
        Flight flight = instance.getFlight();

        LocalDateTime departure = LocalDateTime.of(instance.getTravelDate(), flight.getDepartureTime());
        LocalDateTime arrival = LocalDateTime.of(instance.getTravelDate(), flight.getArrivalTime());

        // Night flight lands after midnight
        if (arrival.isBefore(departure))
            arrival = arrival.plusDays(1);

        if (!now.isBefore(arrival))
            return FlightStatus.ARRIVED;
        if (!now.isBefore(departure))
            return FlightStatus.DEPARTED;
        if (!now.isBefore(departure.minusMinutes(45)))
            return FlightStatus.BOARDING;
        return instance.getStatus();
    }

    // Send a notification to every passenger travelling on the flight instance.
    private void notifyPassengers(FlightInstance instance, String title, String message) {
        bookingJourneyRepository.findByFlightInstanceIdAndStatusIn(instance.getId(),
                        List.of(JourneyStatus.CONFIRMED, JourneyStatus.CANCELLATION_REQUESTED))
                .forEach(journey -> notificationService.add(journey.getBooking().getPassenger().getUser(), title, message));
    }
}