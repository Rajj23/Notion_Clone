package com.blockverse.app.service;

import com.blockverse.app.dto.NotificationEvent;
import com.blockverse.app.dto.NotificationResponse;
import com.blockverse.app.entity.Notification;
import com.blockverse.app.entity.User;
import com.blockverse.app.enums.NotificationType;
import com.blockverse.app.exception.DocumentException;
import com.blockverse.app.notification.NotificationService;
import com.blockverse.app.notification.NotificationProducer;
import com.blockverse.app.repo.NotificationRepo;
import com.blockverse.app.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepo notificationRepo;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1).email("test@mail.com").build();
    }

    @Test
    void sendNotification_success() {
        NotificationEvent request = NotificationEvent.builder()
                .userId(1)
                .message("Test")
                .type(NotificationType.UPDATE)
                .referencedId(10)
                .build();

        notificationService.sendNotification(request);

        verify(notificationProducer).send(request);
    }

    @Test
    void getUserNotifications_success() {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        Notification n = Notification.builder().id(1).message("Hi").build();
        when(notificationRepo.findByUserIdOrderByCreatedAtDesc(1)).thenReturn(List.of(n));

        List<NotificationResponse> responses = notificationService.getUserNotifications();
        
        assertEquals(1, responses.size());
        assertEquals("Hi", responses.get(0).getMessage());
    }

    @Test
    void markAsRead_success() {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        Notification n = Notification.builder().id(10).userId(1).read(false).build();
        when(notificationRepo.findById(10)).thenReturn(Optional.of(n));

        notificationService.markAsRead(10);
        
        assertTrue(n.isRead());
        verify(notificationRepo).save(n);
    }

    @Test
    void markAsRead_unauthorized() {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        Notification n = Notification.builder().id(10).userId(2).read(false).build(); // different user
        when(notificationRepo.findById(10)).thenReturn(Optional.of(n));

        assertThrows(DocumentException.class, () -> notificationService.markAsRead(10));
        verify(notificationRepo, never()).save(any());
    }
}
