package com.springboot.simplyfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/* Message shown in the bell icon of a user (passenger, airline or admin) */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Notification { //n

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; //n.user u
    @Column(nullable = false)
    private String title;
    @Column(length = 500)
    private String message;
    private boolean isRead;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
