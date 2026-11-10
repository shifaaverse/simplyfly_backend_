package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.Gender;
import com.springboot.simplyfly.enums.GovtIdType;
import com.springboot.simplyfly.enums.MealOption;
import com.springboot.simplyfly.enums.PassengerCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/* Person travelling on a booking (passenger account can book for others) */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Traveller { //t

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking; //t.booking b
    @Column(nullable = false)
    private String name;
    private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Enumerated(EnumType.STRING)
    private PassengerCategory category;
    @Enumerated(EnumType.STRING)
    private GovtIdType govtIdType;
    @ToString.Exclude
    private String govtIdNumber;
    @Enumerated(EnumType.STRING)
    private MealOption mealOption;
    private Integer extraBaggageKg;
}
