package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.BookingDto;
import com.springboot.simplyfly.dto.response.AdminBookingDto;
import com.springboot.simplyfly.dto.response.AirlineBookingDto;
import com.springboot.simplyfly.dto.response.BookingRespDto;
import com.springboot.simplyfly.dto.response.FlightPassengerDto;
import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;


@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingService bookingService;

    // Step 1: Passenger creates a one-way or round-trip booking.

    @PostMapping("/api/passenger/booking/add")
    public BookingRespDto add(@Valid @RequestBody BookingDto bookingDto, Principal principal) {
        return bookingService.add(bookingDto, principal.getName());
    }

    // Step 2: Get all bookings made by the logged-in passenger.
    @GetMapping("/api/passenger/booking/all")
    public List<BookingRespDto> getMyBookings(Principal principal) {
        return bookingService.getMyBookings(principal.getName());
    }

    // Step 3: Get a particular booking using its booking ID.
    @GetMapping("/api/passenger/booking/{bookingId}")
    public BookingRespDto getById(@PathVariable long bookingId, Principal principal) {
        return bookingService.getBookingById(bookingId, principal.getName());
    }

    // Step 4: Airline views bookings belonging only to its own flights.

    @GetMapping("/api/airline/booking/all")
    public List<AirlineBookingDto> getAirlineBookings(Principal principal) {
        return bookingService.getBookingsForAirline(principal.getName());
    }

    // Airline views the passengers of one of its flights (government ID type, seat, meal and baggage).
    @GetMapping("/api/airline/instance/passengers/{instanceId}")
    public List<FlightPassengerDto> getFlightPassengers(@PathVariable long instanceId, Principal principal) {
        return bookingService.getFlightPassengers(instanceId, principal.getName());
    }

    // Step 5: Admin views all bookings across the platform.

    @GetMapping("/api/admin/booking/all")
    public List<AdminBookingDto> getAll(
            @RequestParam(name = "status", required = false) BookingStatus status,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "50") Integer size) {
        return bookingService.getAllForAdmin(status, page, size);
    }
}