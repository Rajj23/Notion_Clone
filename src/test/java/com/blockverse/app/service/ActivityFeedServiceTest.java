package com.blockverse.app.service;

import com.blockverse.app.dto.activityFeed.ActivityFeedRequest;
import com.blockverse.app.dto.activityFeed.ActivityFeedResponse;
import com.blockverse.app.entity.AuditLog;
import com.blockverse.app.entity.User;
import com.blockverse.app.entity.WorkSpace;
import com.blockverse.app.entity.WorkSpaceMember;
import com.blockverse.app.exception.TooManyRequestsException;
import com.blockverse.app.repo.AuditLogRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.repo.WorkSpaceRepo;
import com.blockverse.app.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceTest {

    @Mock
    private AuditLogRepo auditLogRepo;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private WorkSpaceRepo workSpaceRepo;
    @Mock
    private WorkSpaceMemberRepo workSpaceMemberRepo;
    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private ActivityFeedService activityFeedService;

    private User testUser;
    private WorkSpace testWorkspace;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1).email("test@example.com").build();
        testWorkspace = WorkSpace.builder().id(1).build();
    }

    @Test
    void getActivityFeed_success() {
        ActivityFeedRequest request = new ActivityFeedRequest(0, 10);
        
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        when(workSpaceRepo.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(testWorkspace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(testUser, testWorkspace))
                .thenReturn(Optional.of(new WorkSpaceMember()));
                
        AuditLog log = AuditLog.builder()
                .userId(1)
                .actionType(com.blockverse.app.enums.AuditActionType.DOCUMENT_CREATED)
                .entityType(com.blockverse.app.enums.AuditEntityType.DOCUMENT)
                .entityId(100)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepo.findByWorkSpace_IdOrderByCreatedAtDesc(eq(1), any(PageRequest.class)))
                .thenReturn(page);

        List<ActivityFeedResponse> response = activityFeedService.getActivityFeed(1, request);
        
        assertNotNull(response);
        assertEquals(1, response.size());
        verify(rateLimiterService).checkRateLimit(1, "ACTIVITY_FEED");
    }

    @Test
    void getActivityFeed_rateLimitExceeded() {
        ActivityFeedRequest request = new ActivityFeedRequest(0, 10);
        
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        doThrow(new TooManyRequestsException("Too many requests"))
            .when(rateLimiterService).checkRateLimit(1, "ACTIVITY_FEED");
            
        assertThrows(TooManyRequestsException.class, () -> activityFeedService.getActivityFeed(1, request));
    }
}
