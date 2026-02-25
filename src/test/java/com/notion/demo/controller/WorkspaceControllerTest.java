package com.notion.demo.controller;

import com.notion.demo.dto.OwnerInfo;
import com.notion.demo.dto.WorkSpaceDetailsResponse;
import com.notion.demo.enums.WorkSpaceRole;
import com.notion.demo.enums.WorkSpaceType;
import com.notion.demo.exception.InsufficientPermissionException;
import com.notion.demo.repo.UserRepo;
import com.notion.demo.security.AuthService;
import com.notion.demo.security.JwtUtil;
import com.notion.demo.service.WorkSpaceService;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkSpaceController.class)
@AutoConfigureMockMvc(addFilters = false)
public class WorkspaceControllerTest {

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
    private WorkSpaceService workSpaceService;
    
    @Test
    void createWorkspace_shouldReturnSuccessMessage() throws Exception{
        
        when(workSpaceService.createWorkSpace(any())).thenReturn("Workspace created successfully");

        mockMvc.perform(post("/v1/workspaces/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "name": "My Workspace",
                          "workSpaceType": "PRIVATE"
                        }
                    """))
                .andExpect(status().isOk())
                .andExpect(content().string("Workspace created successfully"));
    }
    
    @Test
    void createWorkspace_shouldReturnBadRequestForInvalidInput() throws Exception {
        mockMvc.perform(post("/v1/workspaces/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "name": "",
                          "workSpaceType": "PRIVATE"
                        }
                    """))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void deleteWorkspace_shouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(delete("/v1/workspaces/delete/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("WorkSpace deleted successfully"));
    }
    
    @Test
    void deleteWorkspace_shouldReturnExceptionForNonOwner() throws Exception {
        doThrow(new InsufficientPermissionException("Only owner can delete the workspace"))
                .when(workSpaceService).deleteWorkSpace(1);
        
        mockMvc.perform(delete("/v1/workspaces/delete/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only owner can delete the workspace"));
    }
    
    @Test
    void getAllWorkSpacesForUser_shouldReturnListOfWorkSpaces() throws Exception {
        OwnerInfo owner1 = new OwnerInfo();
        owner1.setId(2);
        owner1.setName("Owner 2");
        
        List<WorkSpaceDetailsResponse> mockList = List.of(
                new WorkSpaceDetailsResponse(1, "Workspace 1", WorkSpaceType.PRIVATE, owner1, 5, WorkSpaceRole.OWNER),
                new WorkSpaceDetailsResponse(2, "Workspace 2", WorkSpaceType.TEAM, owner1, 3, WorkSpaceRole.ADMIN)
        );
        
        when(workSpaceService.getAllWorkSpacesForUser()).thenReturn(mockList);
    }
    
    @Test
    void getWorkSpaceDetails_shouldReturnWorkSpaceDetails() throws Exception {
        OwnerInfo owner = new OwnerInfo();
        owner.setId(2);
        owner.setName("Owner 2");
        
        WorkSpaceDetailsResponse mockResponse = new WorkSpaceDetailsResponse(1, "Workspace 1", WorkSpaceType.PRIVATE, owner, 5, WorkSpaceRole.OWNER);
        
        when(workSpaceService.getWorkSpaceDetails(1)).thenReturn(mockResponse);
    }
    
    @Test
    void updateWorkSpace_shouldReturnSuccessMessage() throws Exception {
            mockMvc.perform(put("/v1/workspaces/update/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                        {  
                                          "name": "Updated Workspace Name",
                                          "workSpaceType": "TEAM"
                                        }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(content().string("WorkSpace updated successfully"));
        }
}
