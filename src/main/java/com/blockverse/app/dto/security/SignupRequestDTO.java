package com.blockverse.app.dto.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDTO {
    
    @NotNull(message = "Name cannot be null")
    private String name;
    
    @NotNull(message = "Email cannot be null")
    @Email
    private String email;
    
    @NotNull(message = "Password cannot be null")
    private String password;
}
