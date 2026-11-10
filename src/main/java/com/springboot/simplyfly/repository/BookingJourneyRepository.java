package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.dto.response.AdminBookingDto;
import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.model.BookingJourney;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingJourneyRepository extends JpaRepository<BookingJourney, Long> {

    // Find all journeys of a booking in ascending ID order.
    List<BookingJourney> findByBookingIdOrderByIdAsc(Long bookingId);

    // Find the journeys of a flight instance having any of the given statuses.
    List<BookingJourney> findByFlightInstanceIdAndStatusIn(Long instanceId, List<JourneyStatus> statuses);

    // Get all bookings for Admin with optional status filtering and pagination.
    @Query("""
            select new com.springboot.simplyfly.dto.response.AdminBookingDto(
            b.id, b.pnr, p.name, a.name, f.flightNumber, s.city, d.city,
            fi.travelDate, bj.journeyType, b.status, bj.status, bj.amount)
            from BookingJourney bj
            join bj.booking b
            join b.passenger p
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            join f.route r
            join r.source s
            join r.destination d
            where (?1 IS NULL OR b.status = ?1)
            order by b.createdAt desc
            """)
    List<AdminBookingDto> getAllForAdmin(BookingStatus status, Pageable pageable);

    // Count bookings belonging to an airline based on booking status.
    @Query("""
            select count(bj.id) from BookingJourney bj
            join bj.booking b
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND b.status in ?2
            """)
    long countByAirlineAndBookingStatus(String airlineUsername, List<BookingStatus> statuses);

    // Count journeys belonging to an airline based on journey status.
    @Query("""
            select count(bj.id) from BookingJourney bj
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND bj.status = ?2
            """)
    long countByAirlineAndJourneyStatus(String airlineUsername, JourneyStatus status);

    // Calculate the total journey amount for an airline.
    @Query("""
            select coalesce(sum(bj.amount), 0) from BookingJourney bj
            join bj.booking b
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND bj.status = ?2
            AND b.status in ?3
            """)
    double sumAmountByAirline(String airlineUsername, JourneyStatus status, List<BookingStatus> statuses);
}