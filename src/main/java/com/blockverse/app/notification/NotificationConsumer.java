package com.blockverse.app.notification;

import com.blockverse.app.dto.BulkNotificationEvent;
import com.blockverse.app.dto.NotificationEvent;
import com.blockverse.app.dto.NotificationResponse;
import com.blockverse.app.entity.Notification;
import com.blockverse.app.repo.NotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepo notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = "${app.kafka.topic.notification:notification-topic}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}"
    )
    @Transactional
    public void consume(NotificationEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .message(event.getMessage())
                .type(event.getType())
                .referencedId(event.getReferencedId())
                .build();

        Notification saved = notificationRepo.save(notification);

        messagingTemplate.convertAndSend("/topic/notifications/" + event.getUserId(), toResponse(saved));
    }

    @KafkaListener(
            topics = "${app.kafka.topic.bulk-notification:bulk-notification-topic}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}"
    )
    @Transactional
    public void consumeBulk(BulkNotificationEvent event) {

        List<Notification> notifications = event.getUserIds().stream()
                .map(userId -> Notification.builder()
                        .userId(userId)
                        .message(event.getMessage())
                        .type(event.getType())
                        .referencedId(event.getReferencedId())
                        .build())
                .toList();

        List<Notification> saved = notificationRepo.saveAll(notifications);
        saved.forEach(notification -> messagingTemplate.convertAndSend(
                "/topic/notifications/" + notification.getUserId(), toResponse(notification)
        ));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .read(notification.isRead())
                .type(notification.getType())
                .referencedId(notification.getReferencedId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
