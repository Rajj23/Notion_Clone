package com.blockverse.app.controller;

import com.blockverse.app.dto.activityFeed.ActivityFeedRequest;
import com.blockverse.app.dto.activityFeed.ActivityFeedResponse;
import com.blockverse.app.service.ActivityFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/workspace")
@RequiredArgsConstructor
public class ActivityFeedController {
    
    private final ActivityFeedService activityFeedService;
    
    @GetMapping("/{workspaceId}/activity-feed")
    public ResponseEntity<List<ActivityFeedResponse>> getActivityFeed(@PathVariable int workspaceId, ActivityFeedRequest request){
        return ResponseEntity.ok(activityFeedService.getActivityFeed(workspaceId, request));
    }
}
