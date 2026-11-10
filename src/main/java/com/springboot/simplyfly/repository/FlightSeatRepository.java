package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.dto.response.SeatCountDto;
import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.enums.SeatStatus;
import com.springboot.simplyfly.model.FlightSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FlightSeatRepository extends JpaRepository<FlightSeat, Long> {

    // Get all seats of a flight instance in ID order.
    List<FlightSeat> findByFlightInstanceIdOrderByIdAsc(Long instanceId);

    // Get seats of a particular class for a flight instance.
    List<FlightSeat> findByFlightInstanceIdAndSeatClassOrderByIdAsc(Long instanceId, SeatClass seatClass);

    /*
     Avoid N+1 : one query gives available seat count for all searched flights
     instanceId  |  availableSeats
        12              54
        15              60
    * */

    // Get the available seat count for multiple flight instances in one query.
    @Query("""
            select new com.springboot.simplyfly.dto.response.SeatCountDto(fi.id, count(fs.id))
            from FlightSeat fs
            join fs.flightInstance fi
            where fi.id in ?1
            AND fs.seatClass = ?2
            AND fs.status = ?3
            group by fi.id
            """)
    List<SeatCountDto> countSeatsByInstances(List<Long> instanceIds, SeatClass seatClass, SeatStatus status);

    // Count seats belonging to an airline from the given date.
    @Query("""
            select count(fs.id) from FlightSeat fs
            join fs.flightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND fs.status = ?2
            AND fi.travelDate >= ?3
            """)
    long countSeatsByAirline(String airlineUsername, SeatStatus status, LocalDate fromDate);
}