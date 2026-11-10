package com.springboot.simplyfly.model;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code; // MAA, DEL
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String city;
}
