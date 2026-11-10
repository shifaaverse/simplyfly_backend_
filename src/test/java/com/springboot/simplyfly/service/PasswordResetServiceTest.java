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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Helper method to create an OTP of harry
    private PasswordReset getReset(String otp, Instant expiresAt, boolean isUsed) {
        PasswordReset reset = new PasswordReset();
        reset.setUser(new User("harry", "old-encoded", Role.PASSENGER));
        reset.setOtp(otp);
        reset.setExpiresAt(expiresAt);
        reset.setUsed(isUsed);
        return reset;
    }

    // ==================== FORGOT PASSWORD TESTS ====================

    @Test
    void forgotPasswordTest() {
        when(userRepository.loadUserByUsername("harry")).thenReturn(new User("harry", "encoded", Role.PASSENGER));

        OtpRespDto resp = passwordResetService.forgotPassword(new ForgotPasswordDto("harry"));

        assertEquals(6, resp.otp().length());
        assertEquals(PasswordResetService.OTP_VALID_MINUTES, resp.validMinutes());

        ArgumentCaptor<PasswordReset> captor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository, times(1)).save(captor.capture());
        assertEquals(resp.otp(), captor.getValue().getOtp());
        assertFalse(captor.getValue().isUsed());
    }

    @Test
    void forgotPasswordTest_UserNotFound() {
        when(userRepository.loadUserByUsername("nobody")).thenReturn(null);
        ForgotPasswordDto dto = new ForgotPasswordDto("nobody");

        assertThrows(ResourceNotFoundException.class, () -> passwordResetService.forgotPassword(dto));

        verifyNoInteractions(passwordResetRepository);
    }

    @Test
    void forgotPasswordTest_Admin() {
        when(userRepository.loadUserByUsername("admin")).thenReturn(new User("admin", "encoded", Role.ADMIN));
        ForgotPasswordDto dto = new ForgotPasswordDto("admin");

        assertThrows(InvalidCallException.class, () -> passwordResetService.forgotPassword(dto));

        verifyNoInteractions(passwordResetRepository);
    }

    // ==================== RESET PASSWORD TESTS ====================

    @Test
    void resetPasswordTest() {
        PasswordReset reset = getReset("123456", Instant.now().plus(3, ChronoUnit.MINUTES), false);
        when(passwordResetRepository.findTopByUserUsernameOrderByIdDesc("harry")).thenReturn(reset);
        when(passwordEncoder.encode("harry@456")).thenReturn("new-encoded");

        passwordResetService.resetPassword(new ResetPasswordDto("harry", "123456", "harry@456"));

        assertEquals("new-encoded", reset.getUser().getPassword());
        assertTrue(reset.isUsed());
        verify(userRepository, times(1)).save(reset.getUser());
    }

    @Test
    void resetPasswordTest_WrongOtp() {
        PasswordReset reset = getReset("123456", Instant.now().plus(3, ChronoUnit.MINUTES), false);
        when(passwordResetRepository.findTopByUserUsernameOrderByIdDesc("harry")).thenReturn(reset);
        ResetPasswordDto dto = new ResetPasswordDto("harry", "000000", "harry@456");

        assertThrows(InvalidCallException.class, () -> passwordResetService.resetPassword(dto));

        assertFalse(reset.isUsed());
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void resetPasswordTest_Expired() {
        PasswordReset reset = getReset("123456", Instant.now().minus(1, ChronoUnit.MINUTES), false);
        when(passwordResetRepository.findTopByUserUsernameOrderByIdDesc("harry")).thenReturn(reset);
        ResetPasswordDto dto = new ResetPasswordDto("harry", "123456", "harry@456");

        assertThrows(InvalidCallException.class, () -> passwordResetService.resetPassword(dto));

        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void resetPasswordTest_AlreadyUsed() {
        PasswordReset reset = getReset("123456", Instant.now().plus(3, ChronoUnit.MINUTES), true);
        when(passwordResetRepository.findTopByUserUsernameOrderByIdDesc("harry")).thenReturn(reset);
        ResetPasswordDto dto = new ResetPasswordDto("harry", "123456", "harry@456");

        assertThrows(InvalidCallException.class, () -> passwordResetService.resetPassword(dto));

        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void resetPasswordTest_NoOtpRequested() {
        when(passwordResetRepository.findTopByUserUsernameOrderByIdDesc("harry")).thenReturn(null);
        ResetPasswordDto dto = new ResetPasswordDto("harry", "123456", "harry@456");

        assertThrows(InvalidCallException.class, () -> passwordResetService.resetPassword(dto));
    }
}
