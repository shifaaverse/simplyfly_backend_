package com.springboot.simplyfly.dto.request;

import com.springboot.simplyfly.enums.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintDto(
        Long bookingId,
        @NotNull(message = "Category is required")
        ComplaintCategory category,
        @NotBlank(message = "Subject is required")
        String subject,
        @NotBlank(message = "Description is required")
        String description
) {
}
