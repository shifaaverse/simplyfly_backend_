package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.ForgotPasswordDto;
import com.springboot.simplyfly.dto.request.ResetPasswordDto;
import com.springboot.simplyfly.dto.response.OtpRespDto;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.PasswordReset;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.PasswordResetRepository;
import com.springboot.simplyfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    // Minutes for which the OTP is valid.
    public static final int OTP_VALID_MINUTES = 5;

    // OTP has 6 digits (000000 to 999999).
    private static final int OTP_LIMIT = 1_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Create a 6 digit OTP for the user (demo: it is returned, not sent by email).
    public OtpRespDto forgotPassword(ForgotPasswordDto dto) {
        // Step 1: User must exist. Admin password cannot be reset this way.
        User user = userRepository.loadUserByUsername(dto.username());
        if (user == null)
            throw new ResourceNotFoundException("User not found");

        if (user.getRole() == Role.ADMIN)
            throw new InvalidCallException("Admin password cannot be reset here");

        // Step 2: Generate the OTP and save it
        String otp = String.format("%06d", RANDOM.nextInt(OTP_LIMIT));

        PasswordReset reset = new PasswordReset();
        reset.setUser(user);
        reset.setOtp(otp);
        reset.setExpiresAt(Instant.now().plus(OTP_VALID_MINUTES, ChronoUnit.MINUTES));
        reset.setUsed(false);
        passwordResetRepository.save(reset);

        return new OtpRespDto(otp, OTP_VALID_MINUTES);
    }

    // Check the OTP and set the new password.
    public void resetPassword(ResetPasswordDto dto) {
        // Step 1: Only the latest OTP of the user can be used, once, before it expires
        PasswordReset reset = passwordResetRepository.findTopByUserUsernameOrderByIdDesc(dto.username());

        if (reset == null
                || reset.isUsed()
                || reset.getExpiresAt().isBefore(Instant.now())
                || !reset.getOtp().equals(dto.otp()))
            throw new InvalidCallException("Invalid or expired OTP");

        // Step 2: Save the new password in encoded form and mark the OTP as used
        User user = reset.getUser();
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        reset.setUsed(true);
        passwordResetRepository.save(reset);
    }
}
