package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.AirlineStatus;

import java.time.Instant;

public record AirlineRespDto(
        Long airlineId,
        String name,
        String code,
        String registrationNumber,
        String email,
        String contact,
        String address,
        String logoUrl,
        AirlineStatus status,
        String rejectionReason,
        String username,
        Instant createdAt
) {
}
