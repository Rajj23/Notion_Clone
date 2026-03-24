package com.blockverse.app.service;

import com.blockverse.app.dto.security.*;
import com.blockverse.app.entity.User;
import com.blockverse.app.exception.InvalidTokenException;
import com.blockverse.app.exception.TooManyRequestsException;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.security.AuthService;
import com.blockverse.app.security.JwtUtil;
import com.blockverse.app.service.RateLimiterService;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private UserRepo userRepo;

    @Mock
    private RateLimiterService rateLimiterService;
    
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

        when(passwordEncoder.encode(any()))
                .thenReturn("encodedRefreshToken");

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

        when(passwordEncoder.encode(any()))
                .thenReturn("encodedRefreshToken");

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
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        
        when(jwtUtil.generateAccessToken(user)).thenReturn("newAccessToken");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("newRefreshToken");
        
        when(passwordEncoder.encode(any()))
                .thenReturn("encodedNewRefreshToken");
        
        RefreshTokenResponseDTO response = authService.refreshToken(request);
        
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
        
        verify(userRepo).save(user);
    }

    // ========================================================================
    // Rate Limiting
    // ========================================================================

    @Nested
    class RateLimitingTests {

        @Test
        void login_rateLimitExceeded_shouldThrowTooManyRequestsException() {
            LoginRequestDTO requestDTO = new LoginRequestDTO("aspen@gmail.com", "12345678a");
            User user = User.builder().id(1).email("aspen@gmail.com").password("encodedPassword").build();
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);

            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            doThrow(new TooManyRequestsException("Too many requests"))
                    .when(rateLimiterService).checkRateLimit(user.getId(), "USER_LOGIN");

            assertThrows(TooManyRequestsException.class, () -> authService.login(requestDTO));
            verify(userRepo, never()).save(any());
        }

        @Test
        void signup_rateLimitExceeded_shouldThrowTooManyRequestsException() {
            SignupRequestDTO requestDTO = new SignupRequestDTO("Aspen", "aspen@gmail.com", "12345678a");
            User savedUser = User.builder().id(1).name("Aspen").email("aspen@gmail.com").build();

            when(userRepo.findByEmail("aspen@gmail.com")).thenReturn(Optional.empty());
            when(userRepo.save(any(User.class))).thenReturn(savedUser);
            when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
            doThrow(new TooManyRequestsException("Too many requests"))
                    .when(rateLimiterService).checkRateLimit(savedUser.getId(), "USER_SIGNUP");

            assertThrows(TooManyRequestsException.class, () -> authService.signup(requestDTO));
        }
    }
}