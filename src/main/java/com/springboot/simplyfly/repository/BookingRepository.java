package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find bookings made by a passenger using their username.
    @Query("""
            select b from Booking b
            join b.passenger p
            join p.user u
            where u.username=?1
            order by b.createdAt desc
            """)
    List<Booking> getByPassengerUsername(String passengerUsername);

    // Find a passenger's bookings, newest first.
    List<Booking> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);

    // Check whether a PNR already exists.
    boolean existsByPnr(String pnr);

    // Count bookings with the given statuses.
    long countByStatusIn(List<BookingStatus> statuses);

    // Find bookings with the given status created before the specified time.
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, Instant time);
}