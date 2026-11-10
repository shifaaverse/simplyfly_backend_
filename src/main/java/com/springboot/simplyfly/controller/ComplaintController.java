package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.ComplaintDto;
import com.springboot.simplyfly.dto.request.ComplaintResponseDto;
import com.springboot.simplyfly.dto.response.ComplaintRespDto;
import com.springboot.simplyfly.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

// Handles all Complaint-related API requests.
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ComplaintController {

    // Service used to perform complaint-related operations.
    private final ComplaintService complaintService;

    // Step 1: Passenger submits a new complaint.
    @PostMapping("/api/passenger/complaint/add")
    public ComplaintRespDto add(@Valid @RequestBody ComplaintDto dto, Principal principal) {
        return complaintService.add(dto, principal.getName());
    }

    // Step 2: Passenger views all their own complaints.
    @GetMapping("/api/passenger/complaint/all")
    public List<ComplaintRespDto> getMyComplaints(Principal principal) {
        return complaintService.getMyComplaints(principal.getName());
    }

    // Step 3: Admin views all complaints submitted on the platform.
    @GetMapping("/api/admin/complaint/all")
    public List<ComplaintRespDto> getAll() {
        return complaintService.getAll();
    }

    // Step 4: Admin forwards a complaint to an airline.
    // Airline ID is optional.
    @PutMapping("/api/admin/complaint/forward/{complaintId}")
    public ComplaintRespDto forward(@PathVariable long complaintId,
                                    @RequestParam(name = "airlineId", required = false) Long airlineId) {
        return complaintService.forward(complaintId, airlineId);
    }

    // Step 5: Airline views complaints forwarded to it.
    @GetMapping("/api/airline/complaint/all")
    public List<ComplaintRespDto> getForAirline(Principal principal) {
        return complaintService.getForAirline(principal.getName());
    }

    // Step 6: Airline submits a response to a complaint.
    @PutMapping("/api/airline/complaint/respond/{complaintId}")
    public ComplaintRespDto respond(@PathVariable long complaintId,
                                    @Valid @RequestBody ComplaintResponseDto dto,
                                    Principal principal) {
        return complaintService.respond(complaintId, dto, principal.getName());
    }
}