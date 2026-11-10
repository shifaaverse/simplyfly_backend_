package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.ComplaintCategory;
import com.springboot.simplyfly.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Complaint { //cm

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger; //cm.passenger p
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking; //cm.booking b
    @ManyToOne
    @JoinColumn(name = "airline_id")
    private Airline airline; //cm.airline a  (set when admin forwards)
    @Enumerated(EnumType.STRING)
    private ComplaintCategory category;
    private String subject;
    @Column(length = 1000)
    private String description;
    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;
    @Column(length = 1000)
    private String airlineResponse;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
