package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.model.FlightSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long> {

    // Provides basic database operations for flight schedules.
}