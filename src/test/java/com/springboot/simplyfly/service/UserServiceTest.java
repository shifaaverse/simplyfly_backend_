package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.AdminDto;
import com.springboot.simplyfly.dto.response.DetailsDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.InactiveUserException;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.AirlineRepository;
import com.springboot.simplyfly.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void getUserObjTest() {
        when(userRepository.existsByUsername("harry")).thenReturn(false);
        when(passwordEncoder.encode("harry@123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User user = userService.getUserObj("harry", "harry@123", Role.PASSENGER);

        assertEquals("harry", user.getUsername());
        // password must be saved only in encoded form
        assertEquals("encoded", user.getPassword());
        assertEquals(Role.PASSENGER, user.getRole());
        assertTrue(user.isActive());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void getUserObjTest_UsernameTaken() {
        when(userRepository.existsByUsername("harry")).thenReturn(true);

        assertThrows(InvalidCallException.class, () -> userService.getUserObj("harry", "harry@123", Role.PASSENGER));

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void addAdminTest() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin@123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.addAdmin(new AdminDto("admin", "admin@123"));

        verify(userRepository, times(1)).save(argThat(u -> u.getRole() == Role.ADMIN));
    }

    @Test
    void addAdminTest_UsernameTaken() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        AdminDto dto = new AdminDto("admin", "admin@123");

        assertThrows(InvalidCallException.class, () -> userService.addAdmin(dto));

        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void getUserDetailsTest() {
        when(userRepository.getUserDetails("harry")).thenReturn(new DetailsDto("harry", true, Role.PASSENGER));

        DetailsDto dto = userService.getUserDetails("harry");

        assertEquals("harry", dto.username());
        assertEquals(Role.PASSENGER, dto.role());
        assertTrue(dto.isActive());
    }

    @Test
    void getUserDetailsTest_InactiveUser() {
        when(userRepository.getUserDetails("indigo")).thenReturn(new DetailsDto("indigo", false, Role.AIRLINE));

        assertThrows(InactiveUserException.class, () -> userService.getUserDetails("indigo"));
    }

    private Airline getAirline(AirlineStatus status, String reason) {
        Airline airline = new Airline();
        airline.setStatus(status);
        airline.setRejectionReason(reason);
        return airline;
    }

    @Test
    void getUserDetailsTest_AirlinePending() {
        when(userRepository.getUserDetails("indigo")).thenReturn(new DetailsDto("indigo", false, Role.AIRLINE));
        when(airlineRepository.getByUsername("indigo")).thenReturn(getAirline(AirlineStatus.PENDING, null));

        InactiveUserException e = assertThrows(InactiveUserException.class, () -> userService.getUserDetails("indigo"));

        assertTrue(e.getMessage().contains("waiting for admin approval"));
    }

    @Test
    void getUserDetailsTest_AirlineRejected() {
        when(userRepository.getUserDetails("indigo")).thenReturn(new DetailsDto("indigo", false, Role.AIRLINE));
        when(airlineRepository.getByUsername("indigo"))
                .thenReturn(getAirline(AirlineStatus.REJECTED, "Documents are not clear"));

        InactiveUserException e = assertThrows(InactiveUserException.class, () -> userService.getUserDetails("indigo"));

        assertTrue(e.getMessage().contains("rejected"));
        assertTrue(e.getMessage().contains("Documents are not clear"));
    }

    @Test
    void getUserDetailsTest_AirlineSuspended() {
        when(userRepository.getUserDetails("indigo")).thenReturn(new DetailsDto("indigo", false, Role.AIRLINE));
        when(airlineRepository.getByUsername("indigo"))
                .thenReturn(getAirline(AirlineStatus.SUSPENDED, "Safety audit"));

        InactiveUserException e = assertThrows(InactiveUserException.class, () -> userService.getUserDetails("indigo"));

        assertTrue(e.getMessage().contains("suspended"));
    }

    @Test
    void getUserDetailsTest_PassengerBlocked() {
        when(userRepository.getUserDetails("harry")).thenReturn(new DetailsDto("harry", false, Role.PASSENGER));

        InactiveUserException e = assertThrows(InactiveUserException.class, () -> userService.getUserDetails("harry"));

        assertTrue(e.getMessage().contains("blocked"));
        verifyNoInteractions(airlineRepository);
    }

    @Test
    void updateActiveTest() {
        User user = new User("harry", "encoded", Role.PASSENGER);

        userService.updateActive(user, false);

        assertFalse(user.isActive());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateActiveTest_Reactivate() {
        User user = new User("harry", "encoded", Role.PASSENGER);
        user.setActive(false);

        userService.updateActive(user, true);

        assertTrue(user.isActive());
        verify(userRepository, times(1)).save(user);
    }
}