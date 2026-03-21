package com.blockverse.app.dto;

import com.blockverse.app.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationEvent {
    private List<Integer> userIds; 
    private String message;
    private NotificationType type;
    private Integer referencedId;
}
