package com.springboot.simplyfly.model;

import jakarta.persistence.*;
import lombok.*;

/* Seat allocation of a traveller on a journey. Infant has no seat (flightSeat = null) */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "traveller_seat")
public class TravellerSeat { //ts

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "traveller_id", nullable = false)
    private Traveller traveller; //ts.traveller t
    @ManyToOne
    @JoinColumn(name = "booking_journey_id", nullable = false)
    private BookingJourney bookingJourney; //ts.bookingJourney bj
    @ManyToOne
    @JoinColumn(name = "flight_seat_id")
    private FlightSeat flightSeat; //ts.flightSeat fs
    private double fare;
    // Fare details (before tax) saved at booking time to explain the fare. Old bookings have null.
    private Double baseFare;
    private Double seatCharge;
    private Double mealCharge;
    private Double baggageCharge;
}
