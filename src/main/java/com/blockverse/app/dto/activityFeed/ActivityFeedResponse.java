package com.blockverse.app.dto.activityFeed;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFeedResponse {
    private int userId;
    private String action;
    private String entityType;
    private int entityId;
    private String metaData;
    private LocalDateTime createdAt;
            
}
