package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.enums.JourneyType;

import java.time.LocalDate;

public record AdminBookingDto(
        Long bookingId,
        String pnr,
        String passengerName,
        String airlineName,
        String flightNumber,
        String source,
        String destination,
        LocalDate travelDate,
        JourneyType journeyType,
        BookingStatus bookingStatus,
        JourneyStatus journeyStatus,
        double amount
) {
}
