package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.RouteDto;
import com.springboot.simplyfly.dto.response.RouteRespDto;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.mapper.RouteMapper;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.Airport;
import com.springboot.simplyfly.model.Route;
import com.springboot.simplyfly.repository.AirportRepository;
import com.springboot.simplyfly.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final AirportRepository airportRepository;
    private final AirlineService airlineService;

    // Add a new route for the logged-in airline.
    public RouteRespDto add(RouteDto dto, String airlineUsername) {
        Airline airline = airlineService.getApprovedAirline(airlineUsername);

        // Validate the source and destination airports.
        Airport[] airports = validateAirports(dto);

        // Check whether the route already exists for this airline.
        if (routeRepository.existsBySourceIdAndDestinationIdAndAirlineId(
                airports[0].getId(), airports[1].getId(), airline.getId()))
            throw new InvalidCallException("Route already exists");

        Route route = new Route();
        route.setSource(airports[0]);
        route.setDestination(airports[1]);
        route.setAirline(airline);
        route.setActive(true);

        return RouteMapper.convertEntityToDto(routeRepository.save(route));
    }

    // Validate and fetch the source and destination airports.
    private Airport[] validateAirports(RouteDto dto) {
        if (dto.sourceAirportId().equals(dto.destinationAirportId()))
            throw new InvalidCallException("Source and destination cannot be same");

        Airport source = airportRepository.findById(dto.sourceAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Source airport not found"));

        Airport destination = airportRepository.findById(dto.destinationAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination airport not found"));

        return new Airport[]{source, destination};
    }

    // Get all routes belonging to an airline.
    public List<RouteRespDto> getRoutes(String airlineUsername) {
        return routeRepository.getByAirlineUsername(airlineUsername)
                .stream()
                .map(RouteMapper::convertEntityToDto)
                .toList();
    }

    // Find a route and check whether it belongs to the airline.
    public Route getOwnedRoute(long routeId, String airlineUsername) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        if (!route.getAirline().getUser().getUsername().equals(airlineUsername))
            throw new OwnershipException("Operation not allowed! Route belongs to another airline");

        return route;
    }

    // Update the source and destination of a route.
    public RouteRespDto update(long routeId, RouteDto dto, String airlineUsername) {
        Route route = getOwnedRoute(routeId, airlineUsername);
        Airport[] airports = validateAirports(dto);

        route.setSource(airports[0]);
        route.setDestination(airports[1]);

        return RouteMapper.convertEntityToDto(routeRepository.save(route));
    }

    // Activate or deactivate a route.
    public RouteRespDto updateStatus(long routeId, boolean isActive, String airlineUsername) {
        Route route = getOwnedRoute(routeId, airlineUsername);
        route.setActive(isActive);

        return RouteMapper.convertEntityToDto(routeRepository.save(route));
    }
}