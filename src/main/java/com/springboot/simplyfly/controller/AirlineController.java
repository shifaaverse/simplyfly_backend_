package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.AirlineDto;
import com.springboot.simplyfly.dto.request.AirlineUpdateDto;
import com.springboot.simplyfly.dto.response.AirlineRespDto;
import com.springboot.simplyfly.dto.response.AirlineStatDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.service.AirlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AirlineController {


    private final AirlineService airlineService;

    // Step 1: Airline submits registration.
    @PostMapping("/api/airline/register")
    public AirlineRespDto register(@Valid @RequestBody AirlineDto airlineDto) {
        return airlineService.register(airlineDto);
    }

    // Step 2: Admin directly adds an airline.
    @PostMapping("/api/admin/airline/add")
    public AirlineRespDto onboard(@Valid @RequestBody AirlineDto airlineDto) {
        return airlineService.onboard(airlineDto);
    }

    // Step 3: Admin gets all airlines.
    @GetMapping("/api/admin/airline/all")
    public List<AirlineRespDto> getAll(@RequestParam(value = "status", required = false) AirlineStatus status) {
        return airlineService.getAll(status);
    }

    // Step 4: Admin approves, rejects or suspends an airline.
       @PutMapping("/api/admin/airline/status/{airlineId}")
    public AirlineRespDto updateStatus(@PathVariable long airlineId,
                                       @RequestParam("status") AirlineStatus status,
                                       @RequestParam(value = "reason", required = false) String reason) {
        return airlineService.updateStatus(airlineId, status, reason);
    }

    // Step 5: Get the logged-in airline's profile.
    @GetMapping("/api/airline/profile")
    public AirlineRespDto getProfile(Principal principal) {
        return airlineService.getProfile(principal.getName());
    }

    // Step 6: Update the logged-in airline's profile.
    @PutMapping("/api/airline/profile")
    public AirlineRespDto updateProfile(@RequestBody AirlineUpdateDto dto, Principal principal) {
        return airlineService.updateProfile(principal.getName(), dto);
    }

    // Step 7: Get dashboard statistics for the logged-in airline.
    @GetMapping("/api/airline/stats")
    public AirlineStatDto getStats(Principal principal) {
        return airlineService.getStats(principal.getName());
    }
}