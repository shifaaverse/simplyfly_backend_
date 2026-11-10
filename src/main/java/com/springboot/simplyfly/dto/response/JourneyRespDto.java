package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.FlightStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.enums.JourneyType;
import com.springboot.simplyfly.enums.SeatClass;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record JourneyRespDto(
        Long journeyId,
        JourneyType journeyType,
        String airlineName,
        String flightNumber,
        String source,
        String destination,
        LocalDate travelDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        SeatClass seatClass,
        double amount,
        JourneyStatus status,
        FlightStatus flightStatus,
        List<TravellerSeatRespDto> travellers
) {
}
