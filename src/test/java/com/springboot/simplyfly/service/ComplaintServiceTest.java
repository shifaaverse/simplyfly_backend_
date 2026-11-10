package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.ComplaintDto;
import com.springboot.simplyfly.dto.request.ComplaintResponseDto;
import com.springboot.simplyfly.dto.response.ComplaintRespDto;
import com.springboot.simplyfly.enums.ComplaintCategory;
import com.springboot.simplyfly.enums.ComplaintStatus;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.Booking;
import com.springboot.simplyfly.model.BookingJourney;
import com.springboot.simplyfly.model.Complaint;
import com.springboot.simplyfly.model.Flight;
import com.springboot.simplyfly.model.FlightInstance;
import com.springboot.simplyfly.model.Passenger;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.BookingJourneyRepository;
import com.springboot.simplyfly.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @InjectMocks
    private ComplaintService complaintService;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private BookingJourneyRepository bookingJourneyRepository;

    @Mock
    private PassengerService passengerService;

    @Mock
    private BookingService bookingService;

    @Mock
    private AirlineService airlineService;

    @Mock
    private NotificationService notificationService;

    private Passenger passenger;
    private Booking booking;
    private Airline airline;

    @BeforeEach
    void setUp() {
        User passengerUser = new User("harry", "encoded", Role.PASSENGER);
        passenger = new Passenger();
        passenger.setId(1L);
        passenger.setName("Harry");
        passenger.setUser(passengerUser);

        booking = new Booking();
        booking.setId(1L);
        booking.setPnr("ABC123");
        booking.setPassenger(passenger);

        User airlineUser = new User("indigo", "encoded", Role.AIRLINE);
        airline = new Airline();
        airline.setId(2L);
        airline.setName("IndiGo");
        airline.setUser(airlineUser);
    }

    private Complaint getComplaint(ComplaintStatus status) {
        Complaint complaint = new Complaint();
        complaint.setId(1L);
        complaint.setPassenger(passenger);
        complaint.setCategory(ComplaintCategory.FLIGHT_DELAY);
        complaint.setSubject("Flight delayed");
        complaint.setDescription("Flight was 3 hours late");
        complaint.setStatus(status);
        return complaint;
    }

    @Test
    void addTest() {
        ComplaintDto dto = new ComplaintDto(null, ComplaintCategory.PAYMENT, "Money deducted", "Amount deducted twice");
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintRespDto resp = complaintService.add(dto, "harry");

        assertEquals(ComplaintStatus.OPEN, resp.status());
        assertEquals("Harry", resp.passengerName());
        // booking is optional
        assertNull(resp.pnr());
        verifyNoInteractions(bookingService);
        verify(complaintRepository, times(1)).save(any(Complaint.class));
    }

    @Test
    void addTest_WithBooking() {
        ComplaintDto dto = new ComplaintDto(1L, ComplaintCategory.BOOKING, "Wrong name", "Name is misspelled");
        when(passengerService.getByUsername("harry")).thenReturn(passenger);
        when(bookingService.getOwnedBooking(1L, "harry")).thenReturn(booking);
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintRespDto resp = complaintService.add(dto, "harry");

        assertEquals("ABC123", resp.pnr());
        assertEquals(ComplaintStatus.OPEN, resp.status());
    }

    @Test
    void addTest_OtherPassengerBooking() {
        ComplaintDto dto = new ComplaintDto(1L, ComplaintCategory.BOOKING, "Wrong name", "Name is misspelled");
        when(passengerService.getByUsername("ron")).thenReturn(passenger);
        when(bookingService.getOwnedBooking(1L, "ron"))
                .thenThrow(new OwnershipException("Operation not allowed! Booking belongs to another passenger"));

        assertThrows(OwnershipException.class, () -> complaintService.add(dto, "ron"));

        verify(complaintRepository, times(0)).save(any(Complaint.class));
    }

    @Test
    void addTest_PassengerNotFound() {
        ComplaintDto dto = new ComplaintDto(null, ComplaintCategory.OTHER, "Help", "Need help");
        when(passengerService.getByUsername("ghost")).thenThrow(new ResourceNotFoundException("Passenger not found"));

        assertThrows(ResourceNotFoundException.class, () -> complaintService.add(dto, "ghost"));

        verifyNoInteractions(complaintRepository);
    }

    @Test
    void getMyComplaintsTest() {
        when(complaintRepository.getByPassengerUsername("harry"))
                .thenReturn(List.of(getComplaint(ComplaintStatus.OPEN)));

        List<ComplaintRespDto> list = complaintService.getMyComplaints("harry");

        assertEquals(1, list.size());
        assertEquals("Flight delayed", list.get(0).subject());
    }

    @Test
    void getMyComplaintsTest_NoComplaints() {
        when(complaintRepository.getByPassengerUsername("harry")).thenReturn(List.of());

        List<ComplaintRespDto> list = complaintService.getMyComplaints("harry");

        assertTrue(list.isEmpty());
    }

    @Test
    void getAllTest() {
        when(complaintRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(getComplaint(ComplaintStatus.OPEN), getComplaint(ComplaintStatus.RESOLVED)));

        List<ComplaintRespDto> list = complaintService.getAll();

        assertEquals(2, list.size());
    }

    @Test
    void forwardTest_WithAirlineId() {
        Complaint complaint = getComplaint(ComplaintStatus.OPEN);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(airlineService.getById(2L)).thenReturn(airline);
        when(complaintRepository.save(complaint)).thenReturn(complaint);

        ComplaintRespDto resp = complaintService.forward(1L, 2L);

        assertEquals(ComplaintStatus.FORWARDED_TO_AIRLINE, resp.status());
        assertEquals("IndiGo", resp.airlineName());
    }

    @Test
    void forwardTest_UsesAirlineOfBookedFlight() {
        Complaint complaint = getComplaint(ComplaintStatus.OPEN);
        complaint.setBooking(booking);

        Flight flight = new Flight();
        flight.setAirline(airline);
        FlightInstance instance = new FlightInstance();
        instance.setFlight(flight);
        BookingJourney journey = new BookingJourney();
        journey.setFlightInstance(instance);

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(bookingJourneyRepository.findByBookingIdOrderByIdAsc(1L)).thenReturn(List.of(journey));
        when(complaintRepository.save(complaint)).thenReturn(complaint);

        ComplaintRespDto resp = complaintService.forward(1L, null);

        assertEquals("IndiGo", resp.airlineName());
        assertEquals(ComplaintStatus.FORWARDED_TO_AIRLINE, resp.status());
        verifyNoInteractions(airlineService);
    }

    @Test
    void forwardTest_NoAirlineAndNoBooking() {
        Complaint complaint = getComplaint(ComplaintStatus.OPEN);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        assertThrows(InvalidCallException.class, () -> complaintService.forward(1L, null));

        verify(complaintRepository, times(0)).save(any(Complaint.class));
    }

    @Test
    void forwardTest_NotOpen() {
        Complaint complaint = getComplaint(ComplaintStatus.RESOLVED);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        assertThrows(InvalidCallException.class, () -> complaintService.forward(1L, 2L));

        verifyNoInteractions(airlineService);
        verify(complaintRepository, times(0)).save(any(Complaint.class));
    }

    @Test
    void forwardTest_ComplaintNotFound() {
        when(complaintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> complaintService.forward(99L, 2L));
    }

    @Test
    void getForAirlineTest() {
        Complaint complaint = getComplaint(ComplaintStatus.FORWARDED_TO_AIRLINE);
        complaint.setAirline(airline);
        when(complaintRepository.getByAirlineUsername("indigo")).thenReturn(List.of(complaint));

        List<ComplaintRespDto> list = complaintService.getForAirline("indigo");

        assertEquals(1, list.size());
        assertEquals("IndiGo", list.get(0).airlineName());
    }

    @Test
    void getForAirlineTest_NothingForwarded() {
        when(complaintRepository.getByAirlineUsername("indigo")).thenReturn(List.of());

        List<ComplaintRespDto> list = complaintService.getForAirline("indigo");

        assertTrue(list.isEmpty());
    }

    @Test
    void respondTest() {
        Complaint complaint = getComplaint(ComplaintStatus.FORWARDED_TO_AIRLINE);
        complaint.setAirline(airline);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(complaint)).thenReturn(complaint);

        ComplaintRespDto resp = complaintService.respond(1L,
                new ComplaintResponseDto("Delay was due to weather", ComplaintStatus.RESOLVED), "indigo");

        assertEquals(ComplaintStatus.RESOLVED, resp.status());
        assertEquals("Delay was due to weather", resp.airlineResponse());
        verify(complaintRepository, times(1)).save(complaint);
    }

    @Test
    void respondTest_NotFound() {
        when(complaintRepository.findById(99L)).thenReturn(Optional.empty());
        ComplaintResponseDto dto = new ComplaintResponseDto("Checking", ComplaintStatus.IN_PROGRESS);

        assertThrows(ResourceNotFoundException.class, () -> complaintService.respond(99L, dto, "indigo"));
    }

    @Test
    void respondTest_NotAssignedToAnyAirline() {
        Complaint complaint = getComplaint(ComplaintStatus.OPEN);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        ComplaintResponseDto dto = new ComplaintResponseDto("Checking", ComplaintStatus.IN_PROGRESS);

        assertThrows(OwnershipException.class, () -> complaintService.respond(1L, dto, "indigo"));

        verify(complaintRepository, times(0)).save(any(Complaint.class));
    }

    @Test
    void respondTest_OtherAirline() {
        Complaint complaint = getComplaint(ComplaintStatus.FORWARDED_TO_AIRLINE);
        complaint.setAirline(airline);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        ComplaintResponseDto dto = new ComplaintResponseDto("Checking", ComplaintStatus.IN_PROGRESS);

        assertThrows(OwnershipException.class, () -> complaintService.respond(1L, dto, "airindia"));

        verify(complaintRepository, times(0)).save(any(Complaint.class));
    }

    @Test
    void respondTest_InvalidStatus() {
        Complaint complaint = getComplaint(ComplaintStatus.FORWARDED_TO_AIRLINE);
        complaint.setAirline(airline);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        // airline can only move to IN_PROGRESS or RESOLVED
        ComplaintResponseDto dto = new ComplaintResponseDto("Checking", ComplaintStatus.OPEN);

        assertThrows(InvalidCallException.class, () -> complaintService.respond(1L, dto, "indigo"));

        verify(complaintRepository, times(0)).save(any(Complaint.class));
    }
}