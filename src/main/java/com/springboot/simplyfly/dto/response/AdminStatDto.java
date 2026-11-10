package com.springboot.simplyfly.dto.response;

public record AdminStatDto(
        long totalPassengers,
        long totalAirlines,
        long activeAirlines,
        long pendingAirlines,
        long totalFlights,
        long totalBookings,
        long totalPayments,
        long pendingRefunds,
        double revenue
) {
}
