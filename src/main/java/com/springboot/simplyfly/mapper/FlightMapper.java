package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.request.FlightDto;
import com.springboot.simplyfly.dto.response.FlightRespDto;
import com.springboot.simplyfly.dto.response.SeatRespDto;
import com.springboot.simplyfly.model.Flight;
import com.springboot.simplyfly.model.FlightSeat;
import com.springboot.simplyfly.utility.FareUtility;

public class FlightMapper {

    private FlightMapper() {
    }

    public static Flight convertDtoToEntity(FlightDto dto) {
        Flight flight = new Flight();
        flight.setFlightNumber(dto.flightNumber().toUpperCase());
        flight.setDepartureTime(dto.departureTime());
        flight.setArrivalTime(dto.arrivalTime());
        flight.setBusinessRows(dto.businessRows());
        flight.setPremiumRows(dto.premiumRows());
        flight.setEconomyRows(dto.economyRows());
        flight.setBusinessFare(dto.businessFare());
        flight.setPremiumFare(dto.premiumFare());
        flight.setEconomyFare(dto.economyFare());
        flight.setWindowCharge(dto.windowCharge());
        flight.setAisleCharge(dto.aisleCharge());
        flight.setBaggageInfo(dto.baggageInfo());
        return flight;
    }

    public static FlightRespDto convertEntityToDto(Flight flight) {
        return new FlightRespDto(
                flight.getId(),
                flight.getFlightNumber(),
                flight.getRoute().getId(),
                flight.getRoute().getSource().getCity(),
                flight.getRoute().getDestination().getCity(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getBusinessRows(),
                flight.getPremiumRows(),
                flight.getEconomyRows(),
                flight.getBusinessFare(),
                flight.getPremiumFare(),
                flight.getEconomyFare(),
                flight.getWindowCharge(),
                flight.getAisleCharge(),
                flight.getBaggageInfo(),
                flight.isActive()
        );
    }

    public static SeatRespDto convertSeatToDto(FlightSeat seat) {
        Flight flight = seat.getFlightInstance().getFlight();
        return new SeatRespDto(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getSeatClass(),
                seat.getSeatPosition(),
                seat.getStatus(),
                FareUtility.getSeatCharge(flight, seat.getSeatPosition())
        );
    }
}
