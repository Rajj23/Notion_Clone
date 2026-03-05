package com.blockverse.app.dto.security;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequestDTO {
    
    @NotNull(message = "Refresh token cannot be null")
    private String refreshToken;
}
