package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.model.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {

    // Check whether an airport code already exists.
    boolean existsByCode(String code);
}