package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.TripType;

import java.time.Instant;
import java.util.List;

public record BookingRespDto(
        Long bookingId,
        String pnr,
        TripType tripType,
        BookingStatus status,
        double totalAmount,
        Instant createdAt,
        List<JourneyRespDto> journeys
) {
}
