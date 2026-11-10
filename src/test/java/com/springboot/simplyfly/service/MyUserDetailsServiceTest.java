package com.springboot.simplyfly.service;

import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceTest {

    @InjectMocks
    private MyUserDetailsService myUserDetailsService;

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsernameTest() {
        User user = new User("harry", "encoded", Role.PASSENGER);
        when(userRepository.loadUserByUsername("harry")).thenReturn(user);

        UserDetails details = myUserDetailsService.loadUserByUsername("harry");

        assertEquals("harry", details.getUsername());
        assertEquals("encoded", details.getPassword());
        assertEquals("PASSENGER", details.getAuthorities().iterator().next().getAuthority());
        verify(userRepository, times(1)).loadUserByUsername("harry");
    }

    @Test
    void loadUserByUsernameTest_UserNotFound() {
        when(userRepository.loadUserByUsername("ghost")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> myUserDetailsService.loadUserByUsername("ghost"));
    }
}