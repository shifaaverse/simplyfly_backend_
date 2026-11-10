package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.FlightStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record FlightInstanceRespDto(
        Long instanceId,
        String airlineName,
        String flightNumber,
        String source,
        String destination,
        LocalDate travelDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        FlightStatus status
) {
}
