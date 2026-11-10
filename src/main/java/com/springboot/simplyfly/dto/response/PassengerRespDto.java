package com.springboot.simplyfly.dto.response;

import java.time.LocalDate;

public record PassengerRespDto(
        Long passengerId,
        String name,
        String email,
        String contact,
        LocalDate dateOfBirth,
        String username,
        boolean isActive
) {
}
