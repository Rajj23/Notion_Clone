package com.blockverse.app.controller;

import com.blockverse.app.dto.NotificationResponse;
import com.blockverse.app.notification.NotificationService;
import com.blockverse.app.security.JwtUtil;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.security.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserRepo userRepo;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private AuthService authService;

    @Test
    @WithMockUser
    void getUserNotifications_ShouldReturnList() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1).message("Test Notice").build();
        
        when(notificationService.getUserNotifications()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("Test Notice"));
    }

    @Test
    @WithMockUser
    void getUnreadNotifications_ShouldReturnList() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(2).message("Unread Notice").read(false).build();
        
        when(notificationService.getUnreadUserNotifications()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    @WithMockUser
    void markAsRead_ShouldReturnOk() throws Exception {
        doNothing().when(notificationService).markAsRead(1);

        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(1);
    }

    @Test
    @WithMockUser
    void markAllAsRead_ShouldReturnOk() throws Exception {
        doNothing().when(notificationService).markAllAsRead();

        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead();
    }
}
