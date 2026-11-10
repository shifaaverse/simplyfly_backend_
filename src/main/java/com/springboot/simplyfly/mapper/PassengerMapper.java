package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.request.PassengerDto;
import com.springboot.simplyfly.dto.response.PassengerRespDto;
import com.springboot.simplyfly.model.Passenger;

public class PassengerMapper {

    private PassengerMapper() {
    }

    public static Passenger convertDtoToEntity(PassengerDto dto) {
        Passenger passenger = new Passenger();
        passenger.setName(dto.name());
        passenger.setEmail(dto.email());
        passenger.setContact(dto.contact());
        passenger.setDateOfBirth(dto.dateOfBirth());
        return passenger;
    }

    public static PassengerRespDto convertEntityToDto(Passenger passenger) {
        return new PassengerRespDto(
                passenger.getId(),
                passenger.getName(),
                passenger.getEmail(),
                passenger.getContact(),
                passenger.getDateOfBirth(),
                passenger.getUser().getUsername(),
                passenger.getUser().isActive()
        );
    }
}
