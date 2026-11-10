package com.springboot.simplyfly.controller;

import com.springboot.simplyfly.dto.response.NotificationRespDto;
import com.springboot.simplyfly.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

// Notifications (bell icon) for every logged-in user - passenger, airline and admin.
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    private final NotificationService notificationService;

    // Step 1: Get the latest notifications of the logged-in user.
    @GetMapping("/api/notification/all")
    public List<NotificationRespDto> getMine(Principal principal) {
        return notificationService.getMine(principal.getName());
    }

    // Step 2: Get the number of unread notifications.
    @GetMapping("/api/notification/count")
    public long getUnreadCount(Principal principal) {
        return notificationService.getUnreadCount(principal.getName());
    }

    // Step 3: Mark all notifications as read.
    @PutMapping("/api/notification/read")
    public void markAllRead(Principal principal) {
        notificationService.markAllRead(principal.getName());
    }

    // Step 4: Mark one notification as read.
    @PutMapping("/api/notification/read/{notificationId}")
    public void markRead(@PathVariable long notificationId, Principal principal) {
        notificationService.markRead(notificationId, principal.getName());
    }
}
