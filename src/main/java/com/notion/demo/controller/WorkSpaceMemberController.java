package com.notion.demo.controller;

import com.notion.demo.dto.AddMemberRequest;
import com.notion.demo.dto.ChangeMemberRoleRequest;
import com.notion.demo.service.WorkSpaceMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/workspace/member")
@RequiredArgsConstructor
public class WorkSpaceMemberController {
    
    private final WorkSpaceMemberService workSpaceMemberService;
    
    @PostMapping("/{workspaceId}/add")
    public ResponseEntity<String> addMemberToWorkSpace(@PathVariable int workspaceId, @Valid @RequestBody AddMemberRequest request) {
        workSpaceMemberService.addMemberToWorkSpace(workspaceId, request);
        return ResponseEntity.ok("Member added to workspace successfully");
    }
    
    @DeleteMapping("/{workspaceId}/remove")
    public ResponseEntity<String> removeMemberFromWorkSpace(@PathVariable int workspaceId, @RequestParam String email) {
        workSpaceMemberService.removeMemberFromWorkSpace(workspaceId, email);
        return ResponseEntity.ok("Member removed from workspace successfully");
    }
    
    @PostMapping("/{workspaceId}/change-role")
    public ResponseEntity<String> changeMemberRoleInWorkSpace(@PathVariable int workspaceId, @Valid @RequestBody ChangeMemberRoleRequest request) {
        workSpaceMemberService.changeMemberRole(workspaceId, request);
        return ResponseEntity.ok("Member role changed successfully");
    }
    
    @PostMapping("/{workspaceId}/leave")
    public ResponseEntity<String> leaveWorkSpace(@PathVariable int workspaceId) {
        workSpaceMemberService.leaveWorkSpace(workspaceId);
        return ResponseEntity.ok("Left workspace successfully");
    }
    
    @PostMapping("/{workspaceId}/transfer-ownership")
    public ResponseEntity<String> transferOwnership(@PathVariable int workspaceId, @RequestParam String newOwnerEmail) {
        workSpaceMemberService.transferOwnership(workspaceId, newOwnerEmail);
        return ResponseEntity.ok("Workspace ownership transferred successfully");
    }
    
    @GetMapping("/{workspaceId}/count-members")
    public ResponseEntity<Integer> countMembersInWorkSpace(@PathVariable int workspaceId) {
        return ResponseEntity.ok(workSpaceMemberService.countMembersInWorkSpace(workspaceId));
    }
}
