package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.enums.JourneyType;
import com.springboot.simplyfly.enums.SeatClass;
import jakarta.persistence.*;
import lombok.*;

/* Individual journey of a booking. Round trip = 2 journeys (can be different airlines) */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "booking_journey")
public class BookingJourney { //bj

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking; //bj.booking b
    @ManyToOne
    @JoinColumn(name = "flight_instance_id", nullable = false)
    private FlightInstance flightInstance; //bj.flightInstance fi
    @Enumerated(EnumType.STRING)
    private JourneyType journeyType;
    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;
    private double amount;
    @Enumerated(EnumType.STRING)
    private JourneyStatus status;
}
