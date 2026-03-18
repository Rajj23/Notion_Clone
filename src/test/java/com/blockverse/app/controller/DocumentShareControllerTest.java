package com.blockverse.app.controller;

import com.blockverse.app.dto.document.DocumentDetailsResponse;
import com.blockverse.app.dto.document.DocumentResponse;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.security.AuthService;
import com.blockverse.app.security.JwtUtil;
import com.blockverse.app.service.DocumentShareService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentShareController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentShareService documentShareService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserRepo userRepo;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("should return document details for valid token")
    void getSharedDocument_success() throws Exception {
        DocumentResponse docResp = DocumentResponse.builder().id(1).title("Shared Doc").build();
        DocumentDetailsResponse detailsResp = DocumentDetailsResponse.builder().document(docResp).blocks(List.of()).build();

        when(documentShareService.getSharedDocument("valid_token")).thenReturn(detailsResp);

        mockMvc.perform(get("/share/valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.title").value("Shared Doc"));
    }

    @Test
    @DisplayName("should return 400 when token is invalid or expired")
    void getSharedDocument_invalidToken() throws Exception {
        when(documentShareService.getSharedDocument("invalid_token"))
                .thenThrow(new IllegalArgumentException("Invalid or expired share token"));

        mockMvc.perform(get("/share/invalid_token"))
                .andExpect(status().isBadRequest());
    }
}
