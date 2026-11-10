package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefundRequestDto(
        @NotNull(message = "Journey is required")
        Long journeyId,
        @NotBlank(message = "Reason is required")
        String reason
) {
}
