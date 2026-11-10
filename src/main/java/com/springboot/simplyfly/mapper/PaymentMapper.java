package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.response.PaymentRespDto;
import com.springboot.simplyfly.model.Payment;

public class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentRespDto convertEntityToDto(Payment payment) {
        return new PaymentRespDto(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getBooking().getPnr(),
                payment.getBooking().getPassenger().getName(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getCreatedAt()
        );
    }
}
