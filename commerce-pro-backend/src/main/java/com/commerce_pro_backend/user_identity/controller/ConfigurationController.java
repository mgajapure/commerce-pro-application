package com.commerce_pro_backend.user_identity.controller;

import java.util.Map;

import com.commerce_pro_backend.user_identity.entity.User;
import com.commerce_pro_backend.user_identity.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.user_identity.config.SuperAdminConfig;
import com.commerce_pro_backend.user_identity.service.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/identity/config")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Super Admin Configuration", description = "Manage identity module configuration")
public class ConfigurationController {

    private final SuperAdminConfig superAdminConfig;
    private final AuditService auditService;
    private final UserRepository userRepository;

    private String resolveUsername(String userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getUsername).orElse(userId);
    }

    @GetMapping("/superadmin")
    @PreAuthorize("hasAuthority('identity:config:read')")
    @Operation(summary = "Get Super Admin configuration")
    public ApiResponse<SuperAdminConfig> getSuperAdminConfig() {
        return ApiResponse.success(superAdminConfig);
    }

    @PutMapping("/superadmin/security-policy")
    @PreAuthorize("hasAuthority('identity:config:security-policy')")
    @Operation(summary = "Update security policy configuration")
    public ApiResponse<String> updateSecurityPolicy(
            @Valid @RequestBody SuperAdminConfig.SecurityPolicy policy,
            @RequestHeader("X-Admin-Id") String adminId) {
        
        // Apply configuration changes
        superAdminConfig.getSecurityPolicy().setRequireMfa(policy.isRequireMfa());
        superAdminConfig.getSecurityPolicy().setSessionTimeoutMinutes(policy.getSessionTimeoutMinutes());
        superAdminConfig.getSecurityPolicy().setMaxConcurrentSessions(policy.getMaxConcurrentSessions());
        superAdminConfig.getSecurityPolicy().setAllowedIpRanges(policy.getAllowedIpRanges());
        
        //auditService.logConfigChange(adminId, "SECURITY_POLICY", policy);
        auditService.logConfigChange(adminId, resolveUsername(adminId), "SECURITY_POLICY", policy);
        
        return ApiResponse.success("Security policy updated successfully");
    }

    @PutMapping("/superadmin/configuration-management")
    @PreAuthorize("hasAuthority('identity:config:update')")
    @Operation(summary = "Update configuration management settings")
    public ApiResponse<String> updateConfigManagement(
            @Valid @RequestBody SuperAdminConfig.ConfigurationManagement config,
            @RequestHeader("X-Admin-Id") String adminId) {
        
        superAdminConfig.getConfigManagement().setCanDeleteSystemRoles(config.isCanDeleteSystemRoles());
        superAdminConfig.getConfigManagement().setCanModifyOwnRole(config.isCanModifyOwnRole());
        superAdminConfig.getConfigManagement().setMinSuperAdmins(config.getMinSuperAdmins());
        
//        auditService.logConfigChange(adminId, "CONFIG_MANAGEMENT", config);
        auditService.logConfigChange(adminId, resolveUsername(adminId), "CONFIG_MANAGEMENT", config);
        return ApiResponse.success("Configuration management settings updated");
    }

    @GetMapping("/system-settings")
    @PreAuthorize("hasAuthority('identity:config:read')")
    @Operation(summary = "Get all system settings")
    public ApiResponse<Map<String, Object>> getSystemSettings() {
        return ApiResponse.success(superAdminConfig.getConfigManagement().getDefaultSettings());
    }

    @PutMapping("/system-settings")
    @PreAuthorize("hasAuthority('identity:config:update')")
    @Operation(summary = "Update system settings")
    public ApiResponse<String> updateSystemSettings(
            @RequestBody Map<String, Object> settings,
            @RequestHeader("X-Admin-Id") String adminId) {
        
        superAdminConfig.getConfigManagement().getDefaultSettings().putAll(settings);
//        auditService.logConfigChange(adminId, "SYSTEM_SETTINGS", settings);
        auditService.logConfigChange(adminId, resolveUsername(adminId), "SYSTEM_SETTINGS", settings);
        return ApiResponse.success("System settings updated");
    }

    @PostMapping("/reload")
    @PreAuthorize("hasAuthority('identity:config:update')")
    @Operation(summary = "Reload configuration from source")
    public ApiResponse<String> reloadConfiguration(@RequestHeader("X-Admin-Id") String adminId) {
        if (!superAdminConfig.getConfigManagement().isAllowRemoteConfigReload()) {
            return ApiResponse.error("Remote configuration reload is disabled");
        }
        
//        // Trigger configuration reload
//        auditService.logConfigChange(adminId, "CONFIG_RELOAD", Map.of("timestamp", System.currentTimeMillis()));
        // Trigger configuration reload
        auditService.logConfigChange(adminId, resolveUsername(adminId), "CONFIG_RELOAD", Map.of("timestamp", System.currentTimeMillis()));
        
        return ApiResponse.success("Configuration reloaded successfully");
    }
}