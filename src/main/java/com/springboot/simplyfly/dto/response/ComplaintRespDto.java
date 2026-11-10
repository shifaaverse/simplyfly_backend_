package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.ComplaintCategory;
import com.springboot.simplyfly.enums.ComplaintStatus;

import java.time.Instant;

public record ComplaintRespDto(
        Long complaintId,
        String passengerName,
        String airlineName,
        String pnr,
        ComplaintCategory category,
        String subject,
        String description,
        ComplaintStatus status,
        String airlineResponse,
        Instant createdAt
) {
}
