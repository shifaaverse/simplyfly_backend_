package com.springboot.simplyfly.dto.response;

import java.time.LocalTime;

public record FlightRespDto(
        Long flightId,
        String flightNumber,
        Long routeId,
        String source,
        String destination,
        LocalTime departureTime,
        LocalTime arrivalTime,
        int businessRows,
        int premiumRows,
        int economyRows,
        double businessFare,
        double premiumFare,
        double economyFare,
        double windowCharge,
        double aisleCharge,
        String baggageInfo,
        boolean isActive
) {
}
