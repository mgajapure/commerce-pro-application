package com.commerce_pro_backend.user_identity.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentRequest {
    @NotBlank
    private String roleCode;
    private String validFrom;  // ISO date
    private String validUntil;
    private String scopeContext;  // JSON
    private List<String> constraints;
}
