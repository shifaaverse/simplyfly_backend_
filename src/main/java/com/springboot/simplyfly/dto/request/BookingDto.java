package com.springboot.simplyfly.dto.request;

import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.enums.TripType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/*
 * departureSeatIds / returnSeatIds are given in the same order as travellers
 * (infants are skipped because they do not get a seat)
 * */
public record BookingDto(
        @NotNull(message = "Trip type is required")
        TripType tripType,
        @NotNull(message = "Seat class is required")
        SeatClass seatClass,
        @NotNull(message = "Departure flight is required")
        Long departureInstanceId,
        Long returnInstanceId,
        @NotEmpty(message = "At least one traveller is required")
        @Valid
        List<TravellerDto> travellers,
        @NotEmpty(message = "Select seats for departure journey")
        List<Long> departureSeatIds,
        List<Long> returnSeatIds
) {
}
