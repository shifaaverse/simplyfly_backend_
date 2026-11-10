package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.response.DetailsDto;
import com.springboot.simplyfly.dto.response.TokenDto;
import com.springboot.simplyfly.service.UserService;
import com.springboot.simplyfly.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;


@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    // Utility class used to generate and read JWT tokens.
    private final JwtUtility jwtUtility;
    private final UserService userService;



    // Step 1: Generate a JWT token for the authenticated user.
    @GetMapping("/api/auth/login")
    public TokenDto login(Principal principal) {

        // Get the username of the logged-in user.
        String loggedInUsername = principal.getName();

        // Generate a JWT token using the username.
        String token = jwtUtility.generateToken(loggedInUsername);

        // Return the token, its expiration time and the current UTC time.
        return new TokenDto(token,
                jwtUtility.extractExpiration(token),
                LocalDateTime.now(ZoneId.of("UTC")).toString());
    }


    // Step 2: Get the details of the authenticated user.
    @GetMapping("/api/auth/user-details")
    public DetailsDto getUserDetails(Principal principal) {

        // Get the username of the logged-in user.
        String loggedInUsername = principal.getName();

        // Fetch and return the user's details.
        return userService.getUserDetails(loggedInUsername);
    }
}