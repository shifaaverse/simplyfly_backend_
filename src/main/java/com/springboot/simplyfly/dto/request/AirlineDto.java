package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AirlineDto(
        @NotBlank(message = "Airline name is required")
        @NotNull(message = "Airline name is required")
        String name,
        @NotBlank(message = "Airline code is required")
        @Size(min = 2, max = 3, message = "Airline code should be 2-3 characters")
        String code,
        @NotBlank(message = "Registration number is required")
        String registrationNumber,
        @NotBlank(message = "Email is required")
        @Email(message = "Email seems invalid")
        String email,
        @NotBlank(message = "Contact is required")
        @Size(min = 10, max = 10, message = "contact should be 10 digit number")
        String contact,
        String address,
        String logoUrl,
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30)
        String username,
        @NotBlank(message = "Password is required")
        String password
) {
}
