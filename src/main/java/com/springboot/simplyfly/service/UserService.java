package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.AdminDto;
import com.springboot.simplyfly.dto.response.DetailsDto;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.InactiveUserException;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.AirlineRepository;
import com.springboot.simplyfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AirlineRepository airlineRepository;
    private final PasswordEncoder passwordEncoder;

    /* Prepares a User with encoded password and saves it in DB */
    public User getUserObj(String username, String password, Role role) {
        // Step 1: Username must be unique
        if (userRepository.existsByUsername(username))
            throw new InvalidCallException("Username already taken");

        // Step 2: Encode the password and save
        User user = new User(username, passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    public void addAdmin(AdminDto adminDto) {
        // Only the very first admin can be created without login
        if (userRepository.existsByRole(Role.ADMIN))
            throw new InvalidCallException("Admin already exists");

        // Prepare User using AdminDto and save it in DB
        getUserObj(adminDto.username(), adminDto.password(), Role.ADMIN);
    }

    public DetailsDto getUserDetails(String loggedInUsername) {
        DetailsDto detailsDto = userRepository.getUserDetails(loggedInUsername);
        if (!detailsDto.isActive())
            throw new InactiveUserException(getInactiveMessage(detailsDto));
        return detailsDto;
    }

    // Clear message for a user who cannot login (airline waiting for approval, rejected, suspended or blocked).
    private String getInactiveMessage(DetailsDto detailsDto) {
        if (detailsDto.role() == Role.AIRLINE) {
            Airline airline = airlineRepository.getByUsername(detailsDto.username());

            if (airline != null) {
                String reason = airline.getRejectionReason() == null ? "not given" : airline.getRejectionReason();

                return switch (airline.getStatus()) {
                    case PENDING -> "Your airline registration is waiting for admin approval";
                    case REJECTED -> "Your airline registration was rejected. Reason: " + reason;
                    case SUSPENDED -> "Your airline account is suspended. Reason: " + reason;
                    case APPROVED -> "Your account is disabled";
                };
            }
        }

        return "Your account has been blocked by the admin";
    }

    public void updateActive(User user, boolean isActive) {
        user.setActive(isActive);
        userRepository.save(user);
    }
}