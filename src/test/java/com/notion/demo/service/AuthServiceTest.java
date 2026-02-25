package com.notion.demo.service;

import com.notion.demo.dto.*;
import com.notion.demo.entity.User;
import com.notion.demo.exception.InvalidTokenException;
import com.notion.demo.repo.UserRepo;
import com.notion.demo.security.AuthService;
import com.notion.demo.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private UserRepo userRepo;
    
    @InjectMocks
    private AuthService authService;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Test
    void signup_shouldThrowException_whenEmailAlreadyExists() {
        // Given
        SignupRequestDTO requestDTO = new SignupRequestDTO("Aspen", "aspen@gmail.com", "12345678a");

        when(userRepo.findByEmail("aspen@gmail.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(RuntimeException.class, () -> authService.signup(requestDTO));
    }
    
    
    @Test
    void signup_shouldReturnTokens_whenSignupSuccessful() {
        // Given
        SignupRequestDTO requestDTO = new SignupRequestDTO("Aspen", "aspen@gmail.com", "12345678a");

        when(userRepo.findByEmail("aspen@gmail.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("12345678a"))
                .thenReturn("encodedPassword");

        User savedUser = User.builder()
                .name("Aspen")
                .email("aspen@gmail.com")
                .password("encodedPassword")
                .build();

        when(userRepo.save(any(User.class)))
                .thenReturn(savedUser);

        when(jwtUtil.generateAccessToken(savedUser))
                .thenReturn("accessToken");

        when(jwtUtil.generateRefreshToken(savedUser))
                .thenReturn("refreshToken");

        SignupResponseDTO response = authService.signup(requestDTO);

        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
    }
    
    @Test
    void login_shouldReturnTokens_whenCredentialsValid() {
        // Given
        LoginRequestDTO requestDTO = new LoginRequestDTO("aspen@gmail.com", "12345678a");

        User user = User.builder()
                .email("aspen.gmail.com")
                .password("encodedPassword")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);

        when(jwtUtil.generateAccessToken(user))
                .thenReturn("accessToken");

        when(jwtUtil.generateRefreshToken(user))
                .thenReturn("refreshToken");

        LoginResponseDTO responseDTO = authService.login(requestDTO);

        assertEquals("accessToken", responseDTO.getAccessToken());
        assertEquals("refreshToken", responseDTO.getRefreshToken());

        verify(userRepo).save(user);
    }
    
    @Test
    void refreshToken_shouldThrowException_whenTokenInvalid() {

        RefreshTokenRequestDTO requestDTO = new RefreshTokenRequestDTO("invalidToken");

        when(jwtUtil.validateToken("invalidToken"))
                .thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(requestDTO));
    }
    
    @Test
    void refreshToken_shouldReturnNewTokens_whenTokenValid() throws InvalidTokenException {
        String oldToken = "oldRefreshToken";
        
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(oldToken);
        
        User user = User.builder()
                .email("aspen@gmail.com")
                .refreshToken("encodedOldToken")
                .build();
        
        when(jwtUtil.validateToken(oldToken)).thenReturn(true);
        when(jwtUtil.getEmailFromToken(oldToken)).thenReturn("aspen@gmail.com");
        when(userRepo.findByEmail("aspen@gmail.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldToken, "encodedOldToken")).thenReturn(true);
        
        when(jwtUtil.generateAccessToken(user)).thenReturn("newAccessToken");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("newRefreshToken");
        
        when(passwordEncoder.encode("newRefreshToken"))
                .thenReturn("encodedNewRefreshToken");
        
        RefreshTokenResponseDTO response = authService.refreshToken(request);
        
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
        
        verify(userRepo).save(user);
    }
}
    