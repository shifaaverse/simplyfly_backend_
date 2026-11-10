package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalTime;

public record FlightDto(
        @NotBlank(message = "Flight number is required")
        String flightNumber,
        @NotNull(message = "Route is required")
        Long routeId,
        @NotNull(message = "Departure time is required")
        LocalTime departureTime,
        @NotNull(message = "Arrival time is required")
        LocalTime arrivalTime,
        @Min(0) @Max(10)
        int businessRows,
        @Min(0) @Max(10)
        int premiumRows,
        @Min(value = 1, message = "At least 1 economy row is required") @Max(40)
        int economyRows,
        @PositiveOrZero
        double businessFare,
        @PositiveOrZero
        double premiumFare,
        @PositiveOrZero
        double economyFare,
        @PositiveOrZero
        double windowCharge,
        @PositiveOrZero
        double aisleCharge,
        String baggageInfo
) {
}
