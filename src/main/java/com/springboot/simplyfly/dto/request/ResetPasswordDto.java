package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDto(
        @NotBlank(message = "Username is required")
        String username,
        @NotBlank(message = "OTP is required")
        String otp,
        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password should have at least 6 characters")
        String newPassword
) {
}
