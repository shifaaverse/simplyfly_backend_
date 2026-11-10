package com.springboot.simplyfly.dto.response;

import java.time.LocalDate;

/* Route that has bookable flights, with the date range they operate */
public record RouteAvailabilityDto(
        String sourceCity,
        String sourceCode,
        String destinationCity,
        String destinationCode,
        LocalDate firstDate,
        LocalDate lastDate,
        Long numberOfFlights
) {
}
