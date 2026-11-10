package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.enums.PaymentStatus;
import com.springboot.simplyfly.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payments made by a passenger.
    @Query("""
            select pay from Payment pay
            join pay.booking b
            join b.passenger p
            join p.user u
            where u.username=?1
            order by pay.createdAt desc
            """)
    List<Payment> getByPassengerUsername(String passengerUsername);

    // Get payments filtered by status.
    @Query("""
            select pay from Payment pay
            where (?1 IS NULL OR pay.status = ?1)
            order by pay.createdAt desc
            """)
    List<Payment> filterByStatus(PaymentStatus status);

    // Count payments with a given status.
    long countByStatus(PaymentStatus status);

    // Calculate the total amount of payments with a given status.
    @Query("""
            select coalesce(sum(pay.amount), 0) from Payment pay
            where pay.status = ?1
            """)
    double sumAmountByStatus(PaymentStatus status);
}