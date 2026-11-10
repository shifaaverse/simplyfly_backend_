package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.FlightDto;
import com.springboot.simplyfly.dto.request.FlightUpdateDto;
import com.springboot.simplyfly.dto.response.FlightFilterDto;
import com.springboot.simplyfly.dto.response.FlightRespDto;
import com.springboot.simplyfly.dto.response.FlightSearchRespDto;
import com.springboot.simplyfly.dto.response.FlightStatusRespDto;
import com.springboot.simplyfly.dto.response.PaginationDto;
import com.springboot.simplyfly.dto.response.RouteAvailabilityDto;
import com.springboot.simplyfly.dto.response.SeatCountDto;
import com.springboot.simplyfly.dto.response.SeatRespDto;
import com.springboot.simplyfly.enums.*;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.mapper.FlightMapper;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.Flight;
import com.springboot.simplyfly.model.FlightInstance;
import com.springboot.simplyfly.model.Route;
import com.springboot.simplyfly.repository.FlightInstanceRepository;
import com.springboot.simplyfly.repository.FlightRepository;
import com.springboot.simplyfly.repository.FlightSeatRepository;
import com.springboot.simplyfly.utility.FareUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightService {

    // Only scheduled and delayed flights can be searched and booked.
    public static final List<FlightStatus> BOOKABLE_STATUSES =
            List.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED);

    private final FlightRepository flightRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final AirlineService airlineService;
    private final RouteService routeService;

    // Add a new flight for the logged-in airline.
    public FlightRespDto add(FlightDto dto, String airlineUsername) {
        Airline airline = airlineService.getApprovedAirline(airlineUsername);

        // Check that the route belongs to the airline and is active.
        Route route = routeService.getOwnedRoute(dto.routeId(), airlineUsername);
        if (!route.isActive())
            throw new InvalidCallException("Route is not active");

        // Validate flight details.
        if (dto.departureTime().equals(dto.arrivalTime()))
            throw new InvalidCallException("Departure and arrival time cannot be same");

        if (flightRepository.existsByFlightNumberAndAirlineId(dto.flightNumber().toUpperCase(), airline.getId()))
            throw new InvalidCallException("Flight number already exists");

        Flight flight = FlightMapper.convertDtoToEntity(dto);
        flight.setRoute(route);
        flight.setAirline(airline);
        flight.setActive(true);

        return FlightMapper.convertEntityToDto(flightRepository.save(flight));
    }

    // Get all flights belonging to an airline.
    public List<FlightRespDto> getFlights(String airlineUsername) {
        return flightRepository.getByAirlineUsername(airlineUsername)
                .stream()
                .map(FlightMapper::convertEntityToDto)
                .toList();
    }

    // Airline changes fares, seat charges and baggage of its flight. Existing bookings keep their old fare.
    public FlightRespDto update(long flightId, FlightUpdateDto dto, String airlineUsername) {
        Flight flight = getOwnedFlight(flightId, airlineUsername);

        flight.setBusinessFare(dto.businessFare());
        flight.setPremiumFare(dto.premiumFare());
        flight.setEconomyFare(dto.economyFare());
        flight.setWindowCharge(dto.windowCharge());
        flight.setAisleCharge(dto.aisleCharge());
        flight.setBaggageInfo(dto.baggageInfo());

        return FlightMapper.convertEntityToDto(flightRepository.save(flight));
    }

    // Airline activates or deactivates its flight. A deactivated flight is not shown in search and cannot be booked.
    public FlightRespDto updateStatus(long flightId, boolean isActive, String airlineUsername) {
        Flight flight = getOwnedFlight(flightId, airlineUsername);

        flight.setActive(isActive);

        return FlightMapper.convertEntityToDto(flightRepository.save(flight));
    }

    // Public flight status of a flight number on a date.
    public FlightStatusRespDto getFlightStatus(String flightNumber, LocalDate date) {
        // Step 1: Find the dated flight
        FlightInstance instance = flightInstanceRepository.getByFlightNumberAndDate(flightNumber.trim(), date)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No flight " + flightNumber + " found on " + date));

        Flight flight = instance.getFlight();
        int delay = instance.getDelayMinutes() == null ? 0 : instance.getDelayMinutes();

        // Step 2: Message for the passenger
        String message = switch (instance.getStatus()) {
            case SCHEDULED -> "Flight is on time";
            case BOARDING -> "Boarding has started";
            case DEPARTED -> "Flight has departed";
            case DELAYED -> "Flight is delayed by " + delay + " minutes";
            case CANCELLED -> "Flight is cancelled. Booked passengers are refunded in full";
            case ARRIVED -> "Flight has arrived";
        };

        // Step 3: Expected departure is the departure time plus the delay
        return new FlightStatusRespDto(
                flight.getFlightNumber(),
                flight.getAirline().getName(),
                flight.getRoute().getSource().getCity(),
                flight.getRoute().getDestination().getCity(),
                instance.getTravelDate(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                instance.getStatus(),
                delay,
                flight.getDepartureTime().plusMinutes(delay),
                message
        );
    }

    // Find a flight and check whether it belongs to the airline.
    public Flight getOwnedFlight(long flightId, String airlineUsername) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        if (!flight.getAirline().getUser().getUsername().equals(airlineUsername))
            throw new OwnershipException("Operation not allowed! Flight belongs to another airline");

        return flight;
    }

    // Calculate flight duration, including overnight flights.
    public static long getDurationMinutes(LocalTime departure, LocalTime arrival) {
        long minutes = Duration.between(departure, arrival).toMinutes();

        return minutes < 0 ? minutes + 24 * 60 : minutes;
    }

    // Get the sort for the chosen option. Price uses the fare of the selected seat class.
    private Sort getSort(FlightSort sortBy, SeatClass seatClass) {
        // "id" is added so flights with the same value keep the same order on every page.
        return switch (sortBy) {
            case DEPARTURE -> Sort.by("flight.departureTime", "id");
            case ARRIVAL -> Sort.by("flight.arrivalTime", "id");
            case PRICE -> switch (seatClass) {
                case BUSINESS -> Sort.by("flight.businessFare", "id");
                case PREMIUM_ECONOMY -> Sort.by("flight.premiumFare", "id");
                case ECONOMY -> Sort.by("flight.economyFare", "id");
            };
        };
    }

    // Search flights by route, date and seat class with optional filters, sorting and paging.
    public FlightFilterDto search(String source, String destination, LocalDate date,
                                  SeatClass seatClass, int travellers,
                                  String airline, LocalTime fromTime, LocalTime toTime,
                                  Double maxPrice, FlightSort sortBy, int page, int size) {
        // Step 1: Validate travel date and page values
        if (date.isBefore(LocalDate.now()))
            throw new InvalidCallException("Travel date cannot be in the past");
        if (page < 0 || size < 1)
            throw new InvalidCallException("Page cannot be negative and size must be at least 1");

        // Step 2: Flights that have already left today are not shown
        LocalTime earliest = fromTime == null ? LocalTime.MIN : fromTime;
        if (date.equals(LocalDate.now()) && LocalTime.now().isAfter(earliest))
            earliest = LocalTime.now();

        // Step 3: Price filter is on the fare shown to the passenger (base fare + tax),
        // so convert it back to base fare (small extra value for decimal rounding)
        Double maxBaseFare = maxPrice == null ? null
                : maxPrice / (1 + FareUtility.TAX_PERCENT / 100) + 0.01;

        // Step 4: Create Pageable with page, size and sorting
        Pageable pageable = PageRequest.of(page, size, getSort(sortBy, seatClass));

        // Step 5: Fetch one page of matching flights
        Page<FlightInstance> pageDto = flightInstanceRepository.filterFlights(
                source.trim(), destination.trim(), date, BOOKABLE_STATUSES, AirlineStatus.APPROVED,
                airline == null || airline.isBlank() ? null : airline,
                earliest, toTime, seatClass, SeatStatus.AVAILABLE, travellers, maxBaseFare, pageable);

        // Step 6: Available seats of the flights in this page (one query, no N+1)
        List<Long> ids = pageDto.stream().map(FlightInstance::getId).toList();

        Map<Long, Long> seatCount = ids.isEmpty() ? Map.of()
                : flightSeatRepository.countSeatsByInstances(ids, seatClass, SeatStatus.AVAILABLE)
                .stream()
                .collect(Collectors.toMap(SeatCountDto::instanceId, SeatCountDto::availableSeats));

        // Step 7: Map flights of this page to response Dto
        List<FlightSearchRespDto> list = pageDto.stream()
                .map(fi -> {
                    Flight f = fi.getFlight();

                    return new FlightSearchRespDto(
                            fi.getId(),
                            f.getAirline().getName(),
                            f.getAirline().getCode(),
                            f.getFlightNumber(),
                            f.getRoute().getSource().getCity(),
                            f.getRoute().getDestination().getCity(),
                            fi.getTravelDate(),
                            f.getDepartureTime(),
                            f.getArrivalTime(),
                            getDurationMinutes(f.getDepartureTime(), f.getArrivalTime()),
                            seatClass,
                            FareUtility.calculateFare(f, seatClass, PassengerCategory.ADULT, null),
                            seatCount.getOrDefault(fi.getId(), 0L),
                            f.getBaggageInfo(),
                            fi.getStatus()
                    );
                })
                .toList();

        // Step 8: Flights + pagination info + airlines for the airline filter
        return new FlightFilterDto(
                list,
                new PaginationDto(pageDto.getTotalPages(), pageDto.getTotalElements(), pageDto.getNumberOfElements()),
                flightInstanceRepository.getAirlineNames(
                        source.trim(), destination.trim(), date, BOOKABLE_STATUSES, AirlineStatus.APPROVED));
    }

    // Get routes that currently have bookable flights.
    public List<RouteAvailabilityDto> getAvailableRoutes() {
        return flightInstanceRepository.getAvailableRoutes(
                LocalDate.now(), BOOKABLE_STATUSES, AirlineStatus.APPROVED);
    }

    // Get seats for a flight instance, optionally filtered by seat class.
    public List<SeatRespDto> getSeats(long instanceId, SeatClass seatClass) {
        if (!flightInstanceRepository.existsById(instanceId))
            throw new ResourceNotFoundException("Flight instance not found");

        var seats = seatClass == null
                ? flightSeatRepository.findByFlightInstanceIdOrderByIdAsc(instanceId)
                : flightSeatRepository.findByFlightInstanceIdAndSeatClassOrderByIdAsc(instanceId, seatClass);

        return seats.stream()
                .map(FlightMapper::convertSeatToDto)
                .toList();
    }
}