package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.TripType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/* One booking = one or two journeys + many travellers */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Booking { //b

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String pnr;
    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger; //b.passenger p
    @Enumerated(EnumType.STRING)
    private TripType tripType;
    @Enumerated(EnumType.STRING)
    private BookingStatus status;
    private double totalAmount;
    private String razorpayOrderId; // Razorpay order created for this booking
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
