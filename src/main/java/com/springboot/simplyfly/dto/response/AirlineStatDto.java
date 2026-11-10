package com.springboot.simplyfly.dto.response;

public record AirlineStatDto(
        long totalRoutes,
        long totalFlights,
        long todaysFlights,
        long upcomingFlights,
        long totalBookings,
        long totalPassengers,
        long availableSeats,
        long cancelledJourneys,
        long pendingRefunds,
        double revenue
) {
}
