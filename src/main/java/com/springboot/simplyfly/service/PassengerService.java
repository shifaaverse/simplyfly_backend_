package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.PassengerDto;
import com.springboot.simplyfly.dto.request.PassengerUpdateDto;
import com.springboot.simplyfly.dto.response.PassengerRespDto;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.mapper.PassengerMapper;
import com.springboot.simplyfly.model.Passenger;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final UserService userService;

    // Register a new passenger.
    @Transactional(rollbackFor = Exception.class)
    public void insertPassenger(PassengerDto dto) {
        User user = userService.getUserObj(
                dto.username(),
                dto.password(),
                Role.PASSENGER
        );

        Passenger passenger = PassengerMapper.convertDtoToEntity(dto);
        passenger.setUser(user);

        passengerRepository.save(passenger);
    }

    // Find a passenger by username.
    public Passenger getByUsername(String username) {
        Passenger passenger = passengerRepository.getByUsername(username);

        if (passenger == null)
            throw new ResourceNotFoundException("Passenger not found");

        return passenger;
    }

    // Find a passenger by ID.
    public Passenger getById(long id) {
        return passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid passenger id"));
    }

    // Get the profile of the logged-in passenger.
    public PassengerRespDto getProfile(String username) {
        return PassengerMapper.convertEntityToDto(getByUsername(username));
    }

    // Update the passenger's profile.
    public void updateProfile(String username, PassengerUpdateDto dto) {
        Passenger passenger = getByUsername(username);
        boolean flag = false;

        if (dto.name() != null && !dto.name().trim().isEmpty()) {
            passenger.setName(dto.name());
            flag = true;
        }

        if (dto.contact() != null && !dto.contact().trim().isEmpty()) {
            passenger.setContact(dto.contact());
            flag = true;
        }

        if (!flag)
            throw new InvalidCallException("Update op terminated");

        passengerRepository.save(passenger);
    }

    // Get all passengers with optional search and pagination.
    public List<PassengerRespDto> getAll(String search, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        String key = (search == null || search.isBlank()) ? null : search.trim();

        return passengerRepository.searchPassengers(key, pageable)
                .stream()
                .map(PassengerMapper::convertEntityToDto)
                .toList();
    }

    // Activate or deactivate a passenger account.
    public void updateStatus(long passengerId, boolean isActive) {
        Passenger passenger = getById(passengerId);
        userService.updateActive(passenger.getUser(), isActive);
    }
}