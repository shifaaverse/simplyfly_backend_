package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.FlightStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/* Dated flight. Every date has its own seats, bookings and status. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "flight_instance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"flight_id", "travel_date"}))
public class FlightInstance { //fi

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight; //fi.flight f
    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private FlightSchedule schedule;
    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;
    @Enumerated(EnumType.STRING)
    private FlightStatus status;
    private Integer delayMinutes; // only when status is DELAYED
}
