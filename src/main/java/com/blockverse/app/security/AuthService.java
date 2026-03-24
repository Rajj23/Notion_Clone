package com.blockverse.app.security;

import com.blockverse.app.dto.security.*;
import com.blockverse.app.exception.EmailAlreadyExistException;
import com.blockverse.app.exception.InvalidTokenException;
import com.blockverse.app.entity.User;
import com.blockverse.app.exception.UserNotFoundException;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRepo userRepo;
    private final RateLimiterService rateLimiterService;

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO){

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        rateLimiterService.checkRateLimit(user.getId(), "USER_LOGIN");

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        user.setRefreshToken(passwordEncoder.encode(hashTokenForBcrypt(refreshToken)));
        userRepo.save(user);

        return new LoginResponseDTO(accessToken, refreshToken);
    }

    public SignupResponseDTO signup(SignupRequestDTO signupRequestDTO){
        Optional<User> existingUser = userRepo.findByEmail(signupRequestDTO.getEmail());
        if(existingUser.isPresent()){
            throw new EmailAlreadyExistException("User find with same email!");
        }

        User user = userRepo.save(User.builder()
                .name(signupRequestDTO.getName())
                .email(signupRequestDTO.getEmail())
                .password(passwordEncoder.encode(signupRequestDTO.getPassword()))
                .build()
        );
        rateLimiterService.checkRateLimit(user.getId(), "USER_SIGNUP");

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        user.setRefreshToken(passwordEncoder.encode(hashTokenForBcrypt(refreshToken)));
        userRepo.save(user);

        return new SignupResponseDTO(accessToken, refreshToken);
    }

    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenResponseDTO) throws InvalidTokenException {
        String refreshToken = refreshTokenResponseDTO.getRefreshToken();

        if(!jwtUtil.validateToken(refreshToken)){
            throw new InvalidTokenException("Invalid refresh token");
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        
        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new UserNotFoundException("User not found with email: " + email));
        
        if(!passwordEncoder.matches(hashTokenForBcrypt(refreshToken), user.getRefreshToken())){
            throw new InvalidTokenException("Refresh token does not match");
        }
        
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        
        user.setRefreshToken(passwordEncoder.encode(hashTokenForBcrypt(newRefreshToken)));
        userRepo.save(user);
            
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken);
    }

    private String hashTokenForBcrypt(String token) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

}
