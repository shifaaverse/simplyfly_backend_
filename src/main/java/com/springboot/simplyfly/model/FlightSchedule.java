package com.springboot.simplyfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/* Recurring schedule. operatingDays = DAILY or comma separated days e.g. MONDAY,FRIDAY */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "flight_schedule")
public class FlightSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;
    private LocalDate startDate;
    private LocalDate endDate;
    private String operatingDays;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
