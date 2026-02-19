package com.notion.demo.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SignupRequestDTO {
    private String name;
    private String email;
    private String password;
}
