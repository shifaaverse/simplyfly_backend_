package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.PassengerDto;
import com.springboot.simplyfly.dto.request.PassengerUpdateDto;
import com.springboot.simplyfly.dto.response.BookingRespDto;
import com.springboot.simplyfly.dto.response.PassengerRespDto;
import com.springboot.simplyfly.service.BookingService;
import com.springboot.simplyfly.service.PassengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class PassengerController {

    private final PassengerService passengerService;
    private final BookingService bookingService;

    // Step 1: Register a new passenger.
    /* Sign up */
    @PostMapping("/api/passenger/add")
    public void insertPassenger(@Valid @RequestBody PassengerDto passengerDto) {
        passengerService.insertPassenger(passengerDto);
    }

    // Step 2: Get the profile of the logged-in passenger.
    @GetMapping("/api/passenger/profile")
    public PassengerRespDto getProfile(Principal principal) {
        return passengerService.getProfile(principal.getName());
    }

    // Step 3: Update the profile of the logged-in passenger.
    @PutMapping("/api/passenger/profile")
    public void updateProfile(@RequestBody PassengerUpdateDto dto, Principal principal) {
        passengerService.updateProfile(principal.getName(), dto);
    }

    // Step 4: Admin views or searches passengers.
    @GetMapping("/api/admin/passenger/all")
    public List<PassengerRespDto> getAll(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size) {
        return passengerService.getAll(search, page, size);
    }

    // Step 5: Admin activates or deactivates a passenger account.
    @PutMapping("/api/admin/passenger/status/{passengerId}")
    public void updateStatus(@PathVariable long passengerId,
                             @RequestParam("active") boolean active) {
        passengerService.updateStatus(passengerId, active);
    }

    // Step 6: Admin views the booking history of a particular passenger.
    @GetMapping("/api/admin/passenger/{passengerId}/bookings")
    public List<BookingRespDto> getPassengerBookings(@PathVariable long passengerId) {
        return bookingService.getBookingsByPassengerId(passengerId);
    }
}