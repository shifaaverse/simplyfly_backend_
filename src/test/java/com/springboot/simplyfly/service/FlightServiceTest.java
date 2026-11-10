package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.FlightDto;
import com.springboot.simplyfly.dto.request.FlightUpdateDto;
import com.springboot.simplyfly.dto.response.FlightFilterDto;
import com.springboot.simplyfly.dto.response.FlightRespDto;
import com.springboot.simplyfly.dto.response.FlightStatusRespDto;
import com.springboot.simplyfly.dto.response.RouteAvailabilityDto;
import com.springboot.simplyfly.dto.response.SeatCountDto;
import com.springboot.simplyfly.dto.response.SeatRespDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.enums.FlightSort;
import com.springboot.simplyfly.enums.FlightStatus;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.enums.SeatPosition;
import com.springboot.simplyfly.enums.SeatStatus;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.Airport;
import com.springboot.simplyfly.model.Flight;
import com.springboot.simplyfly.model.FlightInstance;
import com.springboot.simplyfly.model.FlightSeat;
import com.springboot.simplyfly.model.Route;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.FlightInstanceRepository;
import com.springboot.simplyfly.repository.FlightRepository;
import com.springboot.simplyfly.repository.FlightSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @InjectMocks
    private FlightService flightService;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private FlightSeatRepository flightSeatRepository;

    @Mock
    private AirlineService airlineService;

    @Mock
    private RouteService routeService;

    private Airline airline;
    private Route route;
    private Flight flight;
    private FlightInstance instance;

    @BeforeEach
    void setUp() {
        User airlineUser = new User("indigo", "encoded", Role.AIRLINE);
        airline = new Airline();
        airline.setId(1L);
        airline.setName("IndiGo");
        airline.setCode("6E");
        airline.setUser(airlineUser);
        airline.setStatus(AirlineStatus.APPROVED);

        Airport chennai = new Airport();
        chennai.setId(1L);
        chennai.setCode("MAA");
        chennai.setCity("Chennai");
        Airport delhi = new Airport();
        delhi.setId(2L);
        delhi.setCode("DEL");
        delhi.setCity("Delhi");

        route = new Route();
        route.setId(1L);
        route.setSource(chennai);
        route.setDestination(delhi);
        route.setAirline(airline);
        route.setActive(true);

        flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("6E-201");
        flight.setRoute(route);
        flight.setAirline(airline);
        flight.setDepartureTime(LocalTime.of(13, 30));
        flight.setArrivalTime(LocalTime.of(16, 30));
        flight.setBusinessFare(12000);
        flight.setPremiumFare(8000);
        flight.setEconomyFare(4000);
        flight.setWindowCharge(300);
        flight.setAisleCharge(200);
        flight.setBaggageInfo("15kg");
        flight.setActive(true);

        instance = new FlightInstance();
        instance.setId(100L);
        instance.setFlight(flight);
        instance.setTravelDate(LocalDate.now().plusDays(2));
        instance.setStatus(FlightStatus.SCHEDULED);
    }

    private FlightDto getFlightDto() {
        return new FlightDto("6e-201", 1L, LocalTime.of(13, 30), LocalTime.of(16, 30),
                1, 1, 3, 12000, 8000, 4000, 300, 200, "15kg");
    }

    private FlightSeat getSeat(long id, String number, SeatPosition position) {
        FlightSeat seat = new FlightSeat();
        seat.setId(id);
        seat.setFlightInstance(instance);
        seat.setSeatNumber(number);
        seat.setSeatClass(SeatClass.ECONOMY);
        seat.setSeatPosition(position);
        seat.setStatus(SeatStatus.AVAILABLE);
        return seat;
    }

    @Test
    void addTest() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(routeService.getOwnedRoute(1L, "indigo")).thenReturn(route);
        when(flightRepository.existsByFlightNumberAndAirlineId("6E-201", 1L)).thenReturn(false);
        when(flightRepository.save(any(Flight.class))).thenAnswer(i -> i.getArgument(0));

        FlightRespDto dto = flightService.add(getFlightDto(), "indigo");

        // flight number is saved in upper case
        assertEquals("6E-201", dto.flightNumber());
        assertEquals("Chennai", dto.source());
        assertEquals("Delhi", dto.destination());
        assertTrue(dto.isActive());
        verify(flightRepository, times(1)).save(any(Flight.class));
    }

    @Test
    void addTest_RouteOfOtherAirline() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(routeService.getOwnedRoute(1L, "indigo"))
                .thenThrow(new OwnershipException("Operation not allowed! Route belongs to another airline"));
        FlightDto dto = getFlightDto();

        assertThrows(OwnershipException.class, () -> flightService.add(dto, "indigo"));

        verifyNoInteractions(flightRepository);
    }

    @Test
    void addTest_RouteNotActive() {
        route.setActive(false);
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(routeService.getOwnedRoute(1L, "indigo")).thenReturn(route);
        FlightDto dto = getFlightDto();

        assertThrows(InvalidCallException.class, () -> flightService.add(dto, "indigo"));

        verify(flightRepository, times(0)).save(any(Flight.class));
    }

    @Test
    void addTest_SameDepartureAndArrivalTime() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(routeService.getOwnedRoute(1L, "indigo")).thenReturn(route);
        FlightDto dto = new FlightDto("6E-201", 1L, LocalTime.of(13, 30), LocalTime.of(13, 30),
                1, 1, 3, 12000, 8000, 4000, 300, 200, "15kg");

        assertThrows(InvalidCallException.class, () -> flightService.add(dto, "indigo"));

        verify(flightRepository, times(0)).save(any(Flight.class));
    }

    @Test
    void addTest_FlightNumberAlreadyExists() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(routeService.getOwnedRoute(1L, "indigo")).thenReturn(route);
        when(flightRepository.existsByFlightNumberAndAirlineId("6E-201", 1L)).thenReturn(true);
        FlightDto dto = getFlightDto();

        assertThrows(InvalidCallException.class, () -> flightService.add(dto, "indigo"));

        verify(flightRepository, times(0)).save(any(Flight.class));
    }

    @Test
    void getFlightsTest() {
        when(flightRepository.getByAirlineUsername("indigo")).thenReturn(List.of(flight));

        List<FlightRespDto> list = flightService.getFlights("indigo");

        assertEquals(1, list.size());
        assertEquals("6E-201", list.get(0).flightNumber());
    }

    @Test
    void getFlightsTest_NoFlights() {
        when(flightRepository.getByAirlineUsername("indigo")).thenReturn(List.of());

        List<FlightRespDto> list = flightService.getFlights("indigo");

        assertTrue(list.isEmpty());
    }

    @Test
    void getOwnedFlightTest() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        Flight result = flightService.getOwnedFlight(1L, "indigo");

        assertEquals("6E-201", result.getFlightNumber());
    }

    @Test
    void getOwnedFlightTest_NotFound() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> flightService.getOwnedFlight(99L, "indigo"));
    }

    @Test
    void getOwnedFlightTest_OtherAirline() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThrows(OwnershipException.class, () -> flightService.getOwnedFlight(1L, "airindia"));
    }

    @Test
    void getDurationMinutesTest() {
        assertEquals(180, FlightService.getDurationMinutes(LocalTime.of(13, 30), LocalTime.of(16, 30)));
    }

    @Test
    void getDurationMinutesTest_OvernightFlight() {
        // 23:00 -> 01:30 next day
        assertEquals(150, FlightService.getDurationMinutes(LocalTime.of(23, 0), LocalTime.of(1, 30)));
    }

    @Test
    void searchTest() {
        LocalDate date = instance.getTravelDate();
        Page<FlightInstance> page = new PageImpl<>(List.of(instance), PageRequest.of(0, 10), 1);
        when(flightInstanceRepository.filterFlights(eq("Chennai"), eq("Delhi"), eq(date), anyList(),
                eq(AirlineStatus.APPROVED), isNull(), any(LocalTime.class), isNull(), eq(SeatClass.ECONOMY),
                eq(SeatStatus.AVAILABLE), eq(2L), isNull(), any(Pageable.class))).thenReturn(page);
        when(flightSeatRepository.countSeatsByInstances(anyList(), eq(SeatClass.ECONOMY), eq(SeatStatus.AVAILABLE)))
                .thenReturn(List.of(new SeatCountDto(100L, 20L)));
        when(flightInstanceRepository.getAirlineNames(eq("Chennai"), eq("Delhi"), eq(date), anyList(),
                eq(AirlineStatus.APPROVED))).thenReturn(List.of("IndiGo"));

        FlightFilterDto dto = flightService.search("Chennai", "Delhi", date, SeatClass.ECONOMY, 2,
                null, null, null, null, FlightSort.PRICE, 0, 10);

        assertEquals(1, dto.list().size());
        assertEquals("6E-201", dto.list().get(0).flightNumber());
        assertEquals("6E", dto.list().get(0).airlineCode());
        assertEquals(180, dto.list().get(0).durationMinutes());
        assertEquals(20, dto.list().get(0).availableSeats());
        // 4000 + 5% tax, adult without seat charge
        assertEquals(4200.0, dto.list().get(0).fare());
        // pagination info and airline filter options
        assertEquals(1, dto.paginationInfo().totalPages());
        assertEquals(1L, dto.paginationInfo().totalRecords());
        assertEquals(1, dto.paginationInfo().numRecordsInPage());
        assertEquals(List.of("IndiGo"), dto.airlines());
    }

    @Test
    void searchTest_FiltersSortAndPage() {
        LocalDate date = instance.getTravelDate();
        when(flightInstanceRepository.filterFlights(any(), any(), any(), anyList(), any(), any(), any(), any(),
                any(), any(), anyLong(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        flightService.search("Chennai", "Delhi", date, SeatClass.BUSINESS, 1,
                "IndiGo", LocalTime.of(6, 0), LocalTime.of(12, 0), 6300.0, FlightSort.PRICE, 1, 5);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Double> maxBaseFare = ArgumentCaptor.forClass(Double.class);
        verify(flightInstanceRepository).filterFlights(eq("Chennai"), eq("Delhi"), eq(date), anyList(),
                eq(AirlineStatus.APPROVED), eq("IndiGo"), eq(LocalTime.of(6, 0)), eq(LocalTime.of(12, 0)),
                eq(SeatClass.BUSINESS), eq(SeatStatus.AVAILABLE), eq(1L), maxBaseFare.capture(), pageable.capture());

        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        // price is sorted using the fare of the selected seat class
        assertNotNull(pageable.getValue().getSort().getOrderFor("flight.businessFare"));
        // 6300 shown to the passenger = 6000 base fare + 5% tax
        assertEquals(6000.0, maxBaseFare.getValue(), 0.05);
    }

    @Test
    void searchTest_SortByDeparture() {
        LocalDate date = instance.getTravelDate();
        when(flightInstanceRepository.filterFlights(any(), any(), any(), anyList(), any(), any(), any(), any(),
                any(), any(), anyLong(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        flightService.search("Chennai", "Delhi", date, SeatClass.ECONOMY, 1,
                null, null, null, null, FlightSort.DEPARTURE, 0, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(flightInstanceRepository).filterFlights(any(), any(), any(), anyList(), any(), any(), any(), any(),
                any(), any(), anyLong(), any(), pageable.capture());

        assertNotNull(pageable.getValue().getSort().getOrderFor("flight.departureTime"));
    }

    @Test
    void searchTest_PastDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        assertThrows(InvalidCallException.class,
                () -> flightService.search("Chennai", "Delhi", yesterday, SeatClass.ECONOMY, 1,
                        null, null, null, null, FlightSort.PRICE, 0, 10));

        verifyNoInteractions(flightInstanceRepository);
        verifyNoInteractions(flightSeatRepository);
    }

    @Test
    void searchTest_InvalidPage() {
        LocalDate date = instance.getTravelDate();

        assertThrows(InvalidCallException.class,
                () -> flightService.search("Chennai", "Delhi", date, SeatClass.ECONOMY, 1,
                        null, null, null, null, FlightSort.PRICE, -1, 10));
        assertThrows(InvalidCallException.class,
                () -> flightService.search("Chennai", "Delhi", date, SeatClass.ECONOMY, 1,
                        null, null, null, null, FlightSort.PRICE, 0, 0));

        verifyNoInteractions(flightInstanceRepository);
    }

    @Test
    void searchTest_NoFlights() {
        LocalDate date = instance.getTravelDate();
        when(flightInstanceRepository.filterFlights(any(), any(), any(), anyList(), any(), any(), any(), any(),
                any(), any(), anyLong(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        FlightFilterDto dto = flightService.search("Chennai", "Delhi", date, SeatClass.ECONOMY, 1,
                null, null, null, null, FlightSort.PRICE, 0, 10);

        // no flights is not an error, empty list is returned
        assertTrue(dto.list().isEmpty());
        assertEquals(0L, dto.paginationInfo().totalRecords());
        verifyNoInteractions(flightSeatRepository);
    }

    @Test
    void getAvailableRoutesTest() {
        when(flightInstanceRepository.getAvailableRoutes(any(LocalDate.class), anyList(), eq(AirlineStatus.APPROVED)))
                .thenReturn(List.of(new RouteAvailabilityDto("Chennai", "MAA", "Delhi", "DEL",
                        LocalDate.now(), LocalDate.now().plusDays(30), 31L)));

        List<RouteAvailabilityDto> list = flightService.getAvailableRoutes();

        assertEquals(1, list.size());
        assertEquals("MAA", list.get(0).sourceCode());
        assertEquals(31L, list.get(0).numberOfFlights());
    }

    @Test
    void getAvailableRoutesTest_NoRoutes() {
        when(flightInstanceRepository.getAvailableRoutes(any(LocalDate.class), anyList(), eq(AirlineStatus.APPROVED)))
                .thenReturn(List.of());

        List<RouteAvailabilityDto> list = flightService.getAvailableRoutes();

        assertTrue(list.isEmpty());
    }

    @Test
    void getSeatsTest() {
        when(flightInstanceRepository.existsById(100L)).thenReturn(true);
        when(flightSeatRepository.findByFlightInstanceIdOrderByIdAsc(100L))
                .thenReturn(List.of(getSeat(1L, "1A", SeatPosition.WINDOW), getSeat(2L, "1B", SeatPosition.MIDDLE)));

        List<SeatRespDto> list = flightService.getSeats(100L, null);

        assertEquals(2, list.size());
        assertEquals("1A", list.get(0).seatNumber());
        // window seat has extra charge, middle seat is free
        assertEquals(300.0, list.get(0).extraCharge());
        assertEquals(0.0, list.get(1).extraCharge());
    }

    @Test
    void getSeatsTest_BySeatClass() {
        when(flightInstanceRepository.existsById(100L)).thenReturn(true);
        when(flightSeatRepository.findByFlightInstanceIdAndSeatClassOrderByIdAsc(100L, SeatClass.ECONOMY))
                .thenReturn(List.of(getSeat(1L, "1A", SeatPosition.WINDOW)));

        List<SeatRespDto> list = flightService.getSeats(100L, SeatClass.ECONOMY);

        assertEquals(1, list.size());
        verify(flightSeatRepository, times(0)).findByFlightInstanceIdOrderByIdAsc(anyLong());
    }

    @Test
    void getSeatsTest_InstanceNotFound() {
        when(flightInstanceRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> flightService.getSeats(99L, null));

        verifyNoInteractions(flightSeatRepository);
    }

    @Test
    void updateTest() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(i -> i.getArgument(0));

        FlightRespDto dto = flightService.update(1L,
                new FlightUpdateDto(15000, 9000, 4500, 400, 250, "20kg"), "indigo");

        assertEquals(4500.0, dto.economyFare());
        assertEquals(15000.0, dto.businessFare());
        assertEquals(400.0, dto.windowCharge());
        assertEquals("20kg", dto.baggageInfo());
        assertEquals("6E-201", dto.flightNumber());
        verify(flightRepository, times(1)).save(flight);
    }

    @Test
    void updateTest_OtherAirline() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        FlightUpdateDto dto = new FlightUpdateDto(15000, 9000, 4500, 400, 250, "20kg");

        assertThrows(OwnershipException.class, () -> flightService.update(1L, dto, "airindia"));

        verify(flightRepository, times(0)).save(any(Flight.class));
    }

    @Test
    void updateTest_FlightNotFound() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());
        FlightUpdateDto dto = new FlightUpdateDto(15000, 9000, 4500, 400, 250, "20kg");

        assertThrows(ResourceNotFoundException.class, () -> flightService.update(99L, dto, "indigo"));
    }

    @Test
    void updateStatusTest_Deactivate() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(i -> i.getArgument(0));

        flightService.updateStatus(1L, false, "indigo");

        assertFalse(flight.isActive());
    }

    @Test
    void updateStatusTest_OtherAirline() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThrows(OwnershipException.class, () -> flightService.updateStatus(1L, false, "airindia"));

        assertTrue(flight.isActive());
        verify(flightRepository, times(0)).save(any(Flight.class));
    }

    @Test
    void getFlightStatusTest() {
        when(flightInstanceRepository.getByFlightNumberAndDate("6E-201", instance.getTravelDate()))
                .thenReturn(List.of(instance));

        FlightStatusRespDto dto = flightService.getFlightStatus("6E-201", instance.getTravelDate());

        assertEquals("IndiGo", dto.airlineName());
        assertEquals(FlightStatus.SCHEDULED, dto.status());
        assertEquals(0, dto.delayMinutes());
        assertEquals(LocalTime.of(13, 30), dto.expectedDeparture());
    }

    @Test
    void getFlightStatusTest_Delayed() {
        instance.setStatus(FlightStatus.DELAYED);
        instance.setDelayMinutes(45);
        when(flightInstanceRepository.getByFlightNumberAndDate("6E-201", instance.getTravelDate()))
                .thenReturn(List.of(instance));

        FlightStatusRespDto dto = flightService.getFlightStatus(" 6E-201 ", instance.getTravelDate());

        assertEquals(45, dto.delayMinutes());
        assertEquals(LocalTime.of(14, 15), dto.expectedDeparture());
        assertTrue(dto.message().contains("45"));
    }

    @Test
    void getFlightStatusTest_Cancelled() {
        instance.setStatus(FlightStatus.CANCELLED);
        when(flightInstanceRepository.getByFlightNumberAndDate("6E-201", instance.getTravelDate()))
                .thenReturn(List.of(instance));

        FlightStatusRespDto dto = flightService.getFlightStatus("6E-201", instance.getTravelDate());

        assertTrue(dto.message().contains("cancelled"));
    }

    @Test
    void getFlightStatusTest_NoFlight() {
        LocalDate date = instance.getTravelDate();
        when(flightInstanceRepository.getByFlightNumberAndDate("XX-999", date)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> flightService.getFlightStatus("XX-999", date));
    }
}
