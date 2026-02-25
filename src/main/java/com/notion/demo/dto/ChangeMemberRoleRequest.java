package com.notion.demo.dto;

import com.notion.demo.enums.WorkSpaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeMemberRoleRequest {
    
    @NotNull(message = "Email cannot be null")
    @Email
    private String email;
    
    @NotNull(message = "Role cannot be null")
    private WorkSpaceRole role;
}
