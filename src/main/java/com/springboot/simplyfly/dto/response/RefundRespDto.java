package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.RefundStatus;

import java.time.Instant;
import java.time.LocalDate;

public record RefundRespDto(
        Long refundId,
        Long journeyId,
        String pnr,
        String passengerName,
        String airlineName,
        String flightNumber,
        LocalDate travelDate,
        double journeyAmount,
        double refundAmount,
        String reason,
        RefundStatus status,
        String rejectionReason,
        Instant createdAt
) {
}
