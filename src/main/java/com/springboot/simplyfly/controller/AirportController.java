package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.AirportDto;
import com.springboot.simplyfly.model.Airport;
import com.springboot.simplyfly.service.AirportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AirportController {


    private final AirportService airportService;

    // Step 1: Admin adds multiple airports in a single request.
    @PostMapping("/api/admin/airport/batch/add")
    public void addAirportsInBatch(@Valid @RequestBody List<AirportDto> list) {
        airportService.addAirportsInBatch(list);
    }

    // Step 2: Get the list of all airports.
    @GetMapping("/api/airport/all")
    public List<Airport> getAll() {
        return airportService.getAll();
    }
}