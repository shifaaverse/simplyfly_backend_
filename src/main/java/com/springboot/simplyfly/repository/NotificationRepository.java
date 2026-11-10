package com.springboot.simplyfly.repository;

import com.springboot.simplyfly.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Get the latest notifications of a user.
    @Query("""
            select n from Notification n
            join n.user u
            where u.username=?1
            order by n.createdAt desc, n.id desc
            """)
    List<Notification> getByUsername(String username, Pageable pageable);

    // Count the notifications a user has not read yet.
    @Query("""
            select count(n.id) from Notification n
            join n.user u
            where u.username=?1
            AND n.isRead = false
            """)
    long countUnreadByUsername(String username);

    // Mark all notifications of a user as read.
    @Modifying
    @Query("""
            update Notification n set n.isRead = true
            where n.isRead = false
            AND n.user.id in (select u.id from User u where u.username = ?1)
            """)
    int markAllReadByUsername(String username);
}
