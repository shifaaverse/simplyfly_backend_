package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.model.Passenger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    // Find a passenger using their username.
    @Query("""
            select p from Passenger p
            join p.user u
            where u.username=?1
            """)
    Passenger getByUsername(String passengerUsername);

    // Search passengers by name, email or username with pagination.
    @Query("""
            select p from Passenger p
            join p.user u
            where (?1 IS NULL
                   OR lower(p.name) like lower(concat('%', ?1, '%'))
                   OR lower(p.email) like lower(concat('%', ?1, '%'))
                   OR lower(u.username) like lower(concat('%', ?1, '%')))
            order by p.id desc
            """)
    List<Passenger> searchPassengers(String search, Pageable pageable);
}