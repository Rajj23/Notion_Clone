package com.notion.demo.service;

import com.notion.demo.dto.UpdateWorkSpaceRequest;
import com.notion.demo.dto.WorkSpaceCreateRequest;
import com.notion.demo.dto.WorkSpaceDetailsResponse;
import com.notion.demo.entity.User;
import com.notion.demo.entity.WorkSpace;
import com.notion.demo.entity.WorkSpaceMember;
import com.notion.demo.enums.WorkSpaceRole;
import com.notion.demo.enums.WorkSpaceType;
import com.notion.demo.exception.InsufficientPermissionException;
import com.notion.demo.repo.UserRepo;
import com.notion.demo.repo.WorkSpaceMemberRepo;
import com.notion.demo.repo.WorkSpaceRepo;
import com.notion.demo.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkspaceServiceTest {
    
    @Mock
    private WorkSpaceRepo workSpaceRepo;
    
    @Mock
    private WorkSpaceMemberRepo workSpaceMemberRepo;
    
    @Mock
    private SecurityUtil securityUtil;
    
    @Mock
    private UserRepo userRepo;
    
    @InjectMocks
    private WorkSpaceService workSpaceService;
    
    @Test
    void createWorkspace_shouldCreateWorkspaceAndOwnerMembership(){
        User currentUser = User.builder().id(1).build();

        WorkSpaceCreateRequest request = new WorkSpaceCreateRequest("MySpace", WorkSpaceType.PRIVATE);

        WorkSpace savedWorkspace = WorkSpace.builder().id(10).name("MySpace").build();
        
        when(securityUtil.getLoggedInUser()).thenReturn(currentUser);
        when(workSpaceRepo.save(any(WorkSpace.class))).thenReturn(savedWorkspace);
        
        String result = workSpaceService.createWorkSpace(request);
        
        assertEquals("WorkSpace created successfully", result);
        
        verify(workSpaceRepo).save(any(WorkSpace.class));
        verify(workSpaceMemberRepo).save(any(WorkSpaceMember.class));
    }
    
    @Test
    void deleteWorkspace_shouldThrowException_whenUserNotOwner(){
        User user = User.builder().id(1).build();
        WorkSpace workspace = WorkSpace.builder().id(10).build();
        
        WorkSpaceMember member = WorkSpaceMember.builder()
                .user(user)
                .workSpace(workspace)
                .role(WorkSpaceRole.MEMBER)
                .build();
        
        when(securityUtil.getLoggedInUser()).thenReturn(user);
        when(workSpaceRepo.findByIdAndDeletedAtIsNull(10))
                .thenReturn(Optional.of(workspace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workspace))
                .thenReturn(Optional.of(member));
        
        assertThrows(InsufficientPermissionException.class, ()->
                workSpaceService.deleteWorkSpace(10));
    }
    
    @Test
    void deleteWorkspace_shouldDelete_whenUserIsOwner(){
        User user = User.builder().id(1).build();
        WorkSpace workSpace = WorkSpace.builder().id(10).build();
        
        WorkSpaceMember member = WorkSpaceMember.builder()
                .user(user)
                .workSpace(workSpace)
                .role(WorkSpaceRole.OWNER)
                .build();
        
        when(securityUtil.getLoggedInUser()).thenReturn(user);
        when(workSpaceRepo.findByIdAndDeletedAtIsNull(10))
                .thenReturn(Optional.of(workSpace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workSpace))
                .thenReturn(Optional.of(member));
        
        workSpaceService.deleteWorkSpace(10);
        assertNotNull(workSpace.getDeletedAt());
    }
    
    @Test
    void updateWorkspace_shouldThrowException_whenAdminOrOwner(){
        User user = User.builder().id(1).build();
        WorkSpace workspace = WorkSpace.builder().id(10).build();
        
        WorkSpaceMember member = WorkSpaceMember.builder()
                .role(WorkSpaceRole.ADMIN)
                .build();

        UpdateWorkSpaceRequest request = new UpdateWorkSpaceRequest("NewName", WorkSpaceType.TEAM);
        
        when(securityUtil.getLoggedInUser()).thenReturn(user);
        when(workSpaceRepo.findByIdAndDeletedAtIsNull(10))
                .thenReturn(Optional.of(workspace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workspace))
                .thenReturn(Optional.of(member));
        
        workSpaceService.updateWorkSpace(10, request);
        assertEquals("NewName", workspace.getName());
    }

    
    @Test
    void getAllWorkspacesForUser_shouldReturnList(){
        User user = User.builder().id(1).build();
        
        when(securityUtil.getLoggedInUser()).thenReturn(user);
        when(workSpaceMemberRepo.findWorkspaceDetailsForUserAndDeletedAtIsNull(user))
                .thenReturn(List.of(new WorkSpaceDetailsResponse()));
        
        List<WorkSpaceDetailsResponse> result = workSpaceService.getAllWorkSpacesForUser();
        
        assertEquals(1, result.size());
    }
}
