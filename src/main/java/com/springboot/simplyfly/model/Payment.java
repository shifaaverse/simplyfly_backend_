package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.PaymentMethod;
import com.springboot.simplyfly.enums.PaymentStatus;
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
public class Payment { //pay

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking; //pay.booking b
    private double amount;
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String transactionId;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
