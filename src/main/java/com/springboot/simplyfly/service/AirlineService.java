package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.AirlineDto;
import com.springboot.simplyfly.dto.request.AirlineUpdateDto;
import com.springboot.simplyfly.dto.response.AirlineRespDto;
import com.springboot.simplyfly.dto.response.AirlineStatDto;
import com.springboot.simplyfly.enums.*;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.mapper.AirlineMapper;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirlineService {

    private final AirlineRepository airlineRepository;
    private final UserService userService;
    private final RouteRepository routeRepository;
    private final FlightRepository flightRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final BookingJourneyRepository bookingJourneyRepository;
    private final TravellerSeatRepository travellerSeatRepository;
    private final RefundRepository refundRepository;
    private final NotificationService notificationService;

    // Register an airline with PENDING status.
    @Transactional(rollbackFor = Exception.class)
    public AirlineRespDto register(AirlineDto dto) {
        AirlineRespDto airline = save(dto, AirlineStatus.PENDING);

        // Admin has to approve the new airline
        notificationService.addForAdmins("New airline registration", airline.name() + " is waiting for your approval");

        return airline;
    }

    // Register an airline directly with APPROVED status.
    @Transactional(rollbackFor = Exception.class)
    public AirlineRespDto onboard(AirlineDto dto) {
        return save(dto, AirlineStatus.APPROVED);
    }

    // Save airline and user details.
    private AirlineRespDto save(AirlineDto dto, AirlineStatus status) {
        // Check whether the airline code already exists.
        if (airlineRepository.existsByCode(dto.code().toUpperCase()))
            throw new InvalidCallException("Airline code already registered");

        User user = userService.getUserObj(dto.username(), dto.password(), Role.AIRLINE);

        // Allow login only when the airline is approved.
        user.setActive(status == AirlineStatus.APPROVED);

        Airline airline = AirlineMapper.convertDtoToEntity(dto);

        airline.setUser(user);
        airline.setStatus(status);

        airline = airlineRepository.save(airline);
        log.info("Airline {} saved with status {}", airline.getCode(), status);

        return AirlineMapper.convertEntityToDto(airline);
    }

    // Find an airline by ID.
    public Airline getById(long id) {
        return airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found"));
    }

    // Find an airline by username.
    public Airline getByUsername(String username) {
        Airline airline = airlineRepository.getByUsername(username);
        if (airline == null)
            throw new ResourceNotFoundException("Airline not found");
        return airline;
    }

    // Check whether the airline is approved.
    public Airline getApprovedAirline(String username) {
        Airline airline = getByUsername(username);
        if (airline.getStatus() != AirlineStatus.APPROVED)
            throw new OwnershipException("Airline is not approved to operate");
        return airline;
    }

    // Get all airlines or filter them by status.
    public List<AirlineRespDto> getAll(AirlineStatus status) {
        List<Airline> list = status == null ? airlineRepository.findAll() : airlineRepository.findByStatus(status);
        return list.stream()
                .map(AirlineMapper::convertEntityToDto)
                .toList();
    }

    // Admin updates an airline's approval status. A reason is compulsory to reject or suspend.
    public AirlineRespDto updateStatus(long airlineId, AirlineStatus status, String reason) {
        if (status == AirlineStatus.PENDING)
            throw new InvalidCallException("Airline cannot be moved back to PENDING");

        boolean needsReason = status == AirlineStatus.REJECTED || status == AirlineStatus.SUSPENDED;
        if (needsReason && (reason == null || reason.isBlank()))
            throw new InvalidCallException("Reason is required to reject or suspend an airline");

        Airline airline = getById(airlineId);

        airline.setStatus(status);
        airline.setRejectionReason(needsReason ? reason.trim() : null);
        userService.updateActive(airline.getUser(), status == AirlineStatus.APPROVED);

        return AirlineMapper.convertEntityToDto(airlineRepository.save(airline));
    }

    // Get the airline profile.
    public AirlineRespDto getProfile(String username) {
        return AirlineMapper.convertEntityToDto(getByUsername(username));
    }

    // Update the airline profile.
    public AirlineRespDto updateProfile(String username, AirlineUpdateDto dto) {
        Airline airline = getByUsername(username);
        boolean flag = false;

        if (dto.email() != null && !dto.email().isBlank()) {
            airline.setEmail(dto.email());
            flag = true;
        }
        if (dto.contact() != null && !dto.contact().isBlank()) {
            airline.setContact(dto.contact());
            flag = true;
        }
        if (dto.address() != null && !dto.address().isBlank()) {
            airline.setAddress(dto.address());
            flag = true;
        }
        if (dto.logoUrl() != null && !dto.logoUrl().isBlank()) {
            airline.setLogoUrl(dto.logoUrl());
            flag = true;
        }
        if (!flag)
            throw new InvalidCallException("Update op terminated");

        return AirlineMapper.convertEntityToDto(airlineRepository.save(airline));
    }

    // Get statistics for an airline.
    public AirlineStatDto getStats(String username) {
        LocalDate today = LocalDate.now();
        List<BookingStatus> bookedStatuses =
                List.of(BookingStatus.CONFIRMED, BookingStatus.PARTIALLY_CANCELLED, BookingStatus.CANCELLED);

        return new AirlineStatDto(
                routeRepository.countByAirlineUsername(username),
                flightRepository.countByAirlineUsername(username),
                flightInstanceRepository.countByAirlineAndDate(username, today),
                flightInstanceRepository.countUpcomingByAirline(username, today),
                bookingJourneyRepository.countByAirlineAndBookingStatus(username, bookedStatuses),
                travellerSeatRepository.countTravellersByAirline(username, JourneyStatus.CONFIRMED,
                        List.of(BookingStatus.CONFIRMED, BookingStatus.PARTIALLY_CANCELLED)),
                flightSeatRepository.countSeatsByAirline(username, SeatStatus.AVAILABLE, today),
                bookingJourneyRepository.countByAirlineAndJourneyStatus(username, JourneyStatus.CANCELLED),
                refundRepository.countByAirlineAndStatus(username, RefundStatus.FORWARDED_TO_AIRLINE),
                bookingJourneyRepository.sumAmountByAirline(username, JourneyStatus.CONFIRMED,
                        List.of(BookingStatus.CONFIRMED, BookingStatus.PARTIALLY_CANCELLED))
        );
    }
}