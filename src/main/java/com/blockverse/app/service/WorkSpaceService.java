package com.blockverse.app.service;

import com.blockverse.app.dto.workspace.UpdateWorkSpaceRequest;
import com.blockverse.app.dto.workspace.WorkSpaceCreateRequest;
import com.blockverse.app.dto.workspace.WorkSpaceDetailsResponse;
import com.blockverse.app.entity.User;
import com.blockverse.app.entity.WorkSpace;
import com.blockverse.app.entity.WorkSpaceMember;
import com.blockverse.app.exception.InsufficientPermissionException;
import com.blockverse.app.exception.NotWorkSpaceMemberException;
import com.blockverse.app.exception.WorkSpaceNotFoundException;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.repo.WorkSpaceRepo;
import com.blockverse.app.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.blockverse.app.enums.WorkSpaceRole.ADMIN;
import static com.blockverse.app.enums.WorkSpaceRole.OWNER;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkSpaceService {
    
    private final WorkSpaceRepo workSpaceRepo;
    private final WorkSpaceMemberRepo workSpaceMemberRepo;
    private final SecurityUtil securityUtil;
    private final UserRepo userRepo;

    private WorkSpace getWorkSpaceOrThrow(int workspaceId) {
        return workSpaceRepo.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkSpaceNotFoundException("WorkSpace not found"));
    }

    private WorkSpaceMember getMembershipOrThrow(User user, WorkSpace workSpace) {
        return workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workSpace)
                .orElseThrow(() -> new NotWorkSpaceMemberException("User is not a member of this workspace"));
    }

    public String createWorkSpace(WorkSpaceCreateRequest request){
        User currentUser =  securityUtil.getLoggedInUser();
        
        WorkSpace workSpace = workSpaceRepo.save(
                WorkSpace.builder()
                        .name(request.getName())
                        .type(request.getWorkSpaceType())
                        .build()
        );
        
        workSpaceMemberRepo.save(
                WorkSpaceMember.builder()
                        .workSpace(workSpace)
                        .user(currentUser)
                        .role(OWNER)
                        .build()
        );
        return "WorkSpace created successfully";
    }
    
    
    public void deleteWorkSpace(int workspaceId){
        User currentUser =  securityUtil.getLoggedInUser();
        
        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);
        
        WorkSpaceMember membership = getMembershipOrThrow(currentUser, workSpace);
        
        if(membership.getRole() != OWNER){
            throw new InsufficientPermissionException("Only owner can delete the workspace");
        }
        
        workSpace.setDeletedAt(LocalDateTime.now());
    }
    
    
    public List<WorkSpaceDetailsResponse> getAllWorkSpacesForUser(){
        User currentUser =  securityUtil.getLoggedInUser();
        return workSpaceMemberRepo.findWorkspaceDetailsForUserAndDeletedAtIsNull(currentUser);
    }
    
    
    public WorkSpaceDetailsResponse getWorkSpaceDetails(int workspaceId){
        User currentUser =  securityUtil.getLoggedInUser();
        
        return workSpaceMemberRepo.findWorkspaceDetailsForUserAndWorkspaceId(currentUser, workspaceId)
                .orElseThrow(() -> new NotWorkSpaceMemberException("Not a member or workspace not found"));
    }
    
    public void updateWorkSpace(int workSpaceId, UpdateWorkSpaceRequest request){
        User currentUser =  securityUtil.getLoggedInUser();
        WorkSpace workSpace = getWorkSpaceOrThrow(workSpaceId);
        
        WorkSpaceMember membership =  workSpaceMemberRepo
                .findByUserAndWorkSpaceAndDeletedAtIsNull(currentUser, workSpace)
                .orElseThrow(() -> new NotWorkSpaceMemberException("Not a member"));
        
        if(membership.getRole() != OWNER && membership.getRole() != ADMIN){
            throw new InsufficientPermissionException("You are not allowed to update the workspace");
        }
        
        workSpace.setName(request.getName());
        workSpace.setType(request.getWorkSpaceType());
    }
}
