package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    // Get the latest OTP of the user.
    PasswordReset findTopByUserUsernameOrderByIdDesc(String username);
}
