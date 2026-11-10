package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.request.RouteDto;
import com.springboot.simplyfly.dto.response.RouteRespDto;
import com.springboot.simplyfly.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RouteController {

    private final RouteService routeService;

    // Airline adds a new route.
    @PostMapping("/api/airline/route/add")
    public RouteRespDto add(@Valid @RequestBody RouteDto routeDto, Principal principal) {
        return routeService.add(routeDto, principal.getName());
    }

    // Airline views its routes.
    @GetMapping("/api/airline/route/all")
    public List<RouteRespDto> getRoutes(Principal principal) {
        return routeService.getRoutes(principal.getName());

    }

    // Airline updates an existing route.
    @PutMapping("/api/airline/route/{routeId}")
    public RouteRespDto update(@PathVariable long routeId,
                               @Valid @RequestBody RouteDto routeDto,
                               Principal principal) {
        return routeService.update(routeId, routeDto, principal.getName());
    }

    // Airline activates or deactivates a route.
    @PutMapping("/api/airline/route/status/{routeId}")
    public RouteRespDto updateStatus(@PathVariable long routeId,
                                     @RequestParam("active") boolean active,
                                     Principal principal) {
        return routeService.updateStatus(routeId, active, principal.getName());
    }
}