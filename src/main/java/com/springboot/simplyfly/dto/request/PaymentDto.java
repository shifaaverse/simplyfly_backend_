package com.springboot.simplyfly.dto.request;

import com.springboot.simplyfly.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/* Mock payment gateway. simulateFailure = true lets us test failed payments */
public record PaymentDto(
        @NotNull(message = "Payment method is required")
        PaymentMethod method,
        boolean simulateFailure
) {
}
