package com.springboot.simplyfly.dto.request;

import jakarta.validation.constraints.NotBlank;

/* The three values Razorpay checkout gives back after a successful payment */
public record PaymentVerifyDto(
        @NotBlank(message = "Razorpay order id is required")
        String razorpayOrderId,
        @NotBlank(message = "Razorpay payment id is required")
        String razorpayPaymentId,
        @NotBlank(message = "Razorpay signature is required")
        String razorpaySignature
) {
}
