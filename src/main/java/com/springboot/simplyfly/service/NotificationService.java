package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.response.NotificationRespDto;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Notification;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.NotificationRepository;
import com.springboot.simplyfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    // Number of notifications shown in the bell.
    public static final int LATEST_COUNT = 30;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Send a notification to one user.
    public void add(User user, String title, String message) {
        if (user == null)
            return;

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    // Send a notification to every admin.
    public void addForAdmins(String title, String message) {
        userRepository.findByRole(Role.ADMIN)
                .forEach(admin -> add(admin, title, message));
    }

    // Get the latest notifications of the logged-in user.
    public List<NotificationRespDto> getMine(String username) {
        return notificationRepository.getByUsername(username, PageRequest.of(0, LATEST_COUNT))
                .stream()
                .map(n -> new NotificationRespDto(n.getId(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    // Count the unread notifications of the logged-in user.
    public long getUnreadCount(String username) {
        return notificationRepository.countUnreadByUsername(username);
    }

    // Mark all notifications of the logged-in user as read.
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(String username) {
        notificationRepository.markAllReadByUsername(username);
    }

    // Mark one notification as read.
    public void markRead(long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getUsername().equals(username))
            throw new OwnershipException("Operation not allowed! Notification belongs to another user");

        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
