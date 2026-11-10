package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.PaymentDto;
import com.springboot.simplyfly.dto.request.PaymentVerifyDto;
import com.springboot.simplyfly.dto.response.PaymentRespDto;
import com.springboot.simplyfly.dto.response.RazorpayOrderDto;
import com.springboot.simplyfly.enums.PaymentStatus;
import com.springboot.simplyfly.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;

    // Passenger makes payment for a booking.
    @PostMapping("/api/passenger/payment/{bookingId}")
    public PaymentRespDto pay(@PathVariable long bookingId,
                              @Valid @RequestBody PaymentDto paymentDto,
                              Principal principal) {
        return paymentService.pay(bookingId, paymentDto, principal.getName());
    }

    // Passenger starts a Razorpay payment: the order is created for the booking amount.
    @PostMapping("/api/passenger/payment/order/{bookingId}")
    public RazorpayOrderDto createOrder(@PathVariable long bookingId, Principal principal) {
        return paymentService.createOrder(bookingId, principal.getName());
    }

    // Passenger sends the Razorpay result, the server verifies it and confirms the booking.
    @PostMapping("/api/passenger/payment/verify/{bookingId}")
    public PaymentRespDto verify(@PathVariable long bookingId,
                                 @Valid @RequestBody PaymentVerifyDto verifyDto,
                                 Principal principal) {
        return paymentService.verify(bookingId, verifyDto, principal.getName());
    }

    // Passenger views their own payments.
    @GetMapping("/api/passenger/payment/all")
    public List<PaymentRespDto> getMyPayments(Principal principal) {
        return paymentService.getMyPayments(principal.getName());
    }

    // Admin views all payments, optionally filtered by status.
    @GetMapping("/api/admin/payment/all")
    public List<PaymentRespDto> getAll(@RequestParam(name = "status", required = false) PaymentStatus status) {
        return paymentService.getAll(status);
    }
}