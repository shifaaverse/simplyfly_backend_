package com.springboot.simplyfly.dto.request;

import com.springboot.simplyfly.enums.ComplaintStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintResponseDto(
        @NotBlank(message = "Response is required")
        String response,
        @NotNull(message = "Status is required")
        ComplaintStatus status
) {
}
