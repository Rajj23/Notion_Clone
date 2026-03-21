package com.blockverse.app.dto;

import com.blockverse.app.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Integer userId;
    private String message;
    private NotificationType type;
    private Integer referencedId;
}
