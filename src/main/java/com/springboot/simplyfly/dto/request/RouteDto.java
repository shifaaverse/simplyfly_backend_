package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.NotNull;

public record RouteDto(
        @NotNull(message = "Source airport is required")
        Long sourceAirportId,
        @NotNull(message = "Destination airport is required")
        Long destinationAirportId
) {
}
