package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.AirlineDto;
import com.springboot.simplyfly.dto.request.AirlineUpdateDto;
import com.springboot.simplyfly.dto.response.AirlineRespDto;
import com.springboot.simplyfly.dto.response.AirlineStatDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.enums.RefundStatus;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.enums.SeatStatus;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.AirlineRepository;
import com.springboot.simplyfly.repository.BookingJourneyRepository;
import com.springboot.simplyfly.repository.FlightInstanceRepository;
import com.springboot.simplyfly.repository.FlightRepository;
import com.springboot.simplyfly.repository.FlightSeatRepository;
import com.springboot.simplyfly.repository.RefundRepository;
import com.springboot.simplyfly.repository.RouteRepository;
import com.springboot.simplyfly.repository.TravellerSeatRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirlineServiceTest {

    @InjectMocks
    private AirlineService airlineService;

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private UserService userService;

    // Additional repositories required for getStats()
    @Mock
    private RouteRepository routeRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private FlightSeatRepository flightSeatRepository;

    @Mock
    private BookingJourneyRepository bookingJourneyRepository;

    @Mock
    private TravellerSeatRepository travellerSeatRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private NotificationService notificationService;

    // Common test data
    private final AirlineDto dto = new AirlineDto("IndiGo", "6e", "REG-001", "ops@indigo.in",
            "9876543210", "Gurgaon", null, "indigo", "indigo@123");

    // Helper method to create airline
    private Airline getAirline(AirlineStatus status) {

        User user = new User("indigo", "encoded", Role.AIRLINE);

        Airline airline = new Airline();
        airline.setId(1L);
        airline.setName("IndiGo");
        airline.setCode("6E");
        airline.setEmail("ops@indigo.in");
        airline.setContact("9876543210");
        airline.setUser(user);
        airline.setStatus(status);

        return airline;
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void registerTest() {

        User user = new User("indigo", "encoded", Role.AIRLINE);

        when(airlineRepository.existsByCode("6E")).thenReturn(false);
        when(userService.getUserObj("indigo", "indigo@123", Role.AIRLINE)).thenReturn(user);
        when(airlineRepository.save(any(Airline.class))).thenAnswer(i -> i.getArgument(0));

        AirlineRespDto resp = airlineService.register(dto);

        assertEquals(AirlineStatus.PENDING, resp.status());
        assertEquals("6E", resp.code());
        // admin is told about the new registration
        verify(notificationService, times(1)).addForAdmins(eq("New airline registration"), anyString());

        // Pending airline cannot login
        assertFalse(user.isActive());

        verify(airlineRepository, times(1)).save(any(Airline.class));
    }

    @Test
    void registerTest_DuplicateCode() {

        when(airlineRepository.existsByCode("6E")).thenReturn(true);

        assertThrows(InvalidCallException.class, () -> airlineService.register(dto));

        verifyNoInteractions(userService);
    }

    // ==================== ONBOARD TESTS ====================

    @Test
    void onboardTest() {

        User user = new User("indigo", "encoded", Role.AIRLINE);

        when(airlineRepository.existsByCode("6E")).thenReturn(false);
        when(userService.getUserObj("indigo", "indigo@123", Role.AIRLINE)).thenReturn(user);
        when(airlineRepository.save(any(Airline.class))).thenAnswer(i -> i.getArgument(0));

        AirlineRespDto resp = airlineService.onboard(dto);

        // Admin onboarded airline is approved and can login immediately
        assertEquals(AirlineStatus.APPROVED, resp.status());
        assertTrue(user.isActive());
    }

    @Test
    void onboardTest_DuplicateCode() {

        when(airlineRepository.existsByCode("6E")).thenReturn(true);

        assertThrows(InvalidCallException.class, () -> airlineService.onboard(dto));

        verifyNoInteractions(userService);
        verify(airlineRepository, times(0)).save(any(Airline.class));
    }

    // ==================== FETCH AIRLINE TESTS ====================

    @Test
    void getByIdTest() {

        when(airlineRepository.findById(1L))
                .thenReturn(Optional.of(getAirline(AirlineStatus.APPROVED)));

        Airline airline = airlineService.getById(1L);

        assertEquals("IndiGo", airline.getName());
    }

    @Test
    void getByIdTest_NotFound() {

        when(airlineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> airlineService.getById(99L));
    }

    @Test
    void getByUsernameTest() {

        when(airlineRepository.getByUsername("indigo"))
                .thenReturn(getAirline(AirlineStatus.APPROVED));

        Airline airline = airlineService.getByUsername("indigo");

        assertEquals("6E", airline.getCode());
    }

    @Test
    void getByUsernameTest_NotFound() {

        when(airlineRepository.getByUsername("ghost")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> airlineService.getByUsername("ghost"));
    }

    // ==================== APPROVED AIRLINE TESTS ====================

    @Test
    void getApprovedAirlineTest() {

        when(airlineRepository.getByUsername("indigo"))
                .thenReturn(getAirline(AirlineStatus.APPROVED));

        Airline airline = airlineService.getApprovedAirline("indigo");

        assertEquals(AirlineStatus.APPROVED, airline.getStatus());
    }

    @Test
    void getApprovedAirlineTest_Pending() {

        when(airlineRepository.getByUsername("indigo"))
                .thenReturn(getAirline(AirlineStatus.PENDING));

        assertThrows(OwnershipException.class, () -> airlineService.getApprovedAirline("indigo"));
    }

    @Test
    void approvedAirlineTest_NotApproved() {

        Airline airline = new Airline();
        airline.setStatus(AirlineStatus.SUSPENDED);

        when(airlineRepository.getByUsername("indigo")).thenReturn(airline);

        assertThrows(OwnershipException.class, () -> airlineService.getApprovedAirline("indigo"));
    }

    // ==================== GET ALL AIRLINES TESTS ====================

    @Test
    void getAllTest_AllStatuses() {

        when(airlineRepository.findAll()).thenReturn(List.of(
                getAirline(AirlineStatus.APPROVED),
                getAirline(AirlineStatus.PENDING)
        ));

        List<AirlineRespDto> list = airlineService.getAll(null);

        assertEquals(2, list.size());

        verify(airlineRepository, times(0)).findByStatus(any(AirlineStatus.class));
    }

    @Test
    void getAllTest_ByStatus() {

        when(airlineRepository.findByStatus(AirlineStatus.PENDING))
                .thenReturn(List.of(getAirline(AirlineStatus.PENDING)));

        List<AirlineRespDto> list = airlineService.getAll(AirlineStatus.PENDING);

        assertEquals(1, list.size());
        assertEquals(AirlineStatus.PENDING, list.get(0).status());
    }

    @Test
    void getAllTest_NoAirlineWithStatus() {

        when(airlineRepository.findByStatus(AirlineStatus.REJECTED)).thenReturn(List.of());

        List<AirlineRespDto> list = airlineService.getAll(AirlineStatus.REJECTED);

        assertTrue(list.isEmpty());
    }

    // ==================== UPDATE STATUS TESTS ====================

    @Test
    void approveTest() {

        User user = new User("indigo", "encoded", Role.AIRLINE);

        Airline airline = new Airline();
        airline.setId(1L);
        airline.setUser(user);
        airline.setStatus(AirlineStatus.PENDING);

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(airline)).thenReturn(airline);

        AirlineRespDto resp = airlineService.updateStatus(1L, AirlineStatus.APPROVED, null);

        assertEquals(AirlineStatus.APPROVED, resp.status());
        assertNull(resp.rejectionReason());

        verify(userService, times(1)).updateActive(user, true);
    }

    @Test
    void rejectTest() {

        Airline airline = getAirline(AirlineStatus.PENDING);

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(airline)).thenReturn(airline);

        AirlineRespDto resp = airlineService.updateStatus(1L, AirlineStatus.REJECTED, "Documents are not clear");

        assertEquals(AirlineStatus.REJECTED, resp.status());
        assertEquals("Documents are not clear", resp.rejectionReason());

        // Rejected airline cannot login
        verify(userService, times(1)).updateActive(airline.getUser(), false);
    }

    @Test
    void updateStatusTest_BackToPending() {

        assertThrows(InvalidCallException.class,
                () -> airlineService.updateStatus(1L, AirlineStatus.PENDING, null));

        verifyNoInteractions(airlineRepository);
        verifyNoInteractions(userService);
    }

    @Test
    void updateStatusTest_ReasonRequired() {

        // reject and suspend need a reason
        assertThrows(InvalidCallException.class,
                () -> airlineService.updateStatus(1L, AirlineStatus.REJECTED, " "));
        assertThrows(InvalidCallException.class,
                () -> airlineService.updateStatus(1L, AirlineStatus.SUSPENDED, null));

        verifyNoInteractions(airlineRepository);
        verifyNoInteractions(userService);
    }

    @Test
    void approveTest_ClearsOldReason() {

        Airline airline = getAirline(AirlineStatus.SUSPENDED);
        airline.setRejectionReason("Safety audit");

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(airline)).thenReturn(airline);

        AirlineRespDto resp = airlineService.updateStatus(1L, AirlineStatus.APPROVED, null);

        assertEquals(AirlineStatus.APPROVED, resp.status());
        assertNull(resp.rejectionReason());
    }

    @Test
    void updateStatusTest_AirlineNotFound() {

        when(airlineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> airlineService.updateStatus(99L, AirlineStatus.APPROVED, null));

        verifyNoInteractions(userService);
    }

    // ==================== PROFILE TESTS ====================

    @Test
    void getProfileTest() {

        when(airlineRepository.getByUsername("indigo"))
                .thenReturn(getAirline(AirlineStatus.APPROVED));

        AirlineRespDto resp = airlineService.getProfile("indigo");

        assertEquals("IndiGo", resp.name());
        assertEquals("indigo", resp.username());
    }

    @Test
    void getProfileTest_NotFound() {

        when(airlineRepository.getByUsername("ghost")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> airlineService.getProfile("ghost"));
    }

    @Test
    void updateProfileTest() {

        Airline airline = getAirline(AirlineStatus.APPROVED);

        when(airlineRepository.getByUsername("indigo")).thenReturn(airline);
        when(airlineRepository.save(airline)).thenReturn(airline);

        AirlineRespDto resp = airlineService.updateProfile("indigo",
                new AirlineUpdateDto("new@indigo.in", null, "Mumbai", null));

        assertEquals("new@indigo.in", resp.email());
        assertEquals("Mumbai", resp.address());

        // Contact was not sent, so it is untouched
        assertEquals("9876543210", resp.contact());

        verify(airlineRepository, times(1)).save(airline);
    }

    @Test
    void updateProfileTest_NothingToUpdate() {

        when(airlineRepository.getByUsername("indigo"))
                .thenReturn(getAirline(AirlineStatus.APPROVED));

        AirlineUpdateDto emptyDto = new AirlineUpdateDto(null, "  ", null, "");

        assertThrows(InvalidCallException.class,
                () -> airlineService.updateProfile("indigo", emptyDto));

        verify(airlineRepository, times(0)).save(any(Airline.class));
    }

    // ==================== AIRLINE STATISTICS TESTS ====================

    @Test
    void getStatsTest() {

        when(routeRepository.countByAirlineUsername("indigo")).thenReturn(3L);
        when(flightRepository.countByAirlineUsername("indigo")).thenReturn(4L);

        when(flightInstanceRepository.countByAirlineAndDate(eq("indigo"), any(LocalDate.class)))
                .thenReturn(2L);

        when(flightInstanceRepository.countUpcomingByAirline(eq("indigo"), any(LocalDate.class)))
                .thenReturn(10L);

        when(bookingJourneyRepository.countByAirlineAndBookingStatus(eq("indigo"), anyList()))
                .thenReturn(6L);

        when(travellerSeatRepository.countTravellersByAirline(
                eq("indigo"), eq(JourneyStatus.CONFIRMED), anyList()))
                .thenReturn(9L);

        when(flightSeatRepository.countSeatsByAirline(
                eq("indigo"), eq(SeatStatus.AVAILABLE), any(LocalDate.class)))
                .thenReturn(500L);

        when(bookingJourneyRepository.countByAirlineAndJourneyStatus(
                "indigo", JourneyStatus.CANCELLED))
                .thenReturn(1L);

        when(refundRepository.countByAirlineAndStatus(
                "indigo", RefundStatus.FORWARDED_TO_AIRLINE))
                .thenReturn(2L);

        when(bookingJourneyRepository.sumAmountByAirline(
                eq("indigo"), eq(JourneyStatus.CONFIRMED), anyList()))
                .thenReturn(45000.0);

        AirlineStatDto stats = airlineService.getStats("indigo");

        assertEquals(3, stats.totalRoutes());
        assertEquals(4, stats.totalFlights());
        assertEquals(2, stats.todaysFlights());
        assertEquals(10, stats.upcomingFlights());
        assertEquals(6, stats.totalBookings());
        assertEquals(9, stats.totalPassengers());
        assertEquals(500, stats.availableSeats());
        assertEquals(1, stats.cancelledJourneys());
        assertEquals(2, stats.pendingRefunds());
        assertEquals(45000.0, stats.revenue());
    }

    @Test
    void getStatsTest_NewAirline() {

        // Nothing is stubbed, a new airline has no data at all
        AirlineStatDto stats = airlineService.getStats("indigo");

        assertEquals(0, stats.totalRoutes());
        assertEquals(0, stats.totalBookings());
        assertEquals(0.0, stats.revenue());
    }
}
