package com.springboot.simplyfly.dto.request;

import com.springboot.simplyfly.enums.Gender;
import com.springboot.simplyfly.enums.GovtIdType;
import com.springboot.simplyfly.enums.MealOption;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/* Passenger category (ADULT / CHILD / INFANT) is derived from date of birth.
   Government ID is compulsory from age 2. mealOption and extraBaggageKg are optional. */
public record TravellerDto(
        @NotBlank(message = "Traveller name is required")
        String name,
        @NotNull(message = "Traveller date of birth is required")
        @Past
        LocalDate dateOfBirth,
        @NotNull(message = "Gender is required")
        Gender gender,
        GovtIdType govtIdType,
        String govtIdNumber,
        MealOption mealOption,
        @Min(value = 0, message = "Extra baggage cannot be negative")
        @Max(value = 30, message = "Extra baggage can be maximum 30 kg")
        Integer extraBaggageKg
) {
}
