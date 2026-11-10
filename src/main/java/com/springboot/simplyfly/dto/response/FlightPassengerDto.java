package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.Gender;
import com.springboot.simplyfly.enums.GovtIdType;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.enums.MealOption;
import com.springboot.simplyfly.enums.PassengerCategory;

/* Passenger list of one flight, visible to the airline that operates it */
public record FlightPassengerDto(
        String pnr,
        String travellerName,
        PassengerCategory category,
        Gender gender,
        GovtIdType govtIdType,
        String seatNumber,
        MealOption mealOption,
        Integer extraBaggageKg,
        JourneyStatus journeyStatus
) {
}
