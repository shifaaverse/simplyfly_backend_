package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.dto.response.AirlineBookingDto;
import com.springboot.simplyfly.dto.response.FlightPassengerDto;
import com.springboot.simplyfly.enums.BookingStatus;
import com.springboot.simplyfly.enums.JourneyStatus;
import com.springboot.simplyfly.model.TravellerSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravellerSeatRepository extends JpaRepository<TravellerSeat, Long> {

    // Get traveller seats for a booking journey.
    List<TravellerSeat> findByBookingJourneyIdOrderByIdAsc(Long journeyId);

    // Get all traveller seats belonging to a booking.
    @Query("""
            select ts from TravellerSeat ts
            join ts.bookingJourney bj
            join bj.booking b
            where b.id = ?1
            """)
    List<TravellerSeat> getByBookingId(Long bookingId);

    // Get passengers travelling on an airline's flights.
    @Query("""
            select new com.springboot.simplyfly.dto.response.AirlineBookingDto(
            b.id, b.pnr, t.name, t.category, fi.travelDate, f.flightNumber,
            fs.seatNumber, b.status, bj.status)
            from TravellerSeat ts
            join ts.traveller t
            join ts.bookingJourney bj
            join bj.booking b
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            left join ts.flightSeat fs
            where u.username = ?1
            AND b.status in ?2
            order by fi.travelDate desc, b.id desc
            """)
    List<AirlineBookingDto> getBookingsForAirline(String airlineUsername, List<BookingStatus> statuses);

    // Get the passenger list of one flight instance.
    @Query("""
            select new com.springboot.simplyfly.dto.response.FlightPassengerDto(
            b.pnr, t.name, t.category, t.gender, t.govtIdType, fs.seatNumber,
            t.mealOption, t.extraBaggageKg, bj.status)
            from TravellerSeat ts
            join ts.traveller t
            join ts.bookingJourney bj
            join bj.booking b
            join bj.flightInstance fi
            left join ts.flightSeat fs
            where fi.id = ?1
            AND bj.status in ?2
            order by b.pnr, t.id
            """)
    List<FlightPassengerDto> getPassengersByInstance(Long instanceId, List<JourneyStatus> statuses);

    // Count travellers for an airline by journey and booking status.
    @Query("""
            select count(ts.id) from TravellerSeat ts
            join ts.bookingJourney bj
            join bj.booking b
            join bj.flightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND bj.status = ?2
            AND b.status in ?3
            """)
    long countTravellersByAirline(String airlineUsername, JourneyStatus status, List<BookingStatus> bookingStatuses);
}