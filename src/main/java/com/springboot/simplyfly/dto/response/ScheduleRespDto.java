package com.springboot.simplyfly.dto.response;

import java.time.LocalDate;

public record ScheduleRespDto(
        Long scheduleId,
        String flightNumber,
        LocalDate startDate,
        LocalDate endDate,
        String operatingDays,
        int instancesCreated
) {
}
