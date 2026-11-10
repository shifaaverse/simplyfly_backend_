package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.AirportDto;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.model.Airport;
import com.springboot.simplyfly.repository.AirportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirportServiceTest {

    @InjectMocks
    private AirportService airportService;

    @Mock
    private AirportRepository airportRepository;

    private final List<AirportDto> dtoList = List.of(
            new AirportDto("MAA", "Chennai International Airport", "Chennai"),
            new AirportDto("del", "Indira Gandhi International Airport", "Delhi"));

    @Test
    void addAirportsInBatchTest() {
        when(airportRepository.existsByCode("MAA")).thenReturn(false);
        // mapper converts the code to upper case
        when(airportRepository.existsByCode("DEL")).thenReturn(false);

        airportService.addAirportsInBatch(dtoList);

        verify(airportRepository, times(1)).saveAll(argThat(list -> ((List<?>) list).size() == 2));
    }

    @Test
    void addAirportsInBatchTest_DuplicateCode() {
        when(airportRepository.existsByCode("MAA")).thenReturn(true);

        assertThrows(InvalidCallException.class, () -> airportService.addAirportsInBatch(dtoList));

        verify(airportRepository, times(0)).saveAll(anyList());
    }

    @Test
    void getAllTest() {
        Airport chennai = new Airport();
        chennai.setCode("MAA");
        chennai.setCity("Chennai");
        Airport delhi = new Airport();
        delhi.setCode("DEL");
        delhi.setCity("Delhi");
        when(airportRepository.findAll(any(Sort.class))).thenReturn(List.of(chennai, delhi));

        List<Airport> list = airportService.getAll();

        assertEquals(2, list.size());
        assertEquals("MAA", list.get(0).getCode());
        verify(airportRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    void getAllTest_NoAirports() {
        when(airportRepository.findAll(any(Sort.class))).thenReturn(List.of());

        List<Airport> list = airportService.getAll();

        assertTrue(list.isEmpty());
    }
}