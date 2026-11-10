package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.dto.response.AirlineBookingCountDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.model.Airline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Long> {

    // Find an airline using its username.
    @Query("""
            select a from Airline a
            join a.user u
            where u.username=?1
            """)
    Airline getByUsername(String airlineUsername);

    // Find airlines by their status.
    List<Airline> findByStatus(AirlineStatus status); // select a from Airline a where a.status=?1

    // Count airlines with a given status.
    long countByStatus(AirlineStatus status);

    // Check whether an airline code already exists.
    boolean existsByCode(String code);

    /*
       Airline Name  |  No of Bookings
       IndiGo             34
       Air India          12
    * */

    // Get the booking count for each airline.
    @Query("""
            select new com.springboot.simplyfly.dto.response.AirlineBookingCountDto(a.name, count(bj.id))
            from BookingJourney bj
            join bj.booking b
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            where b.status in ?1
            group by a.name
            """)
    List<AirlineBookingCountDto> getAirlineWiseBookingCount(List<BookingStatus> statuses);
}