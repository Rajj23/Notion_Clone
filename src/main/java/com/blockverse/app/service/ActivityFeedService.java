package com.blockverse.app.service;

import com.blockverse.app.dto.activityFeed.ActivityFeedRequest;
import com.blockverse.app.dto.activityFeed.ActivityFeedResponse;
import com.blockverse.app.entity.AuditLog;
import com.blockverse.app.entity.User;
import com.blockverse.app.entity.WorkSpace;
import com.blockverse.app.exception.InsufficientPermissionException;
import com.blockverse.app.exception.WorkSpaceNotFoundException;
import com.blockverse.app.mapper.ActivityFeedMapper;
import com.blockverse.app.repo.AuditLogRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.repo.WorkSpaceRepo;
import com.blockverse.app.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityFeedService {
    private final AuditLogRepo auditLogRepo;
    private final SecurityUtil securityUtil;
    private final WorkSpaceRepo workSpaceRepo;
    private final WorkSpaceMemberRepo workSpaceMemberRepo;
    private final RateLimiterService rateLimiterService;
    
    public List<ActivityFeedResponse> getActivityFeed(int workspaceId, ActivityFeedRequest request) {
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "ACTIVITY_FEED");
        
        WorkSpace workSpace = workSpaceRepo.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkSpaceNotFoundException("Workspace not found"));
        
        workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workSpace)
                .orElseThrow(() -> new InsufficientPermissionException("User is not a member of the workspace"));
        
        int page = Math.max(0, request.getPage());
        int size = Math.max(1, Math.min(request.getSize(), 50));
        PageRequest pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogRepo.findByWorkSpace_IdOrderByCreatedAtDesc(workspaceId, pageable);
        
        return logs.stream()
                .map(ActivityFeedMapper::toResponse)
                .toList();
    }
}
