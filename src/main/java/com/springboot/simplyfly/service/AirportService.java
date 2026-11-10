package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.AirportDto;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.mapper.AirportMapper;
import com.springboot.simplyfly.model.Airport;
import com.springboot.simplyfly.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirportService {

    private final AirportRepository airportRepository;

    // Add multiple airports at once.
    @Transactional(rollbackFor = Exception.class)
    public void addAirportsInBatch(List<AirportDto> list) {
        List<Airport> listEntity = list.stream()
                .map(AirportMapper::mapDtoToEntity)
                .toList();

        // Check whether any airport code already exists.
        listEntity.forEach(a -> {
            if (airportRepository.existsByCode(a.getCode()))
                throw new InvalidCallException("Airport " + a.getCode() + " already exists");
        });

        log.info("Processing multiple airports");
        airportRepository.saveAll(listEntity);
    }

    // Get all airports sorted by city.
    public List<Airport> getAll() {
        return airportRepository.findAll(Sort.by("city"));
    }
}