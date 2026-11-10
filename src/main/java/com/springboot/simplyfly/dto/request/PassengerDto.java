package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PassengerDto(
        @NotBlank(message = "Name is required")
        @NotNull(message = "Name is required")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Email seems invalid")
        String email,
        @NotBlank(message = "Contact is required")
        @Size(min = 10, max = 10, message = "contact should be 10 digit mobile number")
        String contact,
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30)
        String username,
        @NotBlank(message = "Password is required")
        String password
) {
}
