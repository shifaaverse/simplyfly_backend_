package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.ForgotPasswordDto;
import com.springboot.simplyfly.dto.request.ResetPasswordDto;
import com.springboot.simplyfly.dto.response.OtpRespDto;
import com.springboot.simplyfly.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // Step 1: Get an OTP for a forgotten password.
    @PostMapping("/api/auth/forgot-password")
    public OtpRespDto forgotPassword(@Valid @RequestBody ForgotPasswordDto dto) {
        return passwordResetService.forgotPassword(dto);
    }

    // Step 2: Set a new password using the OTP.
    @PostMapping("/api/auth/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        passwordResetService.resetPassword(dto);
    }
}
