package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AirportDto(
        @NotBlank(message = "Airport code is required")
        @Size(min = 3, max = 3, message = "Airport code should be 3 letters")
        String code,
        @NotBlank(message = "Airport name is required")
        String name,
        @NotBlank(message = "City is required")
        String city
) {
}
