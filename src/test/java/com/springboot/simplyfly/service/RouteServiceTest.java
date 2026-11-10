package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.RouteDto;
import com.springboot.simplyfly.dto.response.RouteRespDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Airline;
import com.springboot.simplyfly.model.Airport;
import com.springboot.simplyfly.model.Route;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.AirportRepository;
import com.springboot.simplyfly.repository.RouteRepository;
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
class RouteServiceTest {

    @InjectMocks
    private RouteService routeService;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private AirlineService airlineService;

    private Airline airline;
    private Airport chennai;
    private Airport delhi;
    private Airport bangalore;
    private Route route;

    @BeforeEach
    void setUp() {
        User airlineUser = new User("indigo", "encoded", Role.AIRLINE);
        airline = new Airline();
        airline.setId(1L);
        airline.setName("IndiGo");
        airline.setUser(airlineUser);
        airline.setStatus(AirlineStatus.APPROVED);

        chennai = new Airport();
        chennai.setId(1L);
        chennai.setCode("MAA");
        chennai.setCity("Chennai");

        delhi = new Airport();
        delhi.setId(2L);
        delhi.setCode("DEL");
        delhi.setCity("Delhi");

        bangalore = new Airport();
        bangalore.setId(3L);
        bangalore.setCode("BLR");
        bangalore.setCity("Bangalore");

        route = new Route();
        route.setId(1L);
        route.setSource(chennai);
        route.setDestination(delhi);
        route.setAirline(airline);
        route.setActive(true);
    }

    @Test
    void addTest() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(airportRepository.findById(1L)).thenReturn(Optional.of(chennai));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(delhi));
        when(routeRepository.existsBySourceIdAndDestinationIdAndAirlineId(1L, 2L, 1L)).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenAnswer(i -> i.getArgument(0));

        RouteRespDto dto = routeService.add(new RouteDto(1L, 2L), "indigo");

        assertEquals("MAA", dto.sourceCode());
        assertEquals("DEL", dto.destinationCode());
        // new route is active by default
        assertTrue(dto.isActive());
        verify(routeRepository, times(1)).save(any(Route.class));
    }

    @Test
    void addTest_AirlineNotApproved() {
        when(airlineService.getApprovedAirline("indigo"))
                .thenThrow(new OwnershipException("Airline is not approved to operate"));
        RouteDto dto = new RouteDto(1L, 2L);

        assertThrows(OwnershipException.class, () -> routeService.add(dto, "indigo"));

        verifyNoInteractions(airportRepository);
        verifyNoInteractions(routeRepository);
    }

    @Test
    void addTest_SameSourceAndDestination() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        RouteDto dto = new RouteDto(1L, 1L);

        assertThrows(InvalidCallException.class, () -> routeService.add(dto, "indigo"));

        verifyNoInteractions(airportRepository);
        verify(routeRepository, times(0)).save(any(Route.class));
    }

    @Test
    void addTest_SourceAirportNotFound() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(airportRepository.findById(1L)).thenReturn(Optional.empty());
        RouteDto dto = new RouteDto(1L, 2L);

        assertThrows(ResourceNotFoundException.class, () -> routeService.add(dto, "indigo"));

        verify(routeRepository, times(0)).save(any(Route.class));
    }

    @Test
    void addTest_DestinationAirportNotFound() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(airportRepository.findById(1L)).thenReturn(Optional.of(chennai));
        when(airportRepository.findById(2L)).thenReturn(Optional.empty());
        RouteDto dto = new RouteDto(1L, 2L);

        assertThrows(ResourceNotFoundException.class, () -> routeService.add(dto, "indigo"));

        verify(routeRepository, times(0)).save(any(Route.class));
    }

    @Test
    void addTest_RouteAlreadyExists() {
        when(airlineService.getApprovedAirline("indigo")).thenReturn(airline);
        when(airportRepository.findById(1L)).thenReturn(Optional.of(chennai));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(delhi));
        when(routeRepository.existsBySourceIdAndDestinationIdAndAirlineId(1L, 2L, 1L)).thenReturn(true);
        RouteDto dto = new RouteDto(1L, 2L);

        assertThrows(InvalidCallException.class, () -> routeService.add(dto, "indigo"));

        verify(routeRepository, times(0)).save(any(Route.class));
    }

    @Test
    void getRoutesTest() {
        when(routeRepository.getByAirlineUsername("indigo")).thenReturn(List.of(route));

        List<RouteRespDto> list = routeService.getRoutes("indigo");

        assertEquals(1, list.size());
        assertEquals("Chennai", list.get(0).sourceCity());
        assertEquals("Delhi", list.get(0).destinationCity());
    }

    @Test
    void getRoutesTest_NoRoutes() {
        when(routeRepository.getByAirlineUsername("indigo")).thenReturn(List.of());

        List<RouteRespDto> list = routeService.getRoutes("indigo");

        assertTrue(list.isEmpty());
    }

    @Test
    void getOwnedRouteTest() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        Route result = routeService.getOwnedRoute(1L, "indigo");

        assertEquals(1L, result.getId());
    }

    @Test
    void getOwnedRouteTest_NotFound() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> routeService.getOwnedRoute(99L, "indigo"));
    }

    @Test
    void getOwnedRouteTest_OtherAirline() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        assertThrows(OwnershipException.class, () -> routeService.getOwnedRoute(1L, "airindia"));
    }

    @Test
    void updateTest() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(airportRepository.findById(3L)).thenReturn(Optional.of(bangalore));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(delhi));
        when(routeRepository.save(route)).thenReturn(route);

        // Chennai -> Delhi becomes Bangalore -> Delhi
        RouteRespDto dto = routeService.update(1L, new RouteDto(3L, 2L), "indigo");

        assertEquals("BLR", dto.sourceCode());
        assertEquals("DEL", dto.destinationCode());
        verify(routeRepository, times(1)).save(route);
    }

    @Test
    void updateTest_OtherAirline() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        RouteDto dto = new RouteDto(3L, 2L);

        assertThrows(OwnershipException.class, () -> routeService.update(1L, dto, "airindia"));

        verifyNoInteractions(airportRepository);
        verify(routeRepository, times(0)).save(any(Route.class));
    }

    @Test
    void updateTest_SameSourceAndDestination() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        RouteDto dto = new RouteDto(2L, 2L);

        assertThrows(InvalidCallException.class, () -> routeService.update(1L, dto, "indigo"));

        verify(routeRepository, times(0)).save(any(Route.class));
    }

    @Test
    void updateStatusTest() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeRepository.save(route)).thenReturn(route);

        RouteRespDto dto = routeService.updateStatus(1L, false, "indigo");

        assertFalse(dto.isActive());
        verify(routeRepository, times(1)).save(route);
    }

    @Test
    void updateStatusTest_OtherAirline() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        assertThrows(OwnershipException.class, () -> routeService.updateStatus(1L, false, "airindia"));

        // route is untouched
        assertTrue(route.isActive());
        verify(routeRepository, times(0)).save(any(Route.class));
    }
}