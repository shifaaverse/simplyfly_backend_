package com.springboot.simplyfly.dto.response;

/* Demo only: the OTP is shown on the screen because no email or SMS is sent */
public record OtpRespDto(
        String otp,
        int validMinutes
) {
}
