package com.springboot.simplyfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/* Registered passenger account (the person who books). */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Passenger { //p

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String email;
    private String contact;
    private LocalDate dateOfBirth;
    @OneToOne
    private User user; //p.user u
    @UpdateTimestamp
    private Instant updatedAt;
}
