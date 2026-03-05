package com.blockverse.app.service;

import com.blockverse.app.dto.workspaceMember.AddMemberRequest;
import com.blockverse.app.dto.workspaceMember.ChangeMemberRoleRequest;
import com.blockverse.app.entity.User;
import com.blockverse.app.entity.WorkSpace;
import com.blockverse.app.entity.WorkSpaceMember;
import com.blockverse.app.enums.WorkSpaceRole;
import com.blockverse.app.exception.*;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.repo.WorkSpaceRepo;
import com.blockverse.app.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.blockverse.app.enums.WorkSpaceRole.ADMIN;
import static com.blockverse.app.enums.WorkSpaceRole.OWNER;


@Service
@Transactional
@RequiredArgsConstructor
public class WorkSpaceMemberService {
    
    private final WorkSpaceRepo workSpaceRepo;
    private final UserRepo userRepo;
    private final WorkSpaceMemberRepo workSpaceMemberRepo;
    private final SecurityUtil securityUtil;

    
    private WorkSpace getWorkSpaceOrThrow(int workspaceId) {
        return workSpaceRepo.findById(workspaceId)
                .orElseThrow(() -> new WorkSpaceNotFoundException("WorkSpace not found"));
    }
    
    private WorkSpaceMember getMembershipOrThrow(User user, WorkSpace workSpace) {
        return workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workSpace)
                .orElseThrow(() -> new NotWorkSpaceMemberException("User is not a member of this workspace"));
    }
    
    private User getUserOrThrow(String email){
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    private boolean isOwnerOrAdmin(WorkSpaceRole role){
        return role == OWNER || role == ADMIN;
    }

    @Transactional
    public void addMemberToWorkSpace(int workspaceId, AddMemberRequest request){
        
        User currentUser = securityUtil.getLoggedInUser();
        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);
        
        WorkSpaceMember currentUserMembership =  getMembershipOrThrow(currentUser, workSpace);
        
        if(!isOwnerOrAdmin(currentUserMembership.getRole())) {
            throw new InsufficientPermissionException("Only workspace owners and admin can add members");
        }
        
        if(request.getRole() == OWNER && 
            currentUserMembership.getRole() != OWNER) {
            throw new InsufficientPermissionException("Only owner can assign owner role");
        }

        User user = getUserOrThrow(request.getEmail());

        Optional<WorkSpaceMember> existingMember = workSpaceMemberRepo.findByUserAndWorkSpace(user, workSpace);
        
        if(existingMember.isPresent()){
            WorkSpaceMember member = existingMember.get();
            
            if(member.getDeletedAt() != null){
                member.setDeletedAt(null);
                member.setRole(request.getRole());
                workSpaceMemberRepo.save(member);
                return;
            }
            
            throw new ActiveMemberException("User already active member of the workspace");
        }

        workSpaceMemberRepo.save(
                WorkSpaceMember.builder()
                        .workSpace(workSpace)
                        .user(user)
                        .role(request.getRole())
                        .build()
        );
    }
    
    public void removeMemberFromWorkSpace(int workspaceId, String email){
        User currentUser = securityUtil.getLoggedInUser();
        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);
        
        WorkSpaceMember currentUserMembership = getMembershipOrThrow(currentUser, workSpace);
        
        if(!isOwnerOrAdmin(currentUserMembership.getRole())){
            throw new InsufficientPermissionException("Only workspace owners and admin can remove members");
        }
        
        User user = getUserOrThrow(email);
        
        WorkSpaceMember membershipToRemove = getMembershipOrThrow(user, workSpace);
        
        if(membershipToRemove.getRole() == OWNER) {
            throw new InsufficientPermissionException("Cannot remove the owner of the workspace");
        }

        if(currentUserMembership.getRole() == ADMIN
                && membershipToRemove.getRole() == ADMIN) {
            throw new InsufficientPermissionException("Admin cannot remove another admin");
        }
        
        membershipToRemove.setDeletedAt(LocalDateTime.now());
        workSpaceMemberRepo.save(membershipToRemove);
    }
    
    public void changeMemberRole(int workspaceId, ChangeMemberRoleRequest request){
        User currentUser = securityUtil.getLoggedInUser();
        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);
        
        WorkSpaceMember currentUserMembership = getMembershipOrThrow(currentUser, workSpace);
        
        if(!isOwnerOrAdmin(currentUserMembership.getRole())) {
            throw new InsufficientPermissionException("Only workspace owners and admin can change member roles");
        }
        
        if(request.getRole() == OWNER && 
            currentUserMembership.getRole() != OWNER) {
            throw new InsufficientPermissionException("Only owner can assign owner role");
        }
        
        User user = getUserOrThrow(request.getEmail());
        
        WorkSpaceMember membershipToChange = getMembershipOrThrow(user, workSpace);
        
        if(membershipToChange.getRole() == OWNER) {
            throw new InsufficientPermissionException("Cannot change the role of the owner of the workspace");
        }
        
        membershipToChange.setRole(request.getRole());
        workSpaceMemberRepo.save(membershipToChange);
    }
    
    public void leaveWorkSpace(int workspaceId){
        User user = securityUtil.getLoggedInUser();
        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);
        
        WorkSpaceMember currentUserMembership = getMembershipOrThrow(user, workSpace);
        
        if(currentUserMembership.getRole() == OWNER){
            throw new OwnerLevelException("Owner cannot leave the workspace. Please transfer ownership or delete the workspace.");
        }
        
        workSpaceMemberRepo.delete(currentUserMembership);
    }
    
    public void transferOwnership(int workspaceId, String newOwnerEmail){
        User user = securityUtil.getLoggedInUser();
        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);
        
        WorkSpaceMember currentUserMembership = getMembershipOrThrow(user, workSpace);
        
        if(currentUserMembership.getRole() != OWNER) {
             throw new InsufficientPermissionException("Only the current owner can transfer ownership");
        }
        
        User newOwner = userRepo.findByEmail(newOwnerEmail)
                .orElseThrow(()-> new UserNotFoundException("New owner user not found"));
        
        WorkSpaceMember newOwnerMembership = workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(newOwner, workSpace)
                .orElseThrow(() -> new NotWorkSpaceMemberException("New owner must be a member of the workspace"));
        
        newOwnerMembership.setRole(OWNER);
        currentUserMembership.setRole(ADMIN);

        workSpaceMemberRepo.save(newOwnerMembership);
        workSpaceMemberRepo.save(currentUserMembership);
    }
    
    public int countMembersInWorkSpace(int workSpaceId){
        return workSpaceMemberRepo.countByWorkSpaceIdAndDeletedAtIsNull(workSpaceId);
    }
    
}

