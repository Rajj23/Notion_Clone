package com.notion.demo.controller;

import com.notion.demo.exception.*;
import com.notion.demo.repo.UserRepo;
import com.notion.demo.security.AuthService;
import com.notion.demo.security.JwtUtil;
import com.notion.demo.service.WorkSpaceMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkSpaceMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
public class WorkspaceMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepo userRepo;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private WorkSpaceMemberService workSpaceMemberService;
    
    @Test
    void addMember_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(workSpaceMemberService).addMemberToWorkSpace(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Member added to workspace successfully"));
    }

    @Test
    void addMember_shouldReturnForbidden_whenInsufficientPermission() throws Exception {
        doThrow(new InsufficientPermissionException("Only workspace owners and admin can add members"))
                .when(workSpaceMemberService).addMemberToWorkSpace(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only workspace owners and admin can add members"));
    }

    @Test
    void addMember_shouldReturnForbidden_whenNonOwnerAssignsOwnerRole() throws Exception {
        doThrow(new InsufficientPermissionException("Only owner can assign owner role"))
                .when(workSpaceMemberService).addMemberToWorkSpace(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "OWNER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only owner can assign owner role"));
    }

    @Test
    void addMember_shouldReturnNotFound_whenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException("User not found"))
                .when(workSpaceMemberService).addMemberToWorkSpace(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "nonexistent@mail.com",
                                    "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void addMember_shouldReturnNotFound_whenWorkspaceNotFound() throws Exception {
        doThrow(new WorkSpaceNotFoundException("WorkSpace not found"))
                .when(workSpaceMemberService).addMemberToWorkSpace(eq(999), any());

        mockMvc.perform(post("/v1/workspace/member/999/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WorkSpace not found"));
    }

    @Test
    void addMember_shouldReturnBadRequest_whenUserAlreadyActiveMember() throws Exception {
        doThrow(new ActiveMemberException("User already active member of the workspace"))
                .when(workSpaceMemberService).addMemberToWorkSpace(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "existing@mail.com",
                                    "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User already active member of the workspace"));
    }

    @Test
    void addMember_shouldReturnBadRequest_whenEmailIsEmpty() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "",
                                    "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addMember_shouldReturnBadRequest_whenRoleIsMissing() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addMember_shouldReturnBadRequest_whenEmailIsMissing() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addMember_shouldReturnBadRequest_whenInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addMember_shouldReturnBadRequest_whenInvalidRole() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "SUPERADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }


    @Test
    void removeMember_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(workSpaceMemberService).removeMemberFromWorkSpace(1, "test@mail.com");

        mockMvc.perform(delete("/v1/workspace/member/1/remove")
                        .param("email", "test@mail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Member removed from workspace successfully"));
    }

    @Test
    void removeMember_shouldReturnForbidden_whenInsufficientPermission() throws Exception {
        doThrow(new InsufficientPermissionException("Only workspace owners and admin can remove members"))
                .when(workSpaceMemberService).removeMemberFromWorkSpace(1, "test@mail.com");

        mockMvc.perform(delete("/v1/workspace/member/1/remove")
                        .param("email", "test@mail.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only workspace owners and admin can remove members"));
    }

    @Test
    void removeMember_shouldReturnForbidden_whenTryingToRemoveOwner() throws Exception {
        doThrow(new InsufficientPermissionException("Cannot remove the owner of the workspace"))
                .when(workSpaceMemberService).removeMemberFromWorkSpace(1, "owner@mail.com");

        mockMvc.perform(delete("/v1/workspace/member/1/remove")
                        .param("email", "owner@mail.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Cannot remove the owner of the workspace"));
    }

    @Test
    void removeMember_shouldReturnForbidden_whenAdminRemovesAdmin() throws Exception {
        doThrow(new InsufficientPermissionException("Admin cannot remove another admin"))
                .when(workSpaceMemberService).removeMemberFromWorkSpace(1, "admin@mail.com");

        mockMvc.perform(delete("/v1/workspace/member/1/remove")
                        .param("email", "admin@mail.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin cannot remove another admin"));
    }

    @Test
    void removeMember_shouldReturnNotFound_whenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException("User not found"))
                .when(workSpaceMemberService).removeMemberFromWorkSpace(1, "unknown@mail.com");

        mockMvc.perform(delete("/v1/workspace/member/1/remove")
                        .param("email", "unknown@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void removeMember_shouldReturnForbidden_whenUserNotAMember() throws Exception {
        doThrow(new NotWorkSpaceMemberException("User is not a member of this workspace"))
                .when(workSpaceMemberService).removeMemberFromWorkSpace(1, "notmember@mail.com");

        mockMvc.perform(delete("/v1/workspace/member/1/remove")
                        .param("email", "notmember@mail.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User is not a member of this workspace"));
    }

    @Test
    void removeMember_shouldReturnNotFound_whenWorkspaceNotFound() throws Exception {
        doThrow(new WorkSpaceNotFoundException("WorkSpace not found"))
                .when(workSpaceMemberService).removeMemberFromWorkSpace(999, "test@mail.com");

        mockMvc.perform(delete("/v1/workspace/member/999/remove")
                        .param("email", "test@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WorkSpace not found"));
    }
    

    @Test
    void changeMemberRole_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(workSpaceMemberService).changeMemberRole(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Member role changed successfully"));
    }

    @Test
    void changeMemberRole_shouldReturnForbidden_whenInsufficientPermission() throws Exception {
        doThrow(new InsufficientPermissionException("Only workspace owners and admin can change member roles"))
                .when(workSpaceMemberService).changeMemberRole(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only workspace owners and admin can change member roles"));
    }

    @Test
    void changeMemberRole_shouldReturnForbidden_whenNonOwnerAssignsOwnerRole() throws Exception {
        doThrow(new InsufficientPermissionException("Only owner can assign owner role"))
                .when(workSpaceMemberService).changeMemberRole(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "OWNER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only owner can assign owner role"));
    }

    @Test
    void changeMemberRole_shouldReturnForbidden_whenChangingOwnerRole() throws Exception {
        doThrow(new InsufficientPermissionException("Cannot change the role of the owner of the workspace"))
                .when(workSpaceMemberService).changeMemberRole(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "owner@mail.com",
                                    "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Cannot change the role of the owner of the workspace"));
    }

    @Test
    void changeMemberRole_shouldReturnNotFound_whenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException("User not found"))
                .when(workSpaceMemberService).changeMemberRole(eq(1), any());

        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "unknown@mail.com",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void changeMemberRole_shouldReturnNotFound_whenWorkspaceNotFound() throws Exception {
        doThrow(new WorkSpaceNotFoundException("WorkSpace not found"))
                .when(workSpaceMemberService).changeMemberRole(eq(999), any());

        mockMvc.perform(post("/v1/workspace/member/999/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WorkSpace not found"));
    }

    @Test
    void changeMemberRole_shouldReturnBadRequest_whenEmailIsMissing() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeMemberRole_shouldReturnBadRequest_whenRoleIsMissing() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeMemberRole_shouldReturnBadRequest_whenInvalidRole() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@mail.com",
                                    "role": "SUPERADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeMemberRole_shouldReturnBadRequest_whenInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/v1/workspace/member/1/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
    

    @Test
    void leaveWorkspace_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(workSpaceMemberService).leaveWorkSpace(1);

        mockMvc.perform(post("/v1/workspace/member/1/leave"))
                .andExpect(status().isOk())
                .andExpect(content().string("Left workspace successfully"));
    }

    @Test
    void leaveWorkspace_shouldReturnBadRequest_whenOwnerTriesToLeave() throws Exception {
        doThrow(new OwnerLevelException("Owner cannot leave the workspace. Please transfer ownership or delete the workspace."))
                .when(workSpaceMemberService).leaveWorkSpace(1);

        mockMvc.perform(post("/v1/workspace/member/1/leave"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Owner cannot leave the workspace. Please transfer ownership or delete the workspace."));
    }

    @Test
    void leaveWorkspace_shouldReturnNotFound_whenWorkspaceNotFound() throws Exception {
        doThrow(new WorkSpaceNotFoundException("WorkSpace not found"))
                .when(workSpaceMemberService).leaveWorkSpace(999);

        mockMvc.perform(post("/v1/workspace/member/999/leave"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WorkSpace not found"));
    }

    @Test
    void leaveWorkspace_shouldReturnForbidden_whenUserNotAMember() throws Exception {
        doThrow(new NotWorkSpaceMemberException("User is not a member of this workspace"))
                .when(workSpaceMemberService).leaveWorkSpace(1);

        mockMvc.perform(post("/v1/workspace/member/1/leave"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User is not a member of this workspace"));
    }
    

    @Test
    void transferOwnership_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(workSpaceMemberService).transferOwnership(1, "newowner@mail.com");

        mockMvc.perform(post("/v1/workspace/member/1/transfer-ownership")
                        .param("newOwnerEmail", "newowner@mail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Workspace ownership transferred successfully"));
    }

    @Test
    void transferOwnership_shouldReturnForbidden_whenNotOwner() throws Exception {
        doThrow(new InsufficientPermissionException("Only the current owner can transfer ownership"))
                .when(workSpaceMemberService).transferOwnership(1, "newowner@mail.com");

        mockMvc.perform(post("/v1/workspace/member/1/transfer-ownership")
                        .param("newOwnerEmail", "newowner@mail.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only the current owner can transfer ownership"));
    }

    @Test
    void transferOwnership_shouldReturnNotFound_whenNewOwnerNotFound() throws Exception {
        doThrow(new UserNotFoundException("New owner user not found"))
                .when(workSpaceMemberService).transferOwnership(1, "unknown@mail.com");

        mockMvc.perform(post("/v1/workspace/member/1/transfer-ownership")
                        .param("newOwnerEmail", "unknown@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("New owner user not found"));
    }

    @Test
    void transferOwnership_shouldReturnForbidden_whenNewOwnerNotMember() throws Exception {
        doThrow(new NotWorkSpaceMemberException("New owner must be a member of the workspace"))
                .when(workSpaceMemberService).transferOwnership(1, "notmember@mail.com");

        mockMvc.perform(post("/v1/workspace/member/1/transfer-ownership")
                        .param("newOwnerEmail", "notmember@mail.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("New owner must be a member of the workspace"));
    }

    @Test
    void transferOwnership_shouldReturnNotFound_whenWorkspaceNotFound() throws Exception {
        doThrow(new WorkSpaceNotFoundException("WorkSpace not found"))
                .when(workSpaceMemberService).transferOwnership(999, "newowner@mail.com");

        mockMvc.perform(post("/v1/workspace/member/999/transfer-ownership")
                        .param("newOwnerEmail", "newowner@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WorkSpace not found"));
    }
    

    @Test
    void countMembers_shouldReturnMemberCount() throws Exception {
        when(workSpaceMemberService.countMembersInWorkSpace(1)).thenReturn(5);

        mockMvc.perform(get("/v1/workspace/member/1/count-members"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void countMembers_shouldReturnZero_whenNoMembers() throws Exception {
        when(workSpaceMemberService.countMembersInWorkSpace(1)).thenReturn(0);

        mockMvc.perform(get("/v1/workspace/member/1/count-members"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
