package com.notion.demo.controller;

import com.notion.demo.dto.LoginRequestDTO;
import com.notion.demo.dto.LoginResponseDTO;
import com.notion.demo.dto.SignupRequestDTO;
import com.notion.demo.dto.SignupResponseDTO;
import com.notion.demo.repo.UserRepo;
import com.notion.demo.security.AuthService;
import com.notion.demo.security.JwtUtil;
import com.notion.demo.security.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
public class AuthControllerTest {
    
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
    
    @Test
    void login_shouldReturnTokens_whenValidRequest() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("aspen@gmail.com", "12345678a");
        LoginResponseDTO response = new LoginResponseDTO("accessToken", "refreshToken");
        
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf()) 
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                "email": "aspen@gmail.com",
                "password": "12345678a"
                }"""))
                .andExpect(status().isOk());
    }
    
    @Test
    void signup_shouldReturnTokens_whenValidRequest() throws Exception{
        SignupResponseDTO response = new SignupResponseDTO("accessToken", "refreshToken");
        
        when(authService.signup(any(SignupRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "name": "aspen",
                  "email": "aspen@gmail.com",
                  "password": "1234"
                }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"));
    }
    
    @Test
    void login_shouldReturnBadRequest_whenEmailMissing() throws Exception{
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                "password": "12345678a"
                }"""))
                .andExpect(status().isBadRequest());
    }
    
}
