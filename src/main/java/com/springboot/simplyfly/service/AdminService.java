package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.response.AdminStatDto;
import com.springboot.simplyfly.dto.response.AirlineBookingCountDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.PaymentStatus;
import com.springboot.simplyfly.enums.RefundStatus;
import com.springboot.simplyfly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    // Booking statuses included in the admin statistics.
    private static final List<BookingStatus> BOOKED_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.PARTIALLY_CANCELLED, BookingStatus.CANCELLED);

    private final PassengerRepository passengerRepository;
    private final AirlineRepository airlineRepository;
    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    // Get the overall platform statistics.
    public AdminStatDto getStats() {
        double collected = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        double refunded = refundRepository.sumRefundAmountByStatus(RefundStatus.REFUNDED);

        return new AdminStatDto(
                passengerRepository.count(),
                airlineRepository.count(),
                airlineRepository.countByStatus(AirlineStatus.APPROVED),
                airlineRepository.countByStatus(AirlineStatus.PENDING),
                flightRepository.count(),
                bookingRepository.countByStatusIn(BOOKED_STATUSES),
                paymentRepository.countByStatus(PaymentStatus.SUCCESS),
                refundRepository.countByStatusIn(List.of(RefundStatus.REQUESTED, RefundStatus.FORWARDED_TO_AIRLINE)),
                Math.round((collected - refunded) * 100.0) / 100.0
        );
    }

    // Get the booking count for each airline.
    public List<AirlineBookingCountDto> getAirlineWiseBookingCount() {
        return airlineRepository.getAirlineWiseBookingCount(BOOKED_STATUSES);
    }
}