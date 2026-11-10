package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

/* Only fares, seat charges and baggage can be changed. Timings and seat rows stay fixed because seats are already generated. */
public record FlightUpdateDto(
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
