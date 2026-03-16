package com.blockverse.app.service;

import com.blockverse.app.dto.workspaceMember.AddMemberRequest;
import com.blockverse.app.dto.workspaceMember.ChangeMemberRoleRequest;
import com.blockverse.app.entity.User;
import com.blockverse.app.entity.WorkSpace;
import com.blockverse.app.entity.WorkSpaceMember;
import com.blockverse.app.enums.WorkSpaceRole;
import com.blockverse.app.exception.InsufficientPermissionException;
import com.blockverse.app.exception.OwnerLevelException;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.repo.WorkSpaceRepo;
import com.blockverse.app.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkspaceMemberServiceTest {
    
    @Mock
    private WorkSpaceRepo workSpaceRepo;
    
    @Mock
    private UserRepo userRepo;
    
    @Mock
    private WorkSpaceMemberRepo workSpaceMemberRepo;
    
    @Mock
    private SecurityUtil securityUtil;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private WorkSpaceMemberService service;
    
    @Test
    void addMember_shouldThrow_whenUserNotOwnerOrAdmin(){
        User currentUser = User.builder().build();
        WorkSpace workSpace = WorkSpace.builder().build();

        WorkSpaceMember membership = WorkSpaceMember.builder().role(WorkSpaceRole.MEMBER).build();
        
        when(securityUtil.getLoggedInUser()).thenReturn(currentUser);
        when(workSpaceRepo.findById(1)).thenReturn(Optional.of(workSpace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(currentUser, workSpace)).thenReturn(Optional.of(membership));

        AddMemberRequest request = new AddMemberRequest("test@mail.com", WorkSpaceRole.MEMBER);
        
        assertThrows(InsufficientPermissionException.class, () -> service.addMemberToWorkSpace(1, request));
    }
    
    @Test
    void AddMember_shouldThrow_whenAdminAssignOwner(){
        User currentUser = User.builder().build();
        WorkSpace workSpace = WorkSpace.builder().build();

        WorkSpaceMember membership = WorkSpaceMember.builder().role(WorkSpaceRole.ADMIN).build();

        when(securityUtil.getLoggedInUser()).thenReturn(currentUser);
        when(workSpaceRepo.findById(1)).thenReturn(Optional.of(workSpace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(currentUser, workSpace)).thenReturn(Optional.of(membership));

        AddMemberRequest request = new AddMemberRequest("test@mail.com", WorkSpaceRole.OWNER);

        assertThrows(InsufficientPermissionException.class, () -> service.addMemberToWorkSpace(1, request));
    }
    
    @Test
    void addMember_shouldSaveMember_whenValid(){
        User newUser = User.builder().build();
        User currentUser = User.builder().build();
        WorkSpace workSpace = WorkSpace.builder().build();

        WorkSpaceMember membership = WorkSpaceMember.builder().role(WorkSpaceRole.OWNER).build();

        AddMemberRequest request = new AddMemberRequest("user@gmail.com", WorkSpaceRole.MEMBER);

        when(securityUtil.getLoggedInUser()).thenReturn(currentUser);
        when(workSpaceRepo.findById(1)).thenReturn(Optional.of(workSpace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(currentUser, workSpace)).thenReturn(Optional.of(membership));
        when(userRepo.findByEmail("user@gmail.com")).thenReturn(Optional.of(newUser));
        when(workSpaceMemberRepo.findByUserAndWorkSpace(newUser, workSpace))
                .thenReturn(Optional.empty());

        service.addMemberToWorkSpace(1, request);
        verify(workSpaceMemberRepo).save(any(WorkSpaceMember.class));
    }
    
    @Test
    void removeMember_shouldThrow_whenTryingToRemoveOwner(){
        User currentUser = User.builder().build();
        User targetUser = User.builder().build();
        WorkSpace workSpace = WorkSpace.builder().build();
        
        WorkSpaceMember membership = WorkSpaceMember.builder().role(WorkSpaceRole.OWNER).build();
        WorkSpaceMember targetMembership = WorkSpaceMember.builder().role(WorkSpaceRole.OWNER).build();
        
        when(securityUtil.getLoggedInUser()).thenReturn(currentUser);
        when(workSpaceRepo.findById(1)).thenReturn(Optional.of(workSpace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(currentUser, workSpace))
                .thenReturn(Optional.of(membership));
        when(userRepo.findByEmail("test@mail.com")).thenReturn(Optional.of(targetUser));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(targetUser, workSpace))
                .thenReturn(Optional.of(targetMembership));
        
        assertThrows(InsufficientPermissionException.class, ()->service.removeMemberFromWorkSpace(1, "test@mail.com"));
    }
    
    @Test
    void changeRole_shouldThrow_whenTargetIsOwner(){
        User currentUser = User.builder().build();
        User targetUser = User.builder().build();
        WorkSpace workSpace = WorkSpace.builder().build();
        
        WorkSpaceMember currentMembership = WorkSpaceMember.builder().role(WorkSpaceRole.OWNER).build();
        WorkSpaceMember targetMembership = WorkSpaceMember.builder().role(WorkSpaceRole.OWNER).build();

        ChangeMemberRoleRequest request = new ChangeMemberRoleRequest("test@mai.com", WorkSpaceRole.ADMIN);
        
        when(securityUtil.getLoggedInUser()).thenReturn(currentUser);
        when(workSpaceRepo.findById(1)).thenReturn(Optional.of(workSpace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(currentUser, workSpace)).thenReturn(Optional.of(currentMembership));
        when(userRepo.findByEmail("test@mai.com")).thenReturn(Optional.of(targetUser));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(targetUser, workSpace)).thenReturn(Optional.of(targetMembership));
        
        assertThrows(InsufficientPermissionException.class, ()->service.changeMemberRole(1, request));
        
    }
    
    @Test
    void leaveWorkSpace_shouldThrow_whenOwnerTriesToLeave(){
        User currentUser = User.builder().build();
        WorkSpace workSpace = WorkSpace.builder().build();
        
        WorkSpaceMember membership = WorkSpaceMember.builder().role(WorkSpaceRole.OWNER).build();
        
        when(securityUtil.getLoggedInUser()).thenReturn(currentUser);
        when(workSpaceRepo.findById(1)).thenReturn(Optional.of(workSpace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(currentUser, workSpace)).thenReturn(Optional.of(membership));
        
        assertThrows(OwnerLevelException.class, ()->service.leaveWorkSpace(1));
    }
    
    @Test
    void transferOwnership_shouldThrow_whenNotOwner(){

        User currentUser = User.builder().build();
        WorkSpace workSpace = WorkSpace.builder().build();
        
        WorkSpaceMember membership = WorkSpaceMember.builder().role(WorkSpaceRole.ADMIN).build();
        
        when(securityUtil.getLoggedInUser()).thenReturn(currentUser);
        when(workSpaceRepo.findById(1)).thenReturn(Optional.of(workSpace));
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(currentUser, workSpace)).thenReturn(Optional.of(membership));
        
        assertThrows(InsufficientPermissionException.class, ()->service.transferOwnership(1, "new@mail.com"));
    }
    
    @Test
    void countMembers_shouldReturnCount(){
        when(workSpaceMemberRepo.countByWorkSpaceIdAndDeletedAtIsNull(1))
                .thenReturn(5);
        
        int count = service.countMembersInWorkSpace(1);
        
        assertEquals(5, count);
    }
}
