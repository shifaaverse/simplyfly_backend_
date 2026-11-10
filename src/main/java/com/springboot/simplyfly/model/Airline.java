package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.AirlineStatus;
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
public class Airline { //a

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String code; // e.g. 6E, AI
    private String registrationNumber;
    @Column(nullable = false)
    private String email;
    private String contact;
    private String address;
    private String logoUrl;
    @Enumerated(EnumType.STRING)
    private AirlineStatus status;
    private String rejectionReason; // why admin rejected / suspended
    @OneToOne
    private User user; //a.user u
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
