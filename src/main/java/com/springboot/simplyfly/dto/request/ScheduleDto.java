package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/* operatingDays empty or null = DAILY */
public record ScheduleDto(
        @NotNull(message = "Flight is required")
        Long flightId,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        @NotNull(message = "End date is required")
        LocalDate endDate,
        List<DayOfWeek> operatingDays
) {
}
