package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    // Get routes created by an airline.
    @Query("""
            select r from Route r
            join r.airline a
            join a.user u
            where u.username=?1
            order by r.id
            """)
    List<Route> getByAirlineUsername(String airlineUsername);

    // Count routes created by an airline.
    @Query("""
            select count(r.id) from Route r
            join r.airline a
            join a.user u
            where u.username=?1
            """)
    long countByAirlineUsername(String airlineUsername);

    // Check whether the airline already has this route.
    boolean existsBySourceIdAndDestinationIdAndAirlineId(Long sourceId, Long destinationId, Long airlineId);
}