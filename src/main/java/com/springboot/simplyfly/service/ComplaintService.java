package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.ComplaintDto;
import com.springboot.simplyfly.dto.request.ComplaintResponseDto;
import com.springboot.simplyfly.dto.response.ComplaintRespDto;
import com.springboot.simplyfly.enums.ComplaintStatus;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.mapper.ComplaintMapper;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.BookingJourney;
import com.springboot.simplyfly.model.Complaint;
import com.springboot.simplyfly.repository.BookingJourneyRepository;
import com.springboot.simplyfly.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingJourneyRepository bookingJourneyRepository;
    private final PassengerService passengerService;
    private final BookingService bookingService;
    private final AirlineService airlineService;
    private final NotificationService notificationService;

    // Create a complaint for the logged-in passenger.
    public ComplaintRespDto add(ComplaintDto dto, String passengerUsername) {
        Complaint complaint = ComplaintMapper.convertDtoToEntity(dto);
        complaint.setPassenger(passengerService.getByUsername(passengerUsername));

        // Attach the booking if provided.
        if (dto.bookingId() != null)
            complaint.setBooking(bookingService.getOwnedBooking(dto.bookingId(), passengerUsername));

        complaint.setStatus(ComplaintStatus.OPEN);

        // Admin has to look at the new complaint
        notificationService.addForAdmins("New complaint", "A passenger raised a complaint: " + dto.subject());

        return ComplaintMapper.convertEntityToDto(complaintRepository.save(complaint));
    }

    // Get complaints submitted by a passenger.
    public List<ComplaintRespDto> getMyComplaints(String passengerUsername) {
        return complaintRepository.getByPassengerUsername(passengerUsername)
                .stream()
                .map(ComplaintMapper::convertEntityToDto)
                .toList();
    }

    // Get all complaints for admin.
    public List<ComplaintRespDto> getAll() {
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ComplaintMapper::convertEntityToDto)
                .toList();
    }

    // Forward an open complaint to an airline.
    public ComplaintRespDto forward(long complaintId, Long airlineId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (complaint.getStatus() != ComplaintStatus.OPEN)
            throw new InvalidCallException("Only OPEN complaints can be forwarded");

        Airline airline;

        if (airlineId != null) {
            airline = airlineService.getById(airlineId);
        } else if (complaint.getBooking() != null) {
            List<BookingJourney> journeys =
                    bookingJourneyRepository.findByBookingIdOrderByIdAsc(complaint.getBooking().getId());
            airline = journeys.get(0).getFlightInstance().getFlight().getAirline();
        } else {
            throw new InvalidCallException("Select an airline to forward this complaint");
        }

        complaint.setAirline(airline);
        complaint.setStatus(ComplaintStatus.FORWARDED_TO_AIRLINE);

        // Airline has to respond to it
        notificationService.add(airline.getUser(), "Complaint received", "A complaint was forwarded to you: " + complaint.getSubject());

        return ComplaintMapper.convertEntityToDto(complaintRepository.save(complaint));
    }

    // Get complaints assigned to an airline.
    public List<ComplaintRespDto> getForAirline(String airlineUsername) {
        return complaintRepository.getByAirlineUsername(airlineUsername)
                .stream()
                .map(ComplaintMapper::convertEntityToDto)
                .toList();
    }

    // Allow the assigned airline to respond to a complaint.
    public ComplaintRespDto respond(long complaintId, ComplaintResponseDto dto, String airlineUsername) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (complaint.getAirline() == null
                || !complaint.getAirline().getUser().getUsername().equals(airlineUsername))
            throw new OwnershipException("Operation not allowed! Complaint not assigned to you");

        if (dto.status() != ComplaintStatus.IN_PROGRESS && dto.status() != ComplaintStatus.RESOLVED)
            throw new InvalidCallException("Status must be IN_PROGRESS or RESOLVED");

        complaint.setAirlineResponse(dto.response());
        complaint.setStatus(dto.status());

        // Passenger gets the answer
        notificationService.add(complaint.getPassenger().getUser(), "Complaint update",
                "Your complaint '" + complaint.getSubject() + "' is now " + dto.status());

        return ComplaintMapper.convertEntityToDto(complaintRepository.save(complaint));
    }
}