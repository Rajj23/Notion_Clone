package com.notion.demo.service;

import com.notion.demo.dto.UpdateWorkSpaceRequest;
import com.notion.demo.dto.WorkSpaceCreateRequest;
import com.notion.demo.dto.WorkSpaceDetailsResponse;
import com.notion.demo.entity.User;
import com.notion.demo.entity.WorkSpace;
import com.notion.demo.entity.WorkSpaceMember;
import com.notion.demo.exception.InsufficientPermissionException;
import com.notion.demo.exception.NotWorkSpaceMemberException;
import com.notion.demo.exception.WorkSpaceNotFoundException;
import com.notion.demo.repo.UserRepo;
import com.notion.demo.repo.WorkSpaceMemberRepo;
import com.notion.demo.repo.WorkSpaceRepo;
import com.notion.demo.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.notion.demo.enums.WorkSpaceRole.ADMIN;
import static com.notion.demo.enums.WorkSpaceRole.OWNER;

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
