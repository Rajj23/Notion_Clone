package com.blockverse.app.notification;

import com.blockverse.app.dto.BulkNotificationEvent;
import com.blockverse.app.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.notification:notification-topic}")
    private String notificationTopic;

    @Value("${app.kafka.topic.bulk-notification:bulk-notification-topic}")
    private String bulkNotificationTopic;

    public void send(NotificationEvent event) {
        kafkaTemplate.send(notificationTopic, event);
    }

    public void sendBulk(BulkNotificationEvent event) {
        kafkaTemplate.send(bulkNotificationTopic, event);
    }
}
