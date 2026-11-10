package com.springboot.simplyfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/* One time code (OTP) to reset a forgotten password */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "password_reset")
public class PasswordReset { //pr

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; //pr.user u
    private String otp;
    private Instant expiresAt;
    private boolean isUsed;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
