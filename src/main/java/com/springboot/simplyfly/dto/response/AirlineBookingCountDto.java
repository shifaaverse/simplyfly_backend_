package com.springboot.simplyfly.dto.response;

public record AirlineBookingCountDto(
        String airlineName,
        Long numberOfBookings
) {
}
