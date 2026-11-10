package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.request.AirportDto;
import com.springboot.simplyfly.model.Airport;

public class AirportMapper {

    private AirportMapper() {
    }

    public static Airport mapDtoToEntity(AirportDto dto) {
        Airport airport = new Airport();
        airport.setCode(dto.code().toUpperCase());
        airport.setName(dto.name());
        airport.setCity(dto.city());
        return airport;
    }
}
