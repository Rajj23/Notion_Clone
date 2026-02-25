package com.notion.demo.security;

import com.notion.demo.dto.*;
import com.notion.demo.exception.EmailAlreadyExistException;
import com.notion.demo.exception.InvalidTokenException;
import com.notion.demo.entity.User;
import com.notion.demo.exception.UserNotFoundException;
import com.notion.demo.repo.UserRepo;
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

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO){

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);
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

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

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
        
        if(!passwordEncoder.matches(refreshToken, user.getRefreshToken())){
            throw new InvalidTokenException("Refresh token does not match");
        }
        
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        
        user.setRefreshToken(passwordEncoder.encode(newRefreshToken));
        userRepo.save(user);
            
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken);
    }

}
