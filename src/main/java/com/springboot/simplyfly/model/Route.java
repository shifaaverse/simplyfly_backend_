package com.springboot.simplyfly.model;

import jakarta.persistence.*;
import lombok.*;

/* A route is only source + destination. It does not represent a flight or time. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Route { //r

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private Airport source; //r.source s
    @ManyToOne
    @JoinColumn(name = "destination_id", nullable = false)
    private Airport destination; //r.destination d
    @ManyToOne
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline; //r.airline a
    private boolean isActive;
}
