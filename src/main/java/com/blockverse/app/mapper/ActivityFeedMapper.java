package com.blockverse.app.mapper;

import com.blockverse.app.dto.activityFeed.ActivityFeedResponse;
import com.blockverse.app.entity.AuditLog;
import lombok.Builder;

@Builder
public class ActivityFeedMapper {
    public static ActivityFeedResponse toResponse(AuditLog log){
        ActivityFeedResponse response = new ActivityFeedResponse();
        
        response.setUserId(log.getUserId());
        response.setAction(log.getActionType().name());
        response.setEntityId(log.getEntityId());
        response.setMetaData(log.getMetadata());
        response.setCreatedAt(log.getCreatedAt());
        
        return response;
    }
}
