package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.RefundRequestDto;
import com.springboot.simplyfly.dto.response.RefundRespDto;
import com.springboot.simplyfly.enums.*;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.*;
import com.springboot.simplyfly.repository.BookingJourneyRepository;
import com.springboot.simplyfly.repository.BookingRepository;
import com.springboot.simplyfly.repository.RefundRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @InjectMocks
    private RefundService refundService;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private BookingJourneyRepository bookingJourneyRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private NotificationService notificationService;

    private BookingJourney journey;
    private Booking booking;

    @BeforeEach
    void setUp() {

        User passengerUser = new User();
        passengerUser.setUsername("harry");

        Passenger passenger = new Passenger();
        passenger.setName("Harry");
        passenger.setUser(passengerUser);

        booking = new Booking();
        booking.setId(1L);
        booking.setPnr("ABC123");
        booking.setPassenger(passenger);
        booking.setStatus(BookingStatus.CONFIRMED);

        User airlineUser = new User();
        airlineUser.setUsername("indigo");

        Airline airline = new Airline();
        airline.setName("IndiGo");
        airline.setUser(airlineUser);

        Flight flight = new Flight();
        flight.setFlightNumber("6E-201");
        flight.setAirline(airline);
        flight.setDepartureTime(LocalTime.of(13, 30));

        FlightInstance instance = new FlightInstance();
        instance.setFlight(flight);
        instance.setTravelDate(LocalDate.now().plusDays(5)); // More than 48 hrs -> 80%

        journey = new BookingJourney();
        journey.setId(10L);
        journey.setBooking(booking);
        journey.setFlightInstance(instance);
        journey.setAmount(5000);
        journey.setStatus(JourneyStatus.CONFIRMED);
    }

    // Helper method to create refund
    private Refund getRefund(RefundStatus status) {

        Refund refund = new Refund();
        refund.setBookingJourney(journey);
        refund.setReason("Plan changed");
        refund.setRefundAmount(4000);
        refund.setStatus(status);

        return refund;
    }

    // ==================== REQUEST REFUND TESTS ====================

    @Test
    void requestRefundTest() {

        when(bookingJourneyRepository.findById(10L)).thenReturn(Optional.of(journey));
        when(refundRepository.existsByBookingJourneyIdAndStatusIn(anyLong(), anyList()))
                .thenReturn(false);
        when(refundRepository.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));

        RefundRespDto dto = refundService.request(
                new RefundRequestDto(10L, "Plan changed"), "harry");

        assertEquals(RefundStatus.REQUESTED, dto.status());
        assertEquals(4000.0, dto.refundAmount());
        assertEquals(JourneyStatus.CANCELLATION_REQUESTED, journey.getStatus());

        verify(refundRepository, times(1)).save(any(Refund.class));
        verify(bookingJourneyRepository, times(1)).save(journey);
    }

    @Test
    void requestRefundTest_OtherPassenger() {

        when(bookingJourneyRepository.findById(10L)).thenReturn(Optional.of(journey));

        RefundRequestDto dto = new RefundRequestDto(10L, "Plan changed");

        assertThrows(OwnershipException.class,
                () -> refundService.request(dto, "ron"));

        verify(refundRepository, times(0)).save(any(Refund.class));
    }

    @Test
    void requestRefundTest_JourneyNotFound() {

        when(bookingJourneyRepository.findById(99L)).thenReturn(Optional.empty());

        RefundRequestDto dto = new RefundRequestDto(99L, "Plan changed");

        assertThrows(ResourceNotFoundException.class,
                () -> refundService.request(dto, "harry"));

        verify(refundRepository, times(0)).save(any(Refund.class));
    }

    @Test
    void requestRefundTest_BookingNotConfirmed() {

        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        when(bookingJourneyRepository.findById(10L)).thenReturn(Optional.of(journey));

        RefundRequestDto dto = new RefundRequestDto(10L, "Plan changed");

        assertThrows(InvalidCallException.class,
                () -> refundService.request(dto, "harry"));

        verify(refundRepository, times(0)).save(any(Refund.class));
    }

    @Test
    void requestRefundTest_JourneyAlreadyCancelled() {

        journey.setStatus(JourneyStatus.CANCELLED);

        when(bookingJourneyRepository.findById(10L)).thenReturn(Optional.of(journey));

        RefundRequestDto dto = new RefundRequestDto(10L, "Plan changed");

        assertThrows(InvalidCallException.class,
                () -> refundService.request(dto, "harry"));

        verify(refundRepository, times(0)).save(any(Refund.class));
    }

    @Test
    void requestRefundTest_AlreadyRequested() {

        when(bookingJourneyRepository.findById(10L)).thenReturn(Optional.of(journey));

        when(refundRepository.existsByBookingJourneyIdAndStatusIn(anyLong(), anyList()))
                .thenReturn(true);

        RefundRequestDto dto = new RefundRequestDto(10L, "Plan changed");

        assertThrows(InvalidCallException.class,
                () -> refundService.request(dto, "harry"));

        verify(refundRepository, times(0)).save(any(Refund.class));
        assertEquals(JourneyStatus.CONFIRMED, journey.getStatus());
    }

    @Test
    void requestRefundTest_FlightAlreadyDeparted() {

        journey.getFlightInstance().setTravelDate(LocalDate.now().minusDays(1));

        when(bookingJourneyRepository.findById(10L)).thenReturn(Optional.of(journey));

        RefundRequestDto dto = new RefundRequestDto(10L, "Plan changed");

        assertThrows(InvalidCallException.class,
                () -> refundService.request(dto, "harry"));

        verify(refundRepository, times(0)).save(any(Refund.class));
    }

    // ==================== REFUND CALCULATION TESTS ====================

    @Test
    void requestRefundTest_Between24And48Hours() {

        // Departs in 30 hours -> 50% refund
        LocalDateTime departure = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusHours(30);

        journey.getFlightInstance().setTravelDate(departure.toLocalDate());
        journey.getFlightInstance().getFlight().setDepartureTime(departure.toLocalTime());

        when(bookingJourneyRepository.findById(10L)).thenReturn(Optional.of(journey));
        when(refundRepository.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));

        RefundRespDto dto = refundService.request(
                new RefundRequestDto(10L, "Plan changed"), "harry");

        assertEquals(2500.0, dto.refundAmount());
    }

    @Test
    void requestRefundTest_LessThan24Hours() {

        // Departs in 5 hours -> No refund
        LocalDateTime departure = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusHours(5);

        journey.getFlightInstance().setTravelDate(departure.toLocalDate());
        journey.getFlightInstance().getFlight().setDepartureTime(departure.toLocalTime());

        when(bookingJourneyRepository.findById(10L)).thenReturn(Optional.of(journey));
        when(refundRepository.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));

        RefundRespDto dto = refundService.request(
                new RefundRequestDto(10L, "Plan changed"), "harry");

        assertEquals(0.0, dto.refundAmount());
        assertEquals(RefundStatus.REQUESTED, dto.status());
    }

    // ==================== FORWARD REFUND TESTS ====================

    @Test
    void forwardTest() {

        Refund refund = new Refund();
        refund.setBookingJourney(journey);
        refund.setStatus(RefundStatus.REQUESTED);

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(refund)).thenReturn(refund);

        RefundRespDto dto = refundService.forward(1L);

        assertEquals(RefundStatus.FORWARDED_TO_AIRLINE, dto.status());
    }

    @Test
    void forwardTest_NotRequested() {

        Refund refund = new Refund();
        refund.setStatus(RefundStatus.REFUNDED);

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThrows(InvalidCallException.class,
                () -> refundService.forward(1L));
    }

    @Test
    void forwardTest_RefundNotFound() {

        when(refundRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> refundService.forward(99L));
    }

    // ==================== MY REFUNDS TESTS ====================

    @Test
    void getMyRefundsTest() {

        when(refundRepository.getByPassengerUsername("harry"))
                .thenReturn(List.of(getRefund(RefundStatus.REQUESTED)));

        List<RefundRespDto> list = refundService.getMyRefunds("harry");

        assertEquals(1, list.size());
        assertEquals("ABC123", list.get(0).pnr());
        assertEquals("6E-201", list.get(0).flightNumber());
    }

    @Test
    void getMyRefundsTest_NoRefunds() {

        when(refundRepository.getByPassengerUsername("harry"))
                .thenReturn(List.of());

        List<RefundRespDto> list = refundService.getMyRefunds("harry");

        assertTrue(list.isEmpty());
    }

    // ==================== GET ALL REFUNDS TESTS ====================

    @Test
    void getAllTest_ByStatus() {

        when(refundRepository.filterByStatus(RefundStatus.REQUESTED))
                .thenReturn(List.of(getRefund(RefundStatus.REQUESTED)));

        List<RefundRespDto> list = refundService.getAll(RefundStatus.REQUESTED);

        assertEquals(1, list.size());
    }

    @Test
    void getAllTest_AllStatuses() {

        when(refundRepository.filterByStatus(null))
                .thenReturn(List.of(
                        getRefund(RefundStatus.REQUESTED),
                        getRefund(RefundStatus.REFUNDED)
                ));

        List<RefundRespDto> list = refundService.getAll(null);

        assertEquals(2, list.size());
    }

    @Test
    void getAllTest_NoRefundWithStatus() {

        when(refundRepository.filterByStatus(RefundStatus.FAILED))
                .thenReturn(List.of());

        List<RefundRespDto> list = refundService.getAll(RefundStatus.FAILED);

        assertTrue(list.isEmpty());
    }

    // ==================== AIRLINE REFUND TESTS ====================

    @Test
    void getForAirlineTest() {

        when(refundRepository.getByAirlineUsername(eq("indigo"), anyList()))
                .thenReturn(List.of(getRefund(RefundStatus.FORWARDED_TO_AIRLINE)));

        List<RefundRespDto> list = refundService.getForAirline("indigo");

        assertEquals(1, list.size());
        assertEquals("IndiGo", list.get(0).airlineName());
    }

    @Test
    void getForAirlineTest_NothingForwarded() {

        when(refundRepository.getByAirlineUsername(eq("indigo"), anyList()))
                .thenReturn(List.of());

        List<RefundRespDto> list = refundService.getForAirline("indigo");

        assertTrue(list.isEmpty());
    }

    // ==================== APPROVE REFUND TESTS ====================

    @Test
    void approveTest() {

        Refund refund = new Refund();
        refund.setBookingJourney(journey);
        refund.setStatus(RefundStatus.FORWARDED_TO_AIRLINE);

        journey.setStatus(JourneyStatus.CANCELLATION_REQUESTED);

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        when(bookingJourneyRepository.findByBookingIdOrderByIdAsc(1L))
                .thenReturn(List.of(journey));

        when(refundRepository.save(refund)).thenReturn(refund);

        RefundRespDto dto = refundService.approve(1L, "indigo");

        assertEquals(RefundStatus.REFUNDED, dto.status());
        assertEquals(JourneyStatus.CANCELLED, journey.getStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());

        verify(bookingService, times(1))
                .updateJourneySeatStatus(10L, SeatStatus.AVAILABLE);
    }

    @Test
    void approveTest_OtherAirline() {

        Refund refund = new Refund();
        refund.setBookingJourney(journey);
        refund.setStatus(RefundStatus.FORWARDED_TO_AIRLINE);

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThrows(OwnershipException.class,
                () -> refundService.approve(1L, "airindia"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void approveTest_PartialCancellation() {

        // Round trip: only one of the two journeys is cancelled
        BookingJourney returnJourney = new BookingJourney();
        returnJourney.setStatus(JourneyStatus.CONFIRMED);

        Refund refund = getRefund(RefundStatus.FORWARDED_TO_AIRLINE);

        journey.setStatus(JourneyStatus.CANCELLATION_REQUESTED);

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        when(bookingJourneyRepository.findByBookingIdOrderByIdAsc(1L))
                .thenReturn(List.of(journey, returnJourney));

        when(refundRepository.save(refund)).thenReturn(refund);

        RefundRespDto dto = refundService.approve(1L, "indigo");

        assertEquals(RefundStatus.REFUNDED, dto.status());
        assertEquals(BookingStatus.PARTIALLY_CANCELLED, booking.getStatus());

        verify(bookingService, times(1))
                .updateJourneySeatStatus(10L, SeatStatus.AVAILABLE);
    }

    @Test
    void approveTest_RefundNotFound() {

        when(refundRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> refundService.approve(99L, "indigo"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void approveTest_NotForwarded() {

        // Admin has not forwarded it yet
        Refund refund = getRefund(RefundStatus.REQUESTED);

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThrows(InvalidCallException.class,
                () -> refundService.approve(1L, "indigo"));

        verifyNoInteractions(bookingService);
        assertEquals(RefundStatus.REQUESTED, refund.getStatus());
    }

    // ==================== REJECT REFUND TESTS ====================

    @Test
    void rejectTest() {

        Refund refund = new Refund();
        refund.setBookingJourney(journey);
        refund.setStatus(RefundStatus.FORWARDED_TO_AIRLINE);

        journey.setStatus(JourneyStatus.CANCELLATION_REQUESTED);

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(refund)).thenReturn(refund);

        RefundRespDto dto = refundService.reject(
                1L, "Non refundable fare", "indigo");

        assertEquals(RefundStatus.REJECTED, dto.status());
        assertEquals("Non refundable fare", dto.rejectionReason());
        assertEquals(JourneyStatus.CONFIRMED, journey.getStatus());
    }

    @Test
    void rejectTest_ReasonMissing() {

        assertThrows(InvalidCallException.class,
                () -> refundService.reject(1L, "   ", "indigo"));

        assertThrows(InvalidCallException.class,
                () -> refundService.reject(1L, null, "indigo"));

        verifyNoInteractions(refundRepository);
    }

    @Test
    void rejectTest_OtherAirline() {

        when(refundRepository.findById(1L))
                .thenReturn(Optional.of(getRefund(RefundStatus.FORWARDED_TO_AIRLINE)));

        assertThrows(OwnershipException.class,
                () -> refundService.reject(1L, "Non refundable fare", "airindia"));

        verify(refundRepository, times(0)).save(any(Refund.class));
    }

    @Test
    void rejectTest_NotForwarded() {

        when(refundRepository.findById(1L))
                .thenReturn(Optional.of(getRefund(RefundStatus.REFUNDED)));

        assertThrows(InvalidCallException.class,
                () -> refundService.reject(1L, "Non refundable fare", "indigo"));

        verify(refundRepository, times(0)).save(any(Refund.class));
    }

    // ==================== FLIGHT CANCELLED BY AIRLINE (AUTO REFUND) TESTS ====================

    @Test
    void refundCancelledFlightTest() {

        FlightInstance instance = journey.getFlightInstance();

        when(bookingJourneyRepository.findByFlightInstanceIdAndStatusIn(any(), anyList()))
                .thenReturn(List.of(journey));
        when(refundRepository.findByBookingJourneyIdAndStatusIn(eq(10L), anyList()))
                .thenReturn(List.of());
        when(bookingJourneyRepository.findByBookingIdOrderByIdAsc(1L))
                .thenReturn(List.of(journey));

        int count = refundService.refundCancelledFlight(instance);

        assertEquals(1, count);

        // full journey amount is refunded, no 80/50/0 rule
        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository, times(1)).save(captor.capture());
        assertEquals(5000.0, captor.getValue().getRefundAmount());
        assertEquals(RefundStatus.REFUNDED, captor.getValue().getStatus());

        assertEquals(JourneyStatus.CANCELLED, journey.getStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingService, times(1)).updateJourneySeatStatus(10L, SeatStatus.AVAILABLE);

        // passenger is told by notification
        verify(notificationService, times(1)).add(any(), eq("Flight cancelled - full refund"), anyString());
    }

    @Test
    void refundCancelledFlightTest_UsesRequestedRefund() {

        Refund requested = getRefund(RefundStatus.REQUESTED);
        journey.setStatus(JourneyStatus.CANCELLATION_REQUESTED);

        when(bookingJourneyRepository.findByFlightInstanceIdAndStatusIn(any(), anyList()))
                .thenReturn(List.of(journey));
        when(refundRepository.findByBookingJourneyIdAndStatusIn(eq(10L), anyList()))
                .thenReturn(List.of(requested));
        when(bookingJourneyRepository.findByBookingIdOrderByIdAsc(1L))
                .thenReturn(List.of(journey));

        refundService.refundCancelledFlight(journey.getFlightInstance());

        // the same refund is updated from 4000 (80%) to the full 5000
        assertEquals(5000.0, requested.getRefundAmount());
        assertEquals(RefundStatus.REFUNDED, requested.getStatus());
        verify(refundRepository, times(1)).save(requested);
    }

    @Test
    void refundCancelledFlightTest_RoundTripKeepsBookingPartiallyCancelled() {

        BookingJourney returnJourney = new BookingJourney();
        returnJourney.setId(11L);
        returnJourney.setBooking(booking);
        returnJourney.setStatus(JourneyStatus.CONFIRMED);

        when(bookingJourneyRepository.findByFlightInstanceIdAndStatusIn(any(), anyList()))
                .thenReturn(List.of(journey));
        when(refundRepository.findByBookingJourneyIdAndStatusIn(eq(10L), anyList()))
                .thenReturn(List.of());
        when(bookingJourneyRepository.findByBookingIdOrderByIdAsc(1L))
                .thenReturn(List.of(journey, returnJourney));

        refundService.refundCancelledFlight(journey.getFlightInstance());

        assertEquals(BookingStatus.PARTIALLY_CANCELLED, booking.getStatus());
    }

    @Test
    void refundCancelledFlightTest_NotPaidYet() {

        journey.setStatus(JourneyStatus.PENDING_PAYMENT);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        when(bookingJourneyRepository.findByFlightInstanceIdAndStatusIn(any(), anyList()))
                .thenReturn(List.of(journey));

        int count = refundService.refundCancelledFlight(journey.getFlightInstance());

        // nothing was paid, so nothing is refunded - the booking is only released
        assertEquals(0, count);
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        verify(bookingService, times(1)).updateSeatStatus(1L, SeatStatus.AVAILABLE);
        verify(bookingService, times(1)).updateJourneyStatus(1L, JourneyStatus.EXPIRED);
        verify(refundRepository, times(0)).save(any(Refund.class));
    }

    @Test
    void refundCancelledFlightTest_NoPassengers() {

        when(bookingJourneyRepository.findByFlightInstanceIdAndStatusIn(any(), anyList()))
                .thenReturn(List.of());

        int count = refundService.refundCancelledFlight(journey.getFlightInstance());

        assertEquals(0, count);
        verifyNoInteractions(notificationService);
    }
}