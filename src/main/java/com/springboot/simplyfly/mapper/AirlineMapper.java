package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.request.AirlineDto;
import com.springboot.simplyfly.dto.response.AirlineRespDto;
import com.springboot.simplyfly.model.Airline;

public class AirlineMapper {

    private AirlineMapper() {
    }

    public static Airline convertDtoToEntity(AirlineDto dto) {
        Airline airline = new Airline();
        airline.setName(dto.name());
        airline.setCode(dto.code().toUpperCase());
        airline.setRegistrationNumber(dto.registrationNumber());
        airline.setEmail(dto.email());
        airline.setContact(dto.contact());
        airline.setAddress(dto.address());
        airline.setLogoUrl(dto.logoUrl());
        return airline;
    }

    public static AirlineRespDto convertEntityToDto(Airline airline) {
        return new AirlineRespDto(
                airline.getId(),
                airline.getName(),
                airline.getCode(),
                airline.getRegistrationNumber(),
                airline.getEmail(),
                airline.getContact(),
                airline.getAddress(),
                airline.getLogoUrl(),
                airline.getStatus(),
                airline.getRejectionReason(),
                airline.getUser().getUsername(),
                airline.getCreatedAt()
        );
    }
}
