package com.blockverse.app.notification;

import com.blockverse.app.dto.BulkNotificationEvent;
import com.blockverse.app.dto.NotificationEvent;
import com.blockverse.app.dto.NotificationResponse;
import com.blockverse.app.entity.Notification;
import com.blockverse.app.entity.User;
import com.blockverse.app.enums.NotificationType;
import com.blockverse.app.exception.DocumentException;
import com.blockverse.app.repo.NotificationRepo;
import com.blockverse.app.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProducer notificationProducer;
    private final NotificationRepo notificationRepo;
    private final SecurityUtil securityUtil;

    public void sendNotification(NotificationEvent request) {
        notificationProducer.send(request);
    }

    public void sendBulkNotification(List<Integer> userIds, String message, NotificationType type, Integer referencedId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        BulkNotificationEvent event = BulkNotificationEvent.builder()
                .userIds(userIds)
                .message(message)
                .type(type)
                .referencedId(referencedId)
                .build();
        notificationProducer.sendBulk(event);
    }

    public List<NotificationResponse> getUserNotifications() {
        User user = securityUtil.getLoggedInUser();
        List<Notification> notifications = notificationRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
        return notifications.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadUserNotifications() {
        User user = securityUtil.getLoggedInUser();
        List<Notification> notifications = notificationRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
        return notifications.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void markAsRead(int notificationId) {
        User user = securityUtil.getLoggedInUser();
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new DocumentException("Notification not found"));

        if (!notification.getUserId().equals(user.getId())) {
            throw new DocumentException("Not authorized to access this notification");
        }

        notification.setRead(true);
        notificationRepo.save(notification);
    }

    public void markAllAsRead() {
        User user = securityUtil.getLoggedInUser();
        List<Notification> unread = notificationRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
        unread.forEach(n -> n.setRead(true));
        notificationRepo.saveAll(unread);
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
