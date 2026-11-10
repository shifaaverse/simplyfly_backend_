package com.springboot.simplyfly.utility;

import com.springboot.simplyfly.enums.MealOption;
import com.springboot.simplyfly.enums.PassengerCategory;
import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.enums.SeatPosition;
import com.springboot.simplyfly.model.Flight;

import java.time.LocalDate;
import java.time.Period;

public class FareUtility {

    public static final double TAX_PERCENT = 5.0;
    public static final double CHILD_FACTOR = 0.75;
    public static final double INFANT_FACTOR = 0.10;

    // Add-on prices (before tax)
    public static final double VEG_MEAL_PRICE = 300;
    public static final double NON_VEG_MEAL_PRICE = 350;
    public static final double EXTRA_BAGGAGE_PRICE_PER_KG = 400;

    private FareUtility() {
    }

    // Get the passenger category based on age on the travel date.
    public static PassengerCategory getCategory(LocalDate dateOfBirth, LocalDate travelDate) {
        int age = Period.between(dateOfBirth, travelDate).getYears();

        if (age < 2)
            return PassengerCategory.INFANT;

        if (age < 12)
            return PassengerCategory.CHILD;

        return PassengerCategory.ADULT;
    }

    // Get the base fare for the selected seat class.
    public static double getClassFare(Flight flight, SeatClass seatClass) {
        return switch (seatClass) {
            case BUSINESS -> flight.getBusinessFare();
            case PREMIUM_ECONOMY -> flight.getPremiumFare();
            case ECONOMY -> flight.getEconomyFare();
        };
    }

    // Get the additional charge based on seat position.
    public static double getSeatCharge(Flight flight, SeatPosition seatPosition) {
        if (seatPosition == null)
            return 0;

        return switch (seatPosition) {
            case WINDOW -> flight.getWindowCharge();
            case AISLE -> flight.getAisleCharge();
            case MIDDLE -> 0;
        };
    }

    // Get the base fare of the seat class after the child / infant discount.
    public static double getBaseFare(Flight flight, SeatClass seatClass, PassengerCategory category) {
        double base = getClassFare(flight, seatClass);

        double factor = switch (category) {
            case ADULT -> 1.0;
            case CHILD -> CHILD_FACTOR;
            case INFANT -> INFANT_FACTOR;
        };

        return base * factor;
    }

    // Calculate the total fare including seat charge and tax.
    public static double calculateFare(Flight flight, SeatClass seatClass,
                                       PassengerCategory category, SeatPosition seatPosition) {
        double amount = getBaseFare(flight, seatClass, category) + getSeatCharge(flight, seatPosition);
        double total = amount + (amount * TAX_PERCENT / 100);

        return Math.round(total * 100.0) / 100.0;
    }

    // Get the price of a meal option.
    public static double getMealPrice(MealOption mealOption) {
        if (mealOption == null)
            return 0;

        return switch (mealOption) {
            case VEG -> VEG_MEAL_PRICE;
            case NON_VEG -> NON_VEG_MEAL_PRICE;
            case NONE -> 0;
        };
    }

    // Get the price of the extra baggage.
    public static double getBaggageCharge(Integer extraBaggageKg) {
        return extraBaggageKg == null ? 0 : extraBaggageKg * EXTRA_BAGGAGE_PRICE_PER_KG;
    }

    // Calculate the charge of meal and extra baggage including tax.
    public static double calculateAddOns(MealOption mealOption, Integer extraBaggageKg) {
        double amount = getMealPrice(mealOption) + getBaggageCharge(extraBaggageKg);
        double total = amount + (amount * TAX_PERCENT / 100);

        return Math.round(total * 100.0) / 100.0;
    }

    // Calculate the refund based on the hours before departure.
    public static double calculateRefund(double amount, long hoursBeforeDeparture) {
        double percent;

        if (hoursBeforeDeparture >= 48)
            percent = 80;
        else if (hoursBeforeDeparture >= 24)
            percent = 50;
        else
            percent = 0;

        return Math.round(amount * percent) / 100.0;
    }
}