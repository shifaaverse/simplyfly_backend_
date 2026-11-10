package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.MealOption;
import com.springboot.simplyfly.enums.PassengerCategory;
import com.springboot.simplyfly.enums.SeatPosition;

public record TravellerSeatRespDto(
        String travellerName,
        PassengerCategory category,
        String seatNumber,
        SeatPosition seatPosition,
        MealOption mealOption,
        int extraBaggageKg,
        double baseFare,
        double seatCharge,
        double mealCharge,
        double baggageCharge,
        double tax,
        double fare
) {
}
