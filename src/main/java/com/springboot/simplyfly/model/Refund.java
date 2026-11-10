package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/* Refund is per journey because a round trip can have 2 different airlines */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Refund { //rf

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "booking_journey_id", nullable = false)
    private BookingJourney bookingJourney; //rf.bookingJourney bj
    private String reason;
    private double refundAmount;
    @Enumerated(EnumType.STRING)
    private RefundStatus status;
    private String rejectionReason;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
