package com.springboot.simplyfly.dto.response;

public record RouteRespDto(
        Long routeId,
        Long sourceAirportId,
        String sourceCode,
        String sourceCity,
        Long destinationAirportId,
        String destinationCode,
        String destinationCity,
        boolean isActive
) {
}
