package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.PaymentMethod;
import com.springboot.simplyfly.enums.PaymentStatus;

import java.time.Instant;

public record PaymentRespDto(
        Long paymentId,
        Long bookingId,
        String pnr,
        String passengerName,
        double amount,
        PaymentMethod method,
        PaymentStatus status,
        String transactionId,
        Instant createdAt
) {
}
