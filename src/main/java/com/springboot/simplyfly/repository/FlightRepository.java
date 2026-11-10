package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    // Find all flights belonging to an airline.
    @Query("""
            select f from Flight f
            join f.airline a
            join a.user u
            where u.username=?1
            order by f.id
            """)
    List<Flight> getByAirlineUsername(String airlineUsername);

    // Count the total flights belonging to an airline.
    @Query("""
            select count(f.id) from Flight f
            join f.airline a
            join a.user u
            where u.username=?1
            """)
    long countByAirlineUsername(String airlineUsername);

    // Check whether a flight number already exists for an airline.
    boolean existsByFlightNumberAndAirlineId(String flightNumber, Long airlineId);
}