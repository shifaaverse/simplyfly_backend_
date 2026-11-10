package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.FlightStatus;
import com.springboot.simplyfly.enums.SeatClass;

import java.time.LocalDate;
import java.time.LocalTime;

public record FlightSearchRespDto(
        Long instanceId,
        String airlineName,
        String airlineCode,
        String flightNumber,
        String source,
        String destination,
        LocalDate travelDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        long durationMinutes,
        SeatClass seatClass,
        double fare,
        long availableSeats,
        String baggageInfo,
        FlightStatus status
) {
}
