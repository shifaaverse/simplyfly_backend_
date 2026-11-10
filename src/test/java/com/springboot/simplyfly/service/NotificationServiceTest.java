package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.response.NotificationRespDto;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.OwnershipException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Notification;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.NotificationRepository;
import com.springboot.simplyfly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private User harry;
    private Notification notification;

    @BeforeEach
    void setUp() {
        harry = new User("harry", "encoded", Role.PASSENGER);

        notification = new Notification();
        notification.setId(1L);
        notification.setUser(harry);
        notification.setTitle("Booking confirmed");
        notification.setMessage("PNR ABC123 is confirmed");
        notification.setRead(false);
    }

    @Test
    void addTest() {
        notificationService.add(harry, "Booking confirmed", "PNR ABC123 is confirmed");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        assertEquals(harry, captor.getValue().getUser());
        assertEquals("Booking confirmed", captor.getValue().getTitle());
        // new notification is unread
        assertFalse(captor.getValue().isRead());
    }

    @Test
    void addTest_NoUser() {
        notificationService.add(null, "Title", "Message");

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void addForAdminsTest() {
        User admin1 = new User("admin", "encoded", Role.ADMIN);
        User admin2 = new User("admin2", "encoded", Role.ADMIN);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin1, admin2));

        notificationService.addForAdmins("New complaint", "Someone raised a complaint");

        // one notification for each admin
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void getMineTest() {
        when(notificationRepository.getByUsername(eq("harry"), any(Pageable.class)))
                .thenReturn(List.of(notification));

        List<NotificationRespDto> list = notificationService.getMine("harry");

        assertEquals(1, list.size());
        assertEquals("Booking confirmed", list.get(0).title());
        assertFalse(list.get(0).isRead());
    }

    @Test
    void getMineTest_NoNotifications() {
        when(notificationRepository.getByUsername(eq("harry"), any(Pageable.class))).thenReturn(List.of());

        assertTrue(notificationService.getMine("harry").isEmpty());
    }

    @Test
    void getUnreadCountTest() {
        when(notificationRepository.countUnreadByUsername("harry")).thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount("harry"));
    }

    @Test
    void markAllReadTest() {
        notificationService.markAllRead("harry");

        verify(notificationRepository, times(1)).markAllReadByUsername("harry");
    }

    @Test
    void markReadTest() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markRead(1L, "harry");

        assertTrue(notification.isRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void markReadTest_OtherUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(OwnershipException.class, () -> notificationService.markRead(1L, "ron"));

        assertFalse(notification.isRead());
        verify(notificationRepository, times(0)).save(any(Notification.class));
    }

    @Test
    void markReadTest_NotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markRead(99L, "harry"));
    }
}
