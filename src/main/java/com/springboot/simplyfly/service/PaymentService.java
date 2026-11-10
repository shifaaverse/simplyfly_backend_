package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.PaymentDto;
import com.springboot.simplyfly.dto.request.PaymentVerifyDto;
import com.springboot.simplyfly.dto.response.PaymentRespDto;
import com.springboot.simplyfly.dto.response.RazorpayOrderDto;
import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.enums.PaymentStatus;
import com.springboot.simplyfly.enums.SeatStatus;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.mapper.PaymentMapper;
import com.springboot.simplyfly.model.Booking;
import com.springboot.simplyfly.model.Payment;
import com.springboot.simplyfly.repository.BookingRepository;
import com.springboot.simplyfly.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final RazorpayService razorpayService;
    private final NotificationService notificationService;

    /*
     * Mock payment gateway.
     * SUCCESS -> booking CONFIRMED, seats BOOKED
     * FAILED  -> booking PAYMENT_FAILED, held seats released
     * */
    @Transactional(rollbackFor = Exception.class)
    public PaymentRespDto pay(long bookingId, PaymentDto dto, String passengerUsername) {
        // Step 1: Booking must belong to logged-in passenger and be pending
        Booking booking = getPayableBooking(bookingId, passengerUsername);

        // Step 2: When Razorpay is on, the mock payment is not allowed
        if (razorpayService.isEnabled())
            throw new InvalidCallException("Please pay using Razorpay");

        // Step 3: Prepare payment record
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setMethod(dto.method());

        // Step 4: Call (mock) gateway and update booking + seats
        if (dto.simulateFailure()) {
            payment.setStatus(PaymentStatus.FAILED);
            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            bookingService.updateSeatStatus(booking.getId(), SeatStatus.AVAILABLE);
            bookingService.updateJourneyStatus(booking.getId(), JourneyStatus.PAYMENT_FAILED);
            log.warn("Payment failed for booking {}", booking.getPnr());

            notificationService.add(booking.getPassenger().getUser(), "Payment failed",
                    "Payment for booking " + booking.getPnr() + " failed. No amount was charged.");
        } else {
            confirmBooking(booking, payment,
                    "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }

        // Step 5: Save
        bookingRepository.save(booking);
        return PaymentMapper.convertEntityToDto(paymentRepository.save(payment));
    }

    /*
     * Razorpay flow (test mode):
     * 1. createOrder -> Razorpay order is created for the booking amount
     * 2. Passenger pays in the Razorpay checkout pop-up
     * 3. verify      -> signature is checked, then booking CONFIRMED and seats BOOKED
     * */

    // Step 1: Create a Razorpay order for a pending booking.
    @Transactional(rollbackFor = Exception.class)
    public RazorpayOrderDto createOrder(long bookingId, String passengerUsername) {
        Booking booking = getPayableBooking(bookingId, passengerUsername);

        // Razorpay keys are not set - frontend will use the mock payment
        if (!razorpayService.isEnabled())
            return new RazorpayOrderDto(false, null, null, 0, RazorpayService.CURRENCY);

        String orderId = razorpayService.createOrder(booking.getTotalAmount(), booking.getPnr());

        // Order id is saved, so that only the payment of this order can confirm this booking
        booking.setRazorpayOrderId(orderId);
        bookingRepository.save(booking);

        return new RazorpayOrderDto(true, razorpayService.getKeyId(), orderId,
                RazorpayService.toPaise(booking.getTotalAmount()), RazorpayService.CURRENCY);
    }

    // Step 2: Verify the Razorpay payment and confirm the booking.
    @Transactional(rollbackFor = Exception.class)
    public PaymentRespDto verify(long bookingId, PaymentVerifyDto dto, String passengerUsername) {
        Booking booking = getPayableBooking(bookingId, passengerUsername);

        // The order must be the one created for this booking
        if (booking.getRazorpayOrderId() == null || !booking.getRazorpayOrderId().equals(dto.razorpayOrderId()))
            throw new InvalidCallException("Payment does not belong to this booking");

        if (!razorpayService.verifySignature(dto.razorpayOrderId(), dto.razorpayPaymentId(), dto.razorpaySignature()))
            throw new InvalidCallException("Payment verification failed");

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setMethod(razorpayService.getPaymentMethod(dto.razorpayPaymentId()));
        confirmBooking(booking, payment, dto.razorpayPaymentId());

        bookingRepository.save(booking);
        return PaymentMapper.convertEntityToDto(paymentRepository.save(payment));
    }

    // Booking must belong to the logged-in passenger and still wait for payment.
    private Booking getPayableBooking(long bookingId, String passengerUsername) {
        Booking booking = bookingService.getOwnedBooking(bookingId, passengerUsername);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT)
            throw new InvalidCallException("Booking is " + booking.getStatus() + ", payment not allowed");

        return booking;
    }

    // Payment SUCCESS -> booking CONFIRMED, seats BOOKED, passenger gets a notification.
    private void confirmBooking(Booking booking, Payment payment, String transactionId) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(transactionId);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingService.updateSeatStatus(booking.getId(), SeatStatus.BOOKED);
        bookingService.updateJourneyStatus(booking.getId(), JourneyStatus.CONFIRMED);
        log.info("Payment success for booking {}", booking.getPnr());

        notificationService.add(booking.getPassenger().getUser(), "Booking confirmed",
                "Your booking " + booking.getPnr() + " is confirmed. Have a nice trip!");
    }

    public List<PaymentRespDto> getMyPayments(String passengerUsername) {
        return paymentRepository.getByPassengerUsername(passengerUsername)
                .stream()
                .map(PaymentMapper::convertEntityToDto)
                .toList();
    }

    public List<PaymentRespDto> getAll(PaymentStatus status) {
        return paymentRepository.filterByStatus(status)
                .stream()
                .map(PaymentMapper::convertEntityToDto)
                .toList();
    }
}