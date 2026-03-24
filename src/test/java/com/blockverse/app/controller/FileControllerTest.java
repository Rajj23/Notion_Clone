package com.blockverse.app.controller;

import com.blockverse.app.entity.User;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.security.AuthService;
import com.blockverse.app.security.JwtUtil;
import com.blockverse.app.security.SecurityUtil;
import com.blockverse.app.service.RateLimiterService;
import com.blockverse.app.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private SecurityUtil securityUtil;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private UserRepo userRepo;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    @Test
    void uploadFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image content".getBytes());
        String expectedS3Key = "some-uuid_test.jpg";
        
        User testUser = User.builder().id(1).email("test@example.com").build();
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        when(s3Service.uploadFile(any())).thenReturn(expectedS3Key);

        mockMvc.perform(multipart("/v1/files/upload")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedS3Key));
    }

    @Test
    void uploadFile_rateLimitExceeded() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image content".getBytes());
        User testUser = User.builder().id(1).email("test@example.com").build();
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        org.mockito.Mockito.doThrow(new com.blockverse.app.exception.TooManyRequestsException("Too many requests"))
                .when(rateLimiterService).checkRateLimit(1, "FILE_UPLOAD");

        mockMvc.perform(multipart("/v1/files/upload").file(file))
                .andExpect(status().isTooManyRequests());
    }
}
