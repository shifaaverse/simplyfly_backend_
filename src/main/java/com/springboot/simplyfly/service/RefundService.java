package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.RefundRequestDto;
import com.springboot.simplyfly.dto.response.RefundRespDto;
import com.springboot.simplyfly.enums.*;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.mapper.RefundMapper;
import com.springboot.simplyfly.model.Booking;
import com.springboot.simplyfly.model.BookingJourney;
import com.springboot.simplyfly.model.FlightInstance;
import com.springboot.simplyfly.model.Refund;
import com.springboot.simplyfly.repository.BookingJourneyRepository;
import com.springboot.simplyfly.repository.BookingRepository;
import com.springboot.simplyfly.repository.RefundRepository;
import com.springboot.simplyfly.utility.FareUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    // Refunds that are still being processed.
    private static final List<RefundStatus> ACTIVE_STATUSES =
            List.of(RefundStatus.REQUESTED, RefundStatus.FORWARDED_TO_AIRLINE,
                    RefundStatus.APPROVED, RefundStatus.PROCESSING);

    private final RefundRepository refundRepository;
    private final BookingJourneyRepository bookingJourneyRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final NotificationService notificationService;

    // Submit a refund request for a booking journey.
    @Transactional(rollbackFor = Exception.class)
    public RefundRespDto request(RefundRequestDto dto, String passengerUsername) {
        BookingJourney journey = bookingJourneyRepository.findById(dto.journeyId())
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found"));

        Booking booking = journey.getBooking();

        // Check whether the booking belongs to the passenger.
        if (!booking.getPassenger().getUser().getUsername().equals(passengerUsername))
            throw new OwnershipException("Operation not allowed! Booking belongs to another passenger");

        // Check whether the booking and journey can be cancelled.
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PARTIALLY_CANCELLED)
            throw new InvalidCallException("Booking is not confirmed");

        if (journey.getStatus() != JourneyStatus.CONFIRMED)
            throw new InvalidCallException("Journey is already " + journey.getStatus());

        if (refundRepository.existsByBookingJourneyIdAndStatusIn(journey.getId(), ACTIVE_STATUSES))
            throw new InvalidCallException("Refund already requested for this journey");

        // Check whether the flight has departed.
        long hoursLeft = getHoursBeforeDeparture(journey.getFlightInstance());

        if (hoursLeft < 0)
            throw new InvalidCallException("Flight has already departed");

        // Calculate the refund amount and save the request.
        Refund refund = new Refund();
        refund.setBookingJourney(journey);
        refund.setReason(dto.reason());
        refund.setRefundAmount(FareUtility.calculateRefund(journey.getAmount(), hoursLeft));
        refund.setStatus(RefundStatus.REQUESTED);

        journey.setStatus(JourneyStatus.CANCELLATION_REQUESTED);
        bookingJourneyRepository.save(journey);

        // Admin has to forward the request to the airline
        notificationService.addForAdmins("New refund request",
                "PNR " + booking.getPnr() + " asked for a refund of " + refund.getRefundAmount());

        return RefundMapper.convertEntityToDto(refundRepository.save(refund));
    }

    // Calculate the hours remaining until departure.
    public static long getHoursBeforeDeparture(FlightInstance instance) {
        LocalDateTime departure = LocalDateTime.of(instance.getTravelDate(), instance.getFlight().getDepartureTime());
        return Duration.between(LocalDateTime.now(), departure).toHours();
    }

    // Get refund requests submitted by a passenger.
    public List<RefundRespDto> getMyRefunds(String passengerUsername) {
        return refundRepository.getByPassengerUsername(passengerUsername)
                .stream()
                .map(RefundMapper::convertEntityToDto)
                .toList();
    }

    // Get all refunds with an optional status filter.
    public List<RefundRespDto> getAll(RefundStatus status) {
        return refundRepository.filterByStatus(status)
                .stream()
                .map(RefundMapper::convertEntityToDto)
                .toList();
    }

    // Find a refund by ID.
    private Refund getById(long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
    }

    // Admin forwards the refund request to the airline.
    public RefundRespDto forward(long refundId) {
        Refund refund = getById(refundId);

        if (refund.getStatus() != RefundStatus.REQUESTED)
            throw new InvalidCallException("Only REQUESTED refunds can be forwarded");

        refund.setStatus(RefundStatus.FORWARDED_TO_AIRLINE);

        // Airline has to approve or reject it
        BookingJourney journey = refund.getBookingJourney();
        notificationService.add(journey.getFlightInstance().getFlight().getAirline().getUser(),
                "Refund request received",
                "PNR " + journey.getBooking().getPnr() + " is waiting for your decision");

        return RefundMapper.convertEntityToDto(refundRepository.save(refund));
    }

    // Get refunds assigned to an airline.
    public List<RefundRespDto> getForAirline(String airlineUsername) {
        return refundRepository.getByAirlineUsername(airlineUsername,
                        List.of(RefundStatus.FORWARDED_TO_AIRLINE, RefundStatus.APPROVED, RefundStatus.PROCESSING,
                                RefundStatus.REFUNDED, RefundStatus.REJECTED, RefundStatus.FAILED))
                .stream()
                .map(RefundMapper::convertEntityToDto)
                .toList();
    }

    // Check whether the refund belongs to the airline and has been forwarded.
    private Refund getForwardedRefund(long refundId, String airlineUsername) {
        Refund refund = getById(refundId);

        String owner = refund.getBookingJourney().getFlightInstance().getFlight()
                .getAirline().getUser().getUsername();

        if (!owner.equals(airlineUsername))
            throw new OwnershipException("Operation not allowed! Refund belongs to another airline");

        if (refund.getStatus() != RefundStatus.FORWARDED_TO_AIRLINE)
            throw new InvalidCallException("Refund is " + refund.getStatus() + ", cannot be decided now");

        return refund;
    }

    // Approve the refund and cancel the journey.
    @Transactional(rollbackFor = Exception.class)
    public RefundRespDto approve(long refundId, String airlineUsername) {
        Refund refund = getForwardedRefund(refundId, airlineUsername);
        BookingJourney journey = refund.getBookingJourney();

        // Mark the refund as completed.
        refund.setStatus(RefundStatus.REFUNDED);

        // Cancel the journey and release its seats.
        journey.setStatus(JourneyStatus.CANCELLED);
        bookingJourneyRepository.save(journey);
        bookingService.updateJourneySeatStatus(journey.getId(), SeatStatus.AVAILABLE);

        // Update the booking status based on its remaining journeys.
        Booking booking = journey.getBooking();
        updateBookingStatus(booking);

        log.info("Refund {} processed for PNR {}", refund.getId(), booking.getPnr());

        notificationService.add(booking.getPassenger().getUser(), "Refund approved",
                "Refund of " + refund.getRefundAmount() + " for PNR " + booking.getPnr() + " is approved");

        return RefundMapper.convertEntityToDto(refundRepository.save(refund));
    }

    // Booking is CANCELLED when all its journeys are cancelled, otherwise PARTIALLY_CANCELLED.
    private void updateBookingStatus(Booking booking) {
        boolean allCancelled = bookingJourneyRepository.findByBookingIdOrderByIdAsc(booking.getId())
                .stream()
                .allMatch(j -> j.getStatus() == JourneyStatus.CANCELLED);

        booking.setStatus(allCancelled ? BookingStatus.CANCELLED : BookingStatus.PARTIALLY_CANCELLED);
        bookingRepository.save(booking);
    }

    // Reject the refund request with a reason.
    @Transactional(rollbackFor = Exception.class)
    public RefundRespDto reject(long refundId, String reason, String airlineUsername) {
        if (reason == null || reason.isBlank())
            throw new InvalidCallException("Rejection reason is required");

        Refund refund = getForwardedRefund(refundId, airlineUsername);
        refund.setStatus(RefundStatus.REJECTED);
        refund.setRejectionReason(reason);

        // Keep the journey confirmed.
        BookingJourney journey = refund.getBookingJourney();
        journey.setStatus(JourneyStatus.CONFIRMED);
        bookingJourneyRepository.save(journey);

        notificationService.add(journey.getBooking().getPassenger().getUser(), "Refund rejected",
                "Refund for PNR " + journey.getBooking().getPnr() + " was rejected. Reason: " + reason);

        return RefundMapper.convertEntityToDto(refundRepository.save(refund));
    }

    /*
     * Airline cancelled the flight: every passenger gets the full journey amount back automatically.
     * - Paid journeys (and pending refund requests) -> REFUNDED, journey CANCELLED
     * - Journeys still waiting for payment -> booking EXPIRED, seats released
     * Returns the number of journeys refunded.
     * */
    @Transactional(rollbackFor = Exception.class)
    public int refundCancelledFlight(FlightInstance instance) {
        // Step 1: Find the journeys of this flight that are paid or waiting for payment
        List<BookingJourney> journeys = bookingJourneyRepository.findByFlightInstanceIdAndStatusIn(instance.getId(),
                List.of(JourneyStatus.CONFIRMED, JourneyStatus.CANCELLATION_REQUESTED, JourneyStatus.PENDING_PAYMENT));

        String flightText = instance.getFlight().getFlightNumber() + " on " + instance.getTravelDate();
        int refunded = 0;

        // Step 2: Release unpaid bookings, refund the paid ones
        for (BookingJourney journey : journeys) {
            if (journey.getStatus() == JourneyStatus.PENDING_PAYMENT) {
                expireUnpaidBooking(journey.getBooking());
                continue;
            }
            refundJourney(journey, flightText);
            refunded++;
        }

        log.info("Flight {} cancelled, {} journeys refunded", flightText, refunded);
        return refunded;
    }

    // Not paid yet - nothing to refund, so release the seats.
    private void expireUnpaidBooking(Booking booking) {
        bookingService.updateSeatStatus(booking.getId(), SeatStatus.AVAILABLE);
        bookingService.updateJourneyStatus(booking.getId(), JourneyStatus.EXPIRED);
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
    }

    // Full refund of one journey, then tell the passenger.
    private void refundJourney(BookingJourney journey, String flightText) {
        Booking booking = journey.getBooking();

        // Use the refund request already raised by the passenger, otherwise create a new one
        Refund refund = refundRepository.findByBookingJourneyIdAndStatusIn(journey.getId(), ACTIVE_STATUSES)
                .stream()
                .findFirst()
                .orElseGet(Refund::new);

        refund.setBookingJourney(journey);
        refund.setReason("Flight cancelled by airline");
        refund.setRefundAmount(journey.getAmount());
        refund.setStatus(RefundStatus.REFUNDED);
        refundRepository.save(refund);

        // Cancel the journey and update the booking
        journey.setStatus(JourneyStatus.CANCELLED);
        bookingJourneyRepository.save(journey);
        bookingService.updateJourneySeatStatus(journey.getId(), SeatStatus.AVAILABLE);
        updateBookingStatus(booking);

        notificationService.add(booking.getPassenger().getUser(), "Flight cancelled - full refund",
                "Flight " + flightText + " was cancelled by the airline. Full refund of "
                        + refund.getRefundAmount() + " is issued for PNR " + booking.getPnr());
    }
}