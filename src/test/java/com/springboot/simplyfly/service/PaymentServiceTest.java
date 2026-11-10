package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.PaymentDto;
import com.springboot.simplyfly.dto.request.PaymentVerifyDto;
import com.springboot.simplyfly.dto.response.PaymentRespDto;
import com.springboot.simplyfly.dto.response.RazorpayOrderDto;
import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.enums.PaymentMethod;
import com.springboot.simplyfly.enums.PaymentStatus;
import com.springboot.simplyfly.enums.SeatStatus;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.model.Booking;
import com.springboot.simplyfly.model.Passenger;
import com.springboot.simplyfly.model.Payment;
import com.springboot.simplyfly.repository.BookingRepository;
import com.springboot.simplyfly.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private RazorpayService razorpayService;
    @Mock
    private NotificationService notificationService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        Passenger passenger = new Passenger();
        passenger.setName("Harry");
        booking = new Booking();
        booking.setId(1L);
        booking.setPnr("ABC123");
        booking.setPassenger(passenger);
        booking.setTotalAmount(4725);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void paySuccessTest() {
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentRespDto dto = paymentService.pay(1L, new PaymentDto(PaymentMethod.UPI, false), "harry");

        assertEquals(PaymentStatus.SUCCESS, dto.status());
        assertEquals(4725, dto.amount());
        assertNotNull(dto.transactionId());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingService, times(1)).updateSeatStatus(1L, SeatStatus.BOOKED);
        verify(bookingService, times(1)).updateJourneyStatus(1L, JourneyStatus.CONFIRMED);
        verify(bookingRepository, times(1)).save(booking);
        // passenger gets a bell notification
        verify(notificationService, times(1)).add(any(), eq("Booking confirmed"), anyString());
    }

    @Test
    void payFailureTest() {
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentRespDto dto = paymentService.pay(1L, new PaymentDto(PaymentMethod.CARD, true), "harry");

        assertEquals(PaymentStatus.FAILED, dto.status());
        assertEquals(BookingStatus.PAYMENT_FAILED, booking.getStatus());
        // held seats must be released
        verify(bookingService, times(1)).updateSeatStatus(1L, SeatStatus.AVAILABLE);
        // journeys must not stay CONFIRMED when payment fails
        verify(bookingService, times(1)).updateJourneyStatus(1L, JourneyStatus.PAYMENT_FAILED);
    }

    @Test
    void payTest_RazorpayIsOn() {
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);
        when(razorpayService.isEnabled()).thenReturn(true);

        PaymentDto dto = new PaymentDto(PaymentMethod.UPI, false);
        assertThrows(InvalidCallException.class, () -> paymentService.pay(1L, dto, "harry"));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createOrderTest() {
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);
        when(razorpayService.isEnabled()).thenReturn(true);
        when(razorpayService.createOrder(4725, "ABC123")).thenReturn("order_1");
        when(razorpayService.getKeyId()).thenReturn("rzp_test_key");

        RazorpayOrderDto dto = paymentService.createOrder(1L, "harry");

        assertTrue(dto.enabled());
        assertEquals("order_1", dto.orderId());
        assertEquals("rzp_test_key", dto.keyId());
        // amount is given to Razorpay in paise
        assertEquals(472500, dto.amountInPaise());
        // order id is saved on the booking to verify the payment later
        assertEquals("order_1", booking.getRazorpayOrderId());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void createOrderTest_RazorpayNotConfigured() {
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);
        when(razorpayService.isEnabled()).thenReturn(false);

        RazorpayOrderDto dto = paymentService.createOrder(1L, "harry");

        assertFalse(dto.enabled());
        assertNull(dto.orderId());
        verify(razorpayService, times(0)).createOrder(anyDouble(), anyString());
    }

    @Test
    void verifyTest() {
        booking.setRazorpayOrderId("order_1");
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);
        when(razorpayService.verifySignature("order_1", "pay_1", "sign_1")).thenReturn(true);
        when(razorpayService.getPaymentMethod("pay_1")).thenReturn(PaymentMethod.UPI);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentRespDto dto = paymentService.verify(1L, new PaymentVerifyDto("order_1", "pay_1", "sign_1"), "harry");

        assertEquals(PaymentStatus.SUCCESS, dto.status());
        assertEquals(PaymentMethod.UPI, dto.method());
        // Razorpay payment id is the transaction id
        assertEquals("pay_1", dto.transactionId());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingService, times(1)).updateSeatStatus(1L, SeatStatus.BOOKED);
    }

    @Test
    void verifyTest_OrderOfAnotherBooking() {
        booking.setRazorpayOrderId("order_1");
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);

        PaymentVerifyDto dto = new PaymentVerifyDto("order_other", "pay_1", "sign_1");
        assertThrows(InvalidCallException.class, () -> paymentService.verify(1L, dto, "harry"));

        verifyNoInteractions(paymentRepository);
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
    }

    @Test
    void verifyTest_WrongSignature() {
        booking.setRazorpayOrderId("order_1");
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);
        when(razorpayService.verifySignature("order_1", "pay_1", "fake")).thenReturn(false);

        PaymentVerifyDto dto = new PaymentVerifyDto("order_1", "pay_1", "fake");
        assertThrows(InvalidCallException.class, () -> paymentService.verify(1L, dto, "harry"));

        verifyNoInteractions(paymentRepository);
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
    }

    @Test
    void payTest_AlreadyConfirmed() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);

        PaymentDto dto = new PaymentDto(PaymentMethod.UPI, false);
        assertThrows(InvalidCallException.class, () -> paymentService.pay(1L, dto, "harry"));

        verifyNoInteractions(paymentRepository);
    }
}