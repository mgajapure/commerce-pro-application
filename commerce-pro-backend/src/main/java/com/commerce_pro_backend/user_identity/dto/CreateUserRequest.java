package com.commerce_pro_backend.user_identity.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank @Email
    private String email;
    
    @NotBlank @Size(min = 8)
    private String password;
    
    private String firstName;
    private String lastName;
    private String phone;
    private boolean sendWelcomeEmail = true;
    private List<String> initialRoleCodes;
}
