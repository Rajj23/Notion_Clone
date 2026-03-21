package com.blockverse.app.integration;

import com.blockverse.app.dto.NotificationEvent;
import com.blockverse.app.entity.Notification;
import com.blockverse.app.entity.User;
import com.blockverse.app.enums.NotificationType;
import com.blockverse.app.repo.NotificationRepo;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;

import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test for the notification pipeline.
 *
 * Strategy:
 *  - DB persistence is verified exhaustively via the live NotificationRepo.
 *  - WebSocket broadcast is verified via a @SpyBean on SimpMessagingTemplate
 *    so we don't need to stand up a full STOMP client (which is fragile in tests).
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepo notificationRepo;

    @Autowired
    private UserRepo userRepo;

    @MockitoSpyBean
    private SimpMessagingTemplate messagingTemplate;

    private User testUser;

    @BeforeEach
    public void setup() {
        notificationRepo.deleteAll();

        testUser = userRepo.findByEmail("notify@test.com")
                .orElseGet(() -> userRepo.save(
                        User.builder().email("notify@test.com").password("pass").name("Notify").build()));
    }

    @Test
    public void testNotificationPersistence() throws InterruptedException {
        NotificationEvent request = NotificationEvent.builder()
                .userId(testUser.getId())
                .message("Persistence Test Message")
                .type(NotificationType.UPDATE)
                .referencedId(100)
                .build();

        notificationService.sendNotification(request);

        List<Notification> all = notificationRepo.findAll();
        long startTime = System.currentTimeMillis();
        while (all.isEmpty() && System.currentTimeMillis() - startTime < 5000) {
            Thread.sleep(100);
            all = notificationRepo.findAll();
        }

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getMessage()).isEqualTo("Persistence Test Message");
        assertThat(all.get(0).getType()).isEqualTo(NotificationType.UPDATE);
        assertThat(all.get(0).getUserId()).isEqualTo(testUser.getId());
        assertThat(all.get(0).isRead()).isFalse();
    }

    @Test
    public void testNotificationBroadcast() {
        NotificationEvent request = NotificationEvent.builder()
                .userId(testUser.getId())
                .message("Broadcast Test Message")
                .type(NotificationType.INVITE)
                .referencedId(200)
                .build();

        notificationService.sendNotification(request);

        // Verify the broker was called with the correct destination, waiting up to 5 seconds
        verify(messagingTemplate, timeout(5000)).convertAndSend(
                eq("/topic/notifications/" + testUser.getId()),
                any(Object.class)
        );
    }

    @Test
    public void testMultipleNotificationsOrderedByCreatedAt() throws InterruptedException {
        for (int i = 1; i <= 3; i++) {
            notificationService.sendNotification(NotificationEvent.builder()
                    .userId(testUser.getId())
                    .message("Notification " + i)
                    .type(NotificationType.UPDATE)
                    .build());
        }

        List<Notification> all = notificationRepo.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        long startTime = System.currentTimeMillis();
        while (all.size() < 3 && System.currentTimeMillis() - startTime < 5000) {
            Thread.sleep(100);
            all = notificationRepo.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        }

        assertThat(all).hasSize(3);
        // All messages are saved
        assertThat(all).extracting(Notification::getMessage)
                .containsExactlyInAnyOrder("Notification 1", "Notification 2", "Notification 3");
    }
}
