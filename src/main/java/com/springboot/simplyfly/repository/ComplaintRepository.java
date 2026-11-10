package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // Find complaints submitted by a passenger.
    @Query("""
            select cm from Complaint cm
            join cm.passenger p
            join p.user u
            where u.username=?1
            order by cm.createdAt desc
            """)
    List<Complaint> getByPassengerUsername(String passengerUsername);

    // Find complaints assigned to an airline.
    @Query("""
            select cm from Complaint cm
            join cm.airline a
            join a.user u
            where u.username=?1
            order by cm.createdAt desc
            """)
    List<Complaint> getByAirlineUsername(String airlineUsername);

    // Find all complaints, newest first.
    List<Complaint> findAllByOrderByCreatedAtDesc();
}