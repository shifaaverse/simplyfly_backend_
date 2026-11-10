package com.springboot.simplyfly.model;

import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.enums.SeatPosition;
import com.springboot.simplyfly.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

/* Seat availability per flight instance */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "flight_seat")
public class FlightSeat { //fs

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "flight_instance_id", nullable = false)
    private FlightInstance flightInstance; //fs.flightInstance fi
    private String seatNumber; // 1A, 12F
    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;
    @Enumerated(EnumType.STRING)
    private SeatPosition seatPosition;
    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    // Optimistic locking: two passengers cannot hold the same seat at the same time
    @Version
    private Long version;
}
