package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.enums.RefundStatus;
import com.springboot.simplyfly.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    // Find refunds requested by a passenger.
    @Query("""
            select rf from Refund rf
            join rf.bookingJourney bj
            join bj.booking b
            join b.passenger p
            join p.user u
            where u.username=?1
            order by rf.createdAt desc
            """)
    List<Refund> getByPassengerUsername(String passengerUsername);

    // Get refunds filtered by status.
    @Query("""
            select rf from Refund rf
            where (?1 IS NULL OR rf.status = ?1)
            order by rf.createdAt desc
            """)
    List<Refund> filterByStatus(RefundStatus status);

    // Find refunds belonging to an airline with the given statuses.
    @Query("""
            select rf from Refund rf
            join rf.bookingJourney bj
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND rf.status in ?2
            order by rf.createdAt desc
            """)
    List<Refund> getByAirlineUsername(String airlineUsername, List<RefundStatus> statuses);

    // Count refunds for an airline with a given status.
    @Query("""
            select count(rf.id) from Refund rf
            join rf.bookingJourney bj
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND rf.status = ?2
            """)
    long countByAirlineAndStatus(String airlineUsername, RefundStatus status);

    // Count refunds with any of the given statuses.
    long countByStatusIn(List<RefundStatus> statuses);

    // Check whether a journey has a refund with any of the given statuses.
    boolean existsByBookingJourneyIdAndStatusIn(Long journeyId, List<RefundStatus> statuses);

    // Find the refunds of a journey having any of the given statuses.
    List<Refund> findByBookingJourneyIdAndStatusIn(Long journeyId, List<RefundStatus> statuses);

    // Calculate the total refund amount for a given status.
    @Query("""
            select coalesce(sum(rf.refundAmount), 0) from Refund rf
            where rf.status = ?1
            """)
    double sumRefundAmountByStatus(RefundStatus status);
}