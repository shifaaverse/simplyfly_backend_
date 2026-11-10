package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.response.RefundRespDto;
import com.springboot.simplyfly.model.BookingJourney;
import com.springboot.simplyfly.model.Refund;

public class RefundMapper {

    private RefundMapper() {
    }

    public static RefundRespDto convertEntityToDto(Refund refund) {
        BookingJourney journey = refund.getBookingJourney();
        return new RefundRespDto(
                refund.getId(),
                journey.getId(),
                journey.getBooking().getPnr(),
                journey.getBooking().getPassenger().getName(),
                journey.getFlightInstance().getFlight().getAirline().getName(),
                journey.getFlightInstance().getFlight().getFlightNumber(),
                journey.getFlightInstance().getTravelDate(),
                journey.getAmount(),
                refund.getRefundAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getRejectionReason(),
                refund.getCreatedAt()
        );
    }
}
