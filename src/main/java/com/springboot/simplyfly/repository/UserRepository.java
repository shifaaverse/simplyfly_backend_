package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.dto.response.DetailsDto;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
            select u
            from User u
            where u.username=?1
            """)
    User loadUserByUsername(String username);

    @Query("""
            select new com.springboot.simplyfly.dto.response.DetailsDto(u.username,u.isActive,u.role)
            from User u
            where u.username=?1
            """)
    DetailsDto getUserDetails(String loggedInUsername);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    // Find all users having the given role.
    List<User> findByRole(Role role);
}