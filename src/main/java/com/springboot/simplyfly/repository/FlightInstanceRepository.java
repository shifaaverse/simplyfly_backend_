package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.dto.response.FlightInstanceRespDto;
import com.springboot.simplyfly.dto.response.RouteAvailabilityDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.enums.FlightStatus;
import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.enums.SeatStatus;
import com.springboot.simplyfly.model.FlightInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface FlightInstanceRepository extends JpaRepository<FlightInstance, Long> {

    // Check whether a flight instance already exists for the given date.
    boolean existsByFlightIdAndTravelDate(Long flightId, LocalDate travelDate);

    // Dated flights up to the given date that are still in one of the given statuses.
    List<FlightInstance> findByTravelDateLessThanEqualAndStatusIn(LocalDate travelDate, List<FlightStatus> statuses);

    /* source / destination can be city name or airport code */

    /*
     Dynamic filter (same idea as dynamicFilterTicket in helpdesk):
     airlineName, toTime and maxBaseFare are optional - null means "do not filter".
     ?7 = earliest departure time, ?8 = latest departure time (before),
     ?9 = seat class, ?10 = seat status, ?11 = travellers needed,
     ?12 = max base fare of the selected seat class.
     Sorting and page come from Pageable.
    * */

    // Search flights with optional airline, departure time and price filters, one page at a time.
    @Query("""
            select fi from FlightInstance fi
            join fi.flight f
            join f.route r
            join r.source s
            join r.destination d
            join f.airline a
            where (lower(s.city) = lower(?1) OR lower(s.code) = lower(?1))
            AND (lower(d.city) = lower(?2) OR lower(d.code) = lower(?2))
            AND fi.travelDate = ?3
            AND fi.status in ?4
            AND a.status = ?5
            AND r.isActive = true
            AND f.isActive = true
            AND (?6 IS NULL OR a.name = ?6)
            AND f.departureTime >= ?7
            AND (?8 IS NULL OR f.departureTime < ?8)
            AND (select count(fs.id) from FlightSeat fs
                 where fs.flightInstance = fi
                 AND fs.seatClass = ?9
                 AND fs.status = ?10) >= ?11
            AND (?12 IS NULL OR (case
                 when ?9 = com.springboot.simplyfly.enums.SeatClass.BUSINESS then f.businessFare
                 when ?9 = com.springboot.simplyfly.enums.SeatClass.PREMIUM_ECONOMY then f.premiumFare
                 else f.economyFare end) <= ?12)
            """)
    Page<FlightInstance> filterFlights(String source,
                                       String destination,
                                       LocalDate travelDate,
                                       List<FlightStatus> statuses,
                                       AirlineStatus airlineStatus,
                                       String airlineName,
                                       LocalTime fromTime,
                                       LocalTime toTime,
                                       SeatClass seatClass,
                                       SeatStatus seatStatus,
                                       long travellers,
                                       Double maxBaseFare,
                                       Pageable pageable);

    // Get names of the airlines flying the route on the given date (used for the airline filter).
    @Query("""
            select distinct a.name from FlightInstance fi
            join fi.flight f
            join f.route r
            join r.source s
            join r.destination d
            join f.airline a
            where (lower(s.city) = lower(?1) OR lower(s.code) = lower(?1))
            AND (lower(d.city) = lower(?2) OR lower(d.code) = lower(?2))
            AND fi.travelDate = ?3
            AND fi.status in ?4
            AND a.status = ?5
            AND r.isActive = true
            AND f.isActive = true
            order by a.name
            """)
    List<String> getAirlineNames(String source,
                                 String destination,
                                 LocalDate travelDate,
                                 List<FlightStatus> statuses,
                                 AirlineStatus airlineStatus);

    /*
     Source  | Destination | First date | Last date | No of dated flights
     Chennai   Delhi         2026-11-04   2026-12-03      60
    * */

    // Find the dated flights having the given flight number on a date (used by flight status).
    @Query("""
            select fi from FlightInstance fi
            join fi.flight f
            where upper(f.flightNumber) = upper(?1)
            AND fi.travelDate = ?2
            order by f.id
            """)
    List<FlightInstance> getByFlightNumberAndDate(String flightNumber, LocalDate travelDate);

    // Get routes that have available flights from the given date.
    @Query("""
            select new com.springboot.simplyfly.dto.response.RouteAvailabilityDto(
            s.city, s.code, d.city, d.code, min(fi.travelDate), max(fi.travelDate), count(fi.id))
            from FlightInstance fi
            join fi.flight f
            join f.route r
            join r.source s
            join r.destination d
            join f.airline a
            where fi.travelDate >= ?1
            AND fi.status in ?2
            AND a.status = ?3
            AND r.isActive = true
            AND f.isActive = true
            group by s.city, s.code, d.city, d.code
            order by s.city, d.city
            """)
    List<RouteAvailabilityDto> getAvailableRoutes(LocalDate fromDate,
                                                  List<FlightStatus> statuses,
                                                  AirlineStatus airlineStatus);

    // Get flight instances belonging to a particular airline.
    @Query("""
            select new com.springboot.simplyfly.dto.response.FlightInstanceRespDto(
            fi.id, a.name, f.flightNumber, s.city, d.city,
            fi.travelDate, f.departureTime, f.arrivalTime, fi.status)
            from FlightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            join f.route r
            join r.source s
            join r.destination d
            where u.username = ?1
            AND fi.travelDate >= ?2
            order by fi.travelDate, f.departureTime
            """)
    List<FlightInstanceRespDto> getByAirlineUsername(String airlineUsername, LocalDate fromDate, Pageable pageable);

    // Get all flight instances from the given date.
    @Query("""
            select new com.springboot.simplyfly.dto.response.FlightInstanceRespDto(
            fi.id, a.name, f.flightNumber, s.city, d.city,
            fi.travelDate, f.departureTime, f.arrivalTime, fi.status)
            from FlightInstance fi
            join fi.flight f
            join f.airline a
            join f.route r
            join r.source s
            join r.destination d
            where fi.travelDate >= ?1
            order by fi.travelDate, f.departureTime
            """)
    List<FlightInstanceRespDto> getAllFromDate(LocalDate fromDate, Pageable pageable);

    // Count an airline's flight instances on a particular date.
    @Query("""
            select count(fi.id) from FlightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND fi.travelDate = ?2
            """)
    long countByAirlineAndDate(String airlineUsername, LocalDate date);

    // Count an airline's upcoming flight instances.
    @Query("""
            select count(fi.id) from FlightInstance fi
            join fi.flight f
            join f.airline a
            join a.user u
            where u.username = ?1
            AND fi.travelDate > ?2
            """)
    long countUpcomingByAirline(String airlineUsername, LocalDate date);
}