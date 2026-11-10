package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.FlightDto;
import com.springboot.simplyfly.dto.request.FlightUpdateDto;
import com.springboot.simplyfly.dto.response.FlightFilterDto;
import com.springboot.simplyfly.dto.response.FlightRespDto;
import com.springboot.simplyfly.dto.response.FlightStatusRespDto;
import com.springboot.simplyfly.dto.response.RouteAvailabilityDto;
import com.springboot.simplyfly.dto.response.SeatRespDto;
import com.springboot.simplyfly.enums.FlightSort;
import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Handles all Flight-related API requests.
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class FlightController {


    private final FlightService flightService;

    // Step 1: Airline adds a new flight.
    @PostMapping("/api/airline/flight/add")
    public FlightRespDto add(@Valid @RequestBody FlightDto flightDto, Principal principal) {
        return flightService.add(flightDto, principal.getName());
    }

    // Step 2: Get all flights belonging to the logged-in airline.
    @GetMapping("/api/airline/flight/all")
    public List<FlightRespDto> getFlights(Principal principal) {
        return flightService.getFlights(principal.getName());
    }


    // Step 2.1: Airline changes fares, seat charges and baggage of a flight.
    @PutMapping("/api/airline/flight/{flightId}")
    public FlightRespDto update(@PathVariable long flightId,
                                @Valid @RequestBody FlightUpdateDto flightUpdateDto,
                                Principal principal) {
        return flightService.update(flightId, flightUpdateDto, principal.getName());
    }

    // Step 2.2: Airline activates or deactivates a flight.
    @PutMapping("/api/airline/flight/status/{flightId}")
    public FlightRespDto updateStatus(@PathVariable long flightId,
                                      @RequestParam("active") boolean active,
                                      Principal principal) {
        return flightService.updateStatus(flightId, active, principal.getName());
    }

    // Step 2.3: Anyone can check the status of a flight on a date.
    @GetMapping("/api/flight/status")
    public FlightStatusRespDto getFlightStatus(@RequestParam("flightNumber") String flightNumber,
                                               @RequestParam("date") LocalDate date) {
        return flightService.getFlightStatus(flightNumber, date);
    }

    /*
     Step 3: Search available flights using source, destination and travel date.
     Optional filters: airline, fromTime / toTime (departure time), maxPrice
     Optional sorting: sortBy = PRICE | DEPARTURE | ARRIVAL
     Pagination: page (starts from 0) and size (default 10)
     /api/flight/search?source=MAA&destination=DEL&date=2026-11-10&seatClass=ECONOMY&travellers=2
                       &airline=IndiGo&fromTime=06:00&toTime=12:00&maxPrice=6000&sortBy=PRICE&page=0&size=10
    * */
    @GetMapping("/api/flight/search")
    public FlightFilterDto search(@RequestParam("source") String source,
                                  @RequestParam("destination") String destination,
                                  @RequestParam("date") LocalDate date,
                                  @RequestParam(value = "seatClass", required = false, defaultValue = "ECONOMY") SeatClass seatClass,
                                  @RequestParam(value = "travellers", required = false, defaultValue = "1") Integer travellers,
                                  @RequestParam(value = "airline", required = false) String airline,
                                  @RequestParam(value = "fromTime", required = false) LocalTime fromTime,
                                  @RequestParam(value = "toTime", required = false) LocalTime toTime,
                                  @RequestParam(value = "maxPrice", required = false) Double maxPrice,
                                  @RequestParam(value = "sortBy", required = false, defaultValue = "PRICE") FlightSort sortBy,
                                  @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                  @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        return flightService.search(source, destination, date, seatClass, travellers,
                airline, fromTime, toTime, maxPrice, sortBy, page, size);
    }

    // Step 4: Get routes that have bookable flights.
    @GetMapping("/api/flight/routes")
    public List<RouteAvailabilityDto> getAvailableRoutes() {
        return flightService.getAvailableRoutes();
    }

    // Step 5: Get the seat layout for a particular flight instance.
    @GetMapping("/api/flight/seats/{instanceId}")
    public List<SeatRespDto> getSeats(@PathVariable long instanceId,
                                      @RequestParam(value = "seatClass", required = false) SeatClass seatClass) {
        return flightService.getSeats(instanceId, seatClass);
    }
}