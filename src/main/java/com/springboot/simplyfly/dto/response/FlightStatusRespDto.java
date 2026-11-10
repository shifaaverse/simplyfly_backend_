package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.FlightStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/* Live status of one dated flight. expectedDeparture is different from departureTime only when delayed */
public record FlightStatusRespDto(
        String flightNumber,
        String airlineName,
        String source,
        String destination,
        LocalDate travelDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        FlightStatus status,
        Integer delayMinutes,
        LocalTime expectedDeparture,
        String message
) {
}
