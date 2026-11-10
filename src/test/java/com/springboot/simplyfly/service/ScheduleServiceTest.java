package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.ScheduleDto;
import com.springboot.simplyfly.dto.response.ScheduleRespDto;
import com.springboot.simplyfly.dto.response.FlightInstanceRespDto;
import com.springboot.simplyfly.enums.FlightStatus;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.Booking;
import com.springboot.simplyfly.model.BookingJourney;
import com.springboot.simplyfly.model.Passenger;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.model.Flight;
import com.springboot.simplyfly.model.FlightInstance;
import com.springboot.simplyfly.model.FlightSchedule;
import com.springboot.simplyfly.repository.BookingJourneyRepository;
import com.springboot.simplyfly.repository.FlightInstanceRepository;
import com.springboot.simplyfly.repository.FlightScheduleRepository;
import com.springboot.simplyfly.repository.FlightSeatRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @InjectMocks
    private ScheduleService scheduleService;

    @Mock
    private FlightScheduleRepository flightScheduleRepository;

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private FlightSeatRepository flightSeatRepository;

    @Mock
    private FlightService flightService;

    @Mock
    private RefundService refundService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BookingJourneyRepository bookingJourneyRepository;

    // Helper method to create flight
    private Flight getFlight() {

        Flight flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("6E-201");
        flight.setBusinessRows(1);
        flight.setPremiumRows(1);
        flight.setEconomyRows(3);
        flight.setActive(true);

        return flight;
    }

    // Helper method to create flight instance
    private FlightInstance getInstance(FlightStatus status) {

        User airlineUser = new User("indigo", "encoded", Role.AIRLINE);

        Airline airline = new Airline();
        airline.setUser(airlineUser);

        Flight flight = getFlight();
        flight.setAirline(airline);

        FlightInstance instance = new FlightInstance();
        instance.setId(100L);
        instance.setFlight(flight);
        instance.setTravelDate(LocalDate.now().plusDays(3));
        instance.setStatus(status);

        return instance;
    }

    // ==================== SCHEDULE CREATION TESTS ====================

    @Test
    void dailyScheduleTest() {

        LocalDate start = LocalDate.now().plusDays(1);

        when(flightService.getOwnedFlight(1L, "indigo")).thenReturn(getFlight());
        when(flightScheduleRepository.save(any(FlightSchedule.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(flightInstanceRepository.existsByFlightIdAndTravelDate(anyLong(), any()))
                .thenReturn(false);
        when(flightInstanceRepository.save(any(FlightInstance.class)))
                .thenAnswer(i -> i.getArgument(0));

        ScheduleRespDto resp = scheduleService.add(
                new ScheduleDto(1L, start, start.plusDays(6), null), "indigo");

        // 7 days daily -> 7 instances, each with 5 rows x 6 seats
        assertEquals(7, resp.instancesCreated());
        assertEquals("DAILY", resp.operatingDays());

        verify(flightInstanceRepository, times(7)).save(any(FlightInstance.class));

        verify(flightSeatRepository, times(1))
                .saveAll(argThat(list -> ((List<?>) list).size() == 7 * 30));
    }

    @Test
    void weekdayScheduleTest() {

        LocalDate start = LocalDate.now().plusDays(1);

        when(flightService.getOwnedFlight(1L, "indigo")).thenReturn(getFlight());
        when(flightScheduleRepository.save(any(FlightSchedule.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(flightInstanceRepository.existsByFlightIdAndTravelDate(anyLong(), any()))
                .thenReturn(false);
        when(flightInstanceRepository.save(any(FlightInstance.class)))
                .thenAnswer(i -> i.getArgument(0));

        // 14 days, only Mondays -> exactly 2 instances
        ScheduleRespDto resp = scheduleService.add(
                new ScheduleDto(1L, start, start.plusDays(13), List.of(DayOfWeek.MONDAY)),
                "indigo");

        assertEquals(2, resp.instancesCreated());
        assertEquals("MONDAY", resp.operatingDays());
    }

    @Test
    void scheduleTest_InvalidDates() {

        LocalDate start = LocalDate.now().plusDays(5);

        when(flightService.getOwnedFlight(1L, "indigo")).thenReturn(getFlight());

        ScheduleDto dto = new ScheduleDto(1L, start, start.minusDays(1), null);

        assertThrows(InvalidCallException.class,
                () -> scheduleService.add(dto, "indigo"));

        verifyNoInteractions(flightScheduleRepository);
    }

    @Test
    void scheduleTest_TooLong() {

        LocalDate start = LocalDate.now().plusDays(1);

        when(flightService.getOwnedFlight(1L, "indigo")).thenReturn(getFlight());

        ScheduleDto dto = new ScheduleDto(1L, start, start.plusDays(90), null);

        assertThrows(InvalidCallException.class,
                () -> scheduleService.add(dto, "indigo"));
    }

    // ==================== SCHEDULE VALIDATION TESTS ====================

    @Test
    void scheduleTest_StartDateInPast() {

        LocalDate yesterday = LocalDate.now().minusDays(1);

        when(flightService.getOwnedFlight(1L, "indigo")).thenReturn(getFlight());

        ScheduleDto dto = new ScheduleDto(
                1L, yesterday, LocalDate.now().plusDays(5), null);

        assertThrows(InvalidCallException.class,
                () -> scheduleService.add(dto, "indigo"));

        verifyNoInteractions(flightScheduleRepository);
    }

    @Test
    void scheduleTest_FlightOfOtherAirline() {

        LocalDate start = LocalDate.now().plusDays(1);

        when(flightService.getOwnedFlight(1L, "airindia"))
                .thenThrow(new OwnershipException(
                        "Operation not allowed! Flight belongs to another airline"));

        ScheduleDto dto = new ScheduleDto(
                1L, start, start.plusDays(5), null);

        assertThrows(OwnershipException.class,
                () -> scheduleService.add(dto, "airindia"));

        verifyNoInteractions(flightScheduleRepository);
        verifyNoInteractions(flightInstanceRepository);
    }

    @Test
    void scheduleTest_MultipleOperatingDays() {

        LocalDate start = LocalDate.now().plusDays(1);

        when(flightService.getOwnedFlight(1L, "indigo")).thenReturn(getFlight());
        when(flightScheduleRepository.save(any(FlightSchedule.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(flightInstanceRepository.existsByFlightIdAndTravelDate(anyLong(), any()))
                .thenReturn(false);
        when(flightInstanceRepository.save(any(FlightInstance.class)))
                .thenAnswer(i -> i.getArgument(0));

        // 14 days, Monday and Friday -> 2 weeks x 2 days = 4 instances
        ScheduleRespDto resp = scheduleService.add(
                new ScheduleDto(1L, start, start.plusDays(13),
                        List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
                "indigo");

        assertEquals(4, resp.instancesCreated());
        assertEquals("MONDAY,FRIDAY", resp.operatingDays());
    }

    @Test
    void scheduleTest_ExistingInstancesAreNotRegenerated() {

        LocalDate start = LocalDate.now().plusDays(1);

        when(flightService.getOwnedFlight(1L, "indigo")).thenReturn(getFlight());
        when(flightScheduleRepository.save(any(FlightSchedule.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Every date already has a dated flight with bookings
        when(flightInstanceRepository.existsByFlightIdAndTravelDate(anyLong(), any()))
                .thenReturn(true);

        ScheduleRespDto resp = scheduleService.add(
                new ScheduleDto(1L, start, start.plusDays(6), null), "indigo");

        assertEquals(0, resp.instancesCreated());

        verify(flightInstanceRepository, times(0))
                .save(any(FlightInstance.class));

        verify(flightSeatRepository, times(1))
                .saveAll(argThat(list -> ((List<?>) list).isEmpty()));
    }

    // ==================== AIRLINE FLIGHT INSTANCE TESTS ====================

    @Test
    void getInstancesTest() {

        when(flightInstanceRepository.getByAirlineUsername(
                eq("indigo"), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(List.of(
                        new FlightInstanceRespDto(
                                100L, "IndiGo", "6E-201", "Chennai", "Delhi",
                                LocalDate.now().plusDays(3),
                                LocalTime.of(13, 30),
                                LocalTime.of(16, 30),
                                FlightStatus.SCHEDULED
                        )
                ));

        // From date is optional, today is used
        List<FlightInstanceRespDto> list =
                scheduleService.getInstances("indigo", null, 0, 50);

        assertEquals(1, list.size());
        assertEquals("6E-201", list.get(0).flightNumber());
    }

    @Test
    void getInstancesTest_NoInstances() {

        LocalDate from = LocalDate.now().plusDays(1);

        when(flightInstanceRepository.getByAirlineUsername(
                eq("indigo"), eq(from), any(Pageable.class)))
                .thenReturn(List.of());

        List<FlightInstanceRespDto> list =
                scheduleService.getInstances("indigo", from, 0, 50);

        assertTrue(list.isEmpty());
    }

    // ==================== ADMIN FLIGHT INSTANCE TESTS ====================

    @Test
    void getAllInstancesTest() {

        when(flightInstanceRepository.getAllFromDate(
                any(LocalDate.class), any(Pageable.class)))
                .thenReturn(List.of(
                        new FlightInstanceRespDto(
                                100L, "IndiGo", "6E-201", "Chennai", "Delhi",
                                LocalDate.now().plusDays(3),
                                LocalTime.of(13, 30),
                                LocalTime.of(16, 30),
                                FlightStatus.SCHEDULED
                        )
                ));

        List<FlightInstanceRespDto> list =
                scheduleService.getAllInstances(null, 0, 50);

        assertEquals(1, list.size());
    }

    @Test
    void getAllInstancesTest_NoInstances() {

        when(flightInstanceRepository.getAllFromDate(
                any(LocalDate.class), any(Pageable.class)))
                .thenReturn(List.of());

        List<FlightInstanceRespDto> list =
                scheduleService.getAllInstances(null, 0, 50);

        assertTrue(list.isEmpty());
    }

    // ==================== UPDATE FLIGHT STATUS TESTS ====================

    @Test
    void updateStatusTest() {

        FlightInstance instance = getInstance(FlightStatus.SCHEDULED);

        when(flightInstanceRepository.findById(100L))
                .thenReturn(Optional.of(instance));

        scheduleService.updateStatus(100L, FlightStatus.DELAYED, 45, "indigo");

        assertEquals(FlightStatus.DELAYED, instance.getStatus());
        assertEquals(45, instance.getDelayMinutes());

        verify(flightInstanceRepository, times(1)).save(instance);
    }

    @Test
    void updateStatusTest_NotFound() {

        when(flightInstanceRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> scheduleService.updateStatus(
                        99L, FlightStatus.DELAYED, null, "indigo"));
    }

    @Test
    void updateStatusTest_OtherAirline() {

        FlightInstance instance = getInstance(FlightStatus.SCHEDULED);

        when(flightInstanceRepository.findById(100L))
                .thenReturn(Optional.of(instance));

        assertThrows(OwnershipException.class,
                () -> scheduleService.updateStatus(
                        100L, FlightStatus.CANCELLED, null, "airindia"));

        assertEquals(FlightStatus.SCHEDULED, instance.getStatus());

        verify(flightInstanceRepository, times(0))
                .save(any(FlightInstance.class));
    }

    @Test
    void updateStatusTest_ArrivedIsFinal() {

        FlightInstance instance = getInstance(FlightStatus.ARRIVED);

        when(flightInstanceRepository.findById(100L))
                .thenReturn(Optional.of(instance));

        assertThrows(InvalidCallException.class,
                () -> scheduleService.updateStatus(
                        100L, FlightStatus.DELAYED, null, "indigo"));

        verify(flightInstanceRepository, times(0))
                .save(any(FlightInstance.class));
    }

    @Test
    void updateStatusTest_CancelledIsFinal() {

        FlightInstance instance = getInstance(FlightStatus.CANCELLED);

        when(flightInstanceRepository.findById(100L))
                .thenReturn(Optional.of(instance));

        assertThrows(InvalidCallException.class,
                () -> scheduleService.updateStatus(
                        100L, FlightStatus.SCHEDULED, null, "indigo"));

        verify(flightInstanceRepository, times(0))
                .save(any(FlightInstance.class));
    }

    @Test
    void updateStatusTest_CancelRefundsPassengers() {

        FlightInstance instance = getInstance(FlightStatus.SCHEDULED);

        when(flightInstanceRepository.findById(100L))
                .thenReturn(Optional.of(instance));

        scheduleService.updateStatus(100L, FlightStatus.CANCELLED, null, "indigo");

        assertEquals(FlightStatus.CANCELLED, instance.getStatus());

        // all passengers are refunded automatically
        verify(refundService, times(1)).refundCancelledFlight(instance);
    }

    @Test
    void updateStatusTest_DelayNotifiesPassengers() {

        FlightInstance instance = getInstance(FlightStatus.SCHEDULED);

        User passengerUser = new User("harry", "encoded", Role.PASSENGER);
        Passenger passenger = new Passenger();
        passenger.setUser(passengerUser);
        Booking booking = new Booking();
        booking.setPassenger(passenger);
        BookingJourney journey = new BookingJourney();
        journey.setBooking(booking);

        when(flightInstanceRepository.findById(100L))
                .thenReturn(Optional.of(instance));
        when(bookingJourneyRepository.findByFlightInstanceIdAndStatusIn(eq(100L), anyList()))
                .thenReturn(List.of(journey));

        scheduleService.updateStatus(100L, FlightStatus.DELAYED, 30, "indigo");

        verify(notificationService, times(1)).add(eq(passengerUser), eq("Flight delayed"), anyString());
        verify(refundService, times(0)).refundCancelledFlight(any(FlightInstance.class));
    }

    @Test
    void updateStatusTest_NegativeDelay() {

        FlightInstance instance = getInstance(FlightStatus.SCHEDULED);

        when(flightInstanceRepository.findById(100L))
                .thenReturn(Optional.of(instance));

        assertThrows(InvalidCallException.class,
                () -> scheduleService.updateStatus(
                        100L, FlightStatus.DELAYED, -5, "indigo"));

        verify(flightInstanceRepository, times(0))
                .save(any(FlightInstance.class));
    }

    @Test
    void addTest_InactiveFlight() {

        Flight flight = getFlight();
        flight.setActive(false);
        LocalDate start = LocalDate.now().plusDays(1);

        when(flightService.getOwnedFlight(1L, "indigo")).thenReturn(flight);
        ScheduleDto dto = new ScheduleDto(1L, start, start.plusDays(6), null);

        assertThrows(InvalidCallException.class,
                () -> scheduleService.add(dto, "indigo"));

        verify(flightInstanceRepository, times(0)).save(any(FlightInstance.class));
    }

    // ==================== STATUS BY TIME TESTS ====================

    // Flight 10:00 - 12:00 on the given date
    private FlightInstance getTimedInstance(FlightStatus status, LocalDate date) {
        FlightInstance instance = getInstance(status);
        instance.setTravelDate(date);
        instance.getFlight().setDepartureTime(LocalTime.of(10, 0));
        instance.getFlight().setArrivalTime(LocalTime.of(12, 0));
        return instance;
    }

    @Test
    void updateStatusByTimeTest_Boarding() {
        LocalDate today = LocalDate.of(2026, 11, 7);
        FlightInstance instance = getTimedInstance(FlightStatus.SCHEDULED, today);
        when(flightInstanceRepository.findByTravelDateLessThanEqualAndStatusIn(eq(today), anyList()))
                .thenReturn(List.of(instance));

        // 30 minutes before departure
        scheduleService.updateStatusByTime(LocalDateTime.of(2026, 11, 7, 9, 30));

        assertEquals(FlightStatus.BOARDING, instance.getStatus());
        verify(flightInstanceRepository, times(1)).save(instance);
    }

    @Test
    void updateStatusByTimeTest_Departed() {
        LocalDate today = LocalDate.of(2026, 11, 7);
        FlightInstance instance = getTimedInstance(FlightStatus.BOARDING, today);
        when(flightInstanceRepository.findByTravelDateLessThanEqualAndStatusIn(eq(today), anyList()))
                .thenReturn(List.of(instance));

        scheduleService.updateStatusByTime(LocalDateTime.of(2026, 11, 7, 10, 30));

        assertEquals(FlightStatus.DEPARTED, instance.getStatus());
    }

    @Test
    void updateStatusByTimeTest_OldFlightArrived() {
        LocalDate today = LocalDate.of(2026, 11, 7);
        // nobody updated this flight of 3 days ago
        FlightInstance instance = getTimedInstance(FlightStatus.SCHEDULED, today.minusDays(3));
        when(flightInstanceRepository.findByTravelDateLessThanEqualAndStatusIn(eq(today), anyList()))
                .thenReturn(List.of(instance));

        scheduleService.updateStatusByTime(LocalDateTime.of(2026, 11, 7, 8, 0));

        assertEquals(FlightStatus.ARRIVED, instance.getStatus());
    }

    @Test
    void updateStatusByTimeTest_NoChange() {
        LocalDate today = LocalDate.of(2026, 11, 7);
        FlightInstance instance = getTimedInstance(FlightStatus.SCHEDULED, today);
        when(flightInstanceRepository.findByTravelDateLessThanEqualAndStatusIn(eq(today), anyList()))
                .thenReturn(List.of(instance));

        // 5 hours before departure
        scheduleService.updateStatusByTime(LocalDateTime.of(2026, 11, 7, 5, 0));

        assertEquals(FlightStatus.SCHEDULED, instance.getStatus());
        verify(flightInstanceRepository, times(0)).save(any(FlightInstance.class));
    }
}
