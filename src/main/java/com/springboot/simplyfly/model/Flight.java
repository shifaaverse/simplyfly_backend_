package com.springboot.simplyfly.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/* Flight service e.g. 6E-201 Chennai -> Delhi 13:30 - 16:30 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Flight { //f

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String flightNumber;
    @ManyToOne
    @JoinColumn(name = "route_id", nullable = false)
    private Route route; //f.route r
    @ManyToOne
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline; //f.airline a
    private LocalTime departureTime;
    private LocalTime arrivalTime;

    // Seat configuration (6 seats per row)
    private int businessRows;
    private int premiumRows;
    private int economyRows;

    // Base fare per seat class
    private double businessFare;
    private double premiumFare;
    private double economyFare;

    // Seat position pricing
    private double windowCharge;
    private double aisleCharge;

    private String baggageInfo;
    private boolean isActive;
}
