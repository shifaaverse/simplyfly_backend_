package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.ScheduleDto;
import com.springboot.simplyfly.dto.response.FlightInstanceRespDto;
import com.springboot.simplyfly.dto.response.ScheduleRespDto;
import com.springboot.simplyfly.enums.FlightStatus;
import com.springboot.simplyfly.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // Airline creates a recurring schedule and generates flight instances with seats.
    @PostMapping("/api/airline/schedule/add")
    public ScheduleRespDto add(@Valid @RequestBody ScheduleDto scheduleDto, Principal principal) {
        return scheduleService.add(scheduleDto, principal.getName());
    }

    // Airline views its flight instances with optional date filtering and pagination.
    @GetMapping("/api/airline/instance/all")
    public List<FlightInstanceRespDto> getInstances(
            Principal principal,
            @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "50") Integer size) {
        return scheduleService.getInstances(principal.getName(), fromDate, page, size);
    }

    // Airline updates the status of a flight instance.
    @PutMapping("/api/airline/instance/status/{instanceId}")
    public void updateStatus(@PathVariable long instanceId,
                             @RequestParam("status") FlightStatus status,
                             @RequestParam(name = "delayMinutes", required = false) Integer delayMinutes,
                             Principal principal) {
        scheduleService.updateStatus(instanceId, status, delayMinutes, principal.getName());
    }

    // Admin views all flight instances for monitoring.
    @GetMapping("/api/admin/instance/all")
    public List<FlightInstanceRespDto> getAllInstances(
            @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "50") Integer size) {
        return scheduleService.getAllInstances(fromDate, page, size);
    }
}