package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.enums.PassengerCategory;

import java.time.LocalDate;

/* Booking info visible to an airline - only its own flights */
public record AirlineBookingDto(
        Long bookingId,
        String pnr,
        String travellerName,
        PassengerCategory category,
        LocalDate travelDate,
        String flightNumber,
        String seatNumber,
        BookingStatus bookingStatus,
        JourneyStatus journeyStatus
) {
}
