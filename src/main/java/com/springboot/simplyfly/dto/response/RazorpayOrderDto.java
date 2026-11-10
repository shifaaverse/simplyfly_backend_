package com.springboot.simplyfly.dto.response;

/* enabled = false means Razorpay keys are not configured (demo payment is used instead) */
public record RazorpayOrderDto(
        boolean enabled,
        String keyId,
        String orderId,
        long amountInPaise,
        String currency
) {
}
