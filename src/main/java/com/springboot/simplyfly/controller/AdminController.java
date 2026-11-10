package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.AdminDto;
import com.springboot.simplyfly.dto.response.AdminStatDto;
import com.springboot.simplyfly.dto.response.AirlineBookingCountDto;
import com.springboot.simplyfly.service.AdminService;
import com.springboot.simplyfly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;

    // Step 1: Create the first Admin account.
    @PostMapping("/api/admin/add")
    public void addAdmin(@RequestBody AdminDto adminDto) {
        userService.addAdmin(adminDto);
    }

    // Step 2: Get dashboard counts for passengers, airlines, flights, bookings, payments, refunds and revenue.
    @GetMapping("/api/admin/stats")
    public AdminStatDto getStats() {
        return adminService.getStats();
    }

    /*
       Airline Name  |  No of Bookings
    * */

    // Step 3: Get the number of bookings made through each airline.
    @GetMapping("/api/admin/stats/airline-bookings")
    public List<AirlineBookingCountDto> getAirlineWiseBookingCount() {
        return adminService.getAirlineWiseBookingCount();
    }
}