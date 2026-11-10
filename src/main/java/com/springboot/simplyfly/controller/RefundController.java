package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.RefundRequestDto;
import com.springboot.simplyfly.dto.response.RefundRespDto;
import com.springboot.simplyfly.enums.RefundStatus;
import com.springboot.simplyfly.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RefundController {

    private final RefundService refundService;

    // Passenger cancels a journey and requests a refund.
    @PostMapping("/api/passenger/refund/add")
    public RefundRespDto request(@Valid @RequestBody RefundRequestDto dto, Principal principal) {
        return refundService.request(dto, principal.getName());
    }

    // Passenger views their refund requests.
    @GetMapping("/api/passenger/refund/all")
    public List<RefundRespDto> getMyRefunds(Principal principal) {
        return refundService.getMyRefunds(principal.getName());
    }

    // Admin views all refund requests, optionally filtered by status.
    @GetMapping("/api/admin/refund/all")
    public List<RefundRespDto> getAll(@RequestParam(name = "status", required = false) RefundStatus status) {
        return refundService.getAll(status);
    }

    // Admin forwards a refund request to the airline.
    @PutMapping("/api/admin/refund/forward/{refundId}")
    public RefundRespDto forward(@PathVariable long refundId) {
        return refundService.forward(refundId);
    }

    // Airline views refund requests forwarded to it.
    @GetMapping("/api/airline/refund/all")
    public List<RefundRespDto> getForAirline(Principal principal) {
        return refundService.getForAirline(principal.getName());
    }

    // Airline approves a refund request.
    @PutMapping("/api/airline/refund/approve/{refundId}")
    public RefundRespDto approve(@PathVariable long refundId, Principal principal) {
        return refundService.approve(refundId, principal.getName());
    }

    // Airline rejects a refund request with a reason.
    @PutMapping("/api/airline/refund/reject/{refundId}")
    public RefundRespDto reject(@PathVariable long refundId,
                                @RequestParam("reason") String reason,
                                Principal principal) {
        return refundService.reject(refundId, reason, principal.getName());
    }
}