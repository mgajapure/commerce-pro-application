package com.commerce_pro_backend.system.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce_pro_backend.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/security")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Security Administration", description = "Manage security settings, MFA, SSO, GDPR, PCI compliance, and encryption")
public class SecurityAdminController {

    private final ConcurrentHashMap<String, Object> securitySettings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> mfaPolicy = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> ssoConfig = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> gdprSettings = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> pciChecklist = new ArrayList<>();
    private final List<Map<String, Object>> encryptionKeys = new ArrayList<>();

    @PostConstruct
    public void init() {
        // Security settings defaults
        securitySettings.put("passwordPolicy", Map.of(
                "minLength", 12,
                "requireUppercase", true,
                "requireLowercase", true,
                "requireNumbers", true,
                "requireSpecialChars", true,
                "maxAgeDays", 90,
                "historyCount", 5
        ));
        securitySettings.put("sessionConfig", Map.of(
                "timeoutMinutes", 30,
                "maxConcurrentSessions", 3,
                "extendOnActivity", true
        ));
        securitySettings.put("loginAttemptsConfig", Map.of(
                "maxAttempts", 5,
                "lockoutDurationMinutes", 15,
                "resetAfterMinutes", 60
        ));
        securitySettings.put("ipWhitelist", List.of());

        // MFA policy defaults
        mfaPolicy.put("requireForAllAdmins", false);
        mfaPolicy.put("gracePeriodDays", 7);
        mfaPolicy.put("allowedMethods", List.of("TOTP", "SMS", "EMAIL"));
        mfaPolicy.put("rememberDeviceDays", 30);

        // SSO configuration defaults
        ssoConfig.put("enabled", false);
        ssoConfig.put("provider", "NONE");
        ssoConfig.put("clientId", "");
        ssoConfig.put("issuerUrl", "");
        ssoConfig.put("allowedDomains", List.of());
        ssoConfig.put("autoProvision", false);
        ssoConfig.put("defaultRole", "VIEWER");

        // GDPR settings defaults
        gdprSettings.put("dataRetentionDays", 365);
        gdprSettings.put("anonymizationEnabled", true);
        gdprSettings.put("anonymizeAfterDays", 730);
        gdprSettings.put("consentRequired", true);
        gdprSettings.put("consentVersion", "1.0");
        gdprSettings.put("cookieSettings", Map.of(
                "essential", true,
                "analytics", false,
                "marketing", false,
                "preferences", false,
                "bannerEnabled", true
        ));

        // PCI compliance checklist defaults
        pciChecklist.add(Map.of("id", "pci-1", "requirement", "Install and maintain a firewall", "status", "COMPLIANT", "lastChecked", Instant.now().toString()));
        pciChecklist.add(Map.of("id", "pci-2", "requirement", "Do not use vendor-supplied defaults", "status", "COMPLIANT", "lastChecked", Instant.now().toString()));
        pciChecklist.add(Map.of("id", "pci-3", "requirement", "Protect stored cardholder data", "status", "COMPLIANT", "lastChecked", Instant.now().toString()));
        pciChecklist.add(Map.of("id", "pci-4", "requirement", "Encrypt transmission of cardholder data", "status", "COMPLIANT", "lastChecked", Instant.now().toString()));
        pciChecklist.add(Map.of("id", "pci-5", "requirement", "Use and regularly update anti-virus software", "status", "REVIEW_NEEDED", "lastChecked", Instant.now().toString()));
        pciChecklist.add(Map.of("id", "pci-6", "requirement", "Develop and maintain secure systems", "status", "COMPLIANT", "lastChecked", Instant.now().toString()));

        // Encryption key metadata defaults
        encryptionKeys.add(Map.of(
                "id", "key-001",
                "algorithm", "AES-256-GCM",
                "status", "ACTIVE",
                "created", "2025-01-15T00:00:00Z",
                "expiry", "2026-01-15T00:00:00Z",
                "purpose", "DATA_ENCRYPTION"
        ));
        encryptionKeys.add(Map.of(
                "id", "key-002",
                "algorithm", "RSA-4096",
                "status", "ACTIVE",
                "created", "2025-03-01T00:00:00Z",
                "expiry", "2026-03-01T00:00:00Z",
                "purpose", "TOKEN_SIGNING"
        ));
    }

    // --- Security Settings ---

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('system:security:read')")
    @Operation(summary = "Get security settings")
    public ApiResponse<Map<String, Object>> getSecuritySettings() {
        return ApiResponse.success(new HashMap<>(securitySettings));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAuthority('system:security:manage')")
    @Operation(summary = "Update security settings")
    public ApiResponse<String> updateSecuritySettings(
            @RequestBody Map<String, Object> settings,
            @RequestHeader("X-Admin-Id") String adminId) {
        securitySettings.putAll(settings);
        return ApiResponse.success("Security settings updated successfully");
    }

    // --- MFA / 2FA Policy ---

    @GetMapping("/2fa/policy")
    @PreAuthorize("hasAuthority('system:security:read')")
    @Operation(summary = "Get MFA enforcement policy")
    public ApiResponse<Map<String, Object>> getMfaPolicy() {
        return ApiResponse.success(new HashMap<>(mfaPolicy));
    }

    @PutMapping("/2fa/policy")
    @PreAuthorize("hasAuthority('system:security:manage')")
    @Operation(summary = "Update MFA enforcement policy")
    public ApiResponse<String> updateMfaPolicy(
            @RequestBody Map<String, Object> policy,
            @RequestHeader("X-Admin-Id") String adminId) {
        mfaPolicy.putAll(policy);
        return ApiResponse.success("MFA policy updated successfully");
    }

    // --- SSO Configuration ---

    @GetMapping("/sso")
    @PreAuthorize("hasAuthority('system:security:read')")
    @Operation(summary = "Get SSO configuration")
    public ApiResponse<Map<String, Object>> getSsoConfig() {
        return ApiResponse.success(new HashMap<>(ssoConfig));
    }

    @PutMapping("/sso")
    @PreAuthorize("hasAuthority('system:security:manage')")
    @Operation(summary = "Update SSO configuration")
    public ApiResponse<String> updateSsoConfig(
            @RequestBody Map<String, Object> config,
            @RequestHeader("X-Admin-Id") String adminId) {
        ssoConfig.putAll(config);
        return ApiResponse.success("SSO configuration updated successfully");
    }

    // --- GDPR Settings ---

    @GetMapping("/gdpr")
    @PreAuthorize("hasAuthority('system:security:read')")
    @Operation(summary = "Get GDPR settings")
    public ApiResponse<Map<String, Object>> getGdprSettings() {
        return ApiResponse.success(new HashMap<>(gdprSettings));
    }

    @PutMapping("/gdpr")
    @PreAuthorize("hasAuthority('system:security:manage')")
    @Operation(summary = "Update GDPR settings")
    public ApiResponse<String> updateGdprSettings(
            @RequestBody Map<String, Object> settings,
            @RequestHeader("X-Admin-Id") String adminId) {
        gdprSettings.putAll(settings);
        return ApiResponse.success("GDPR settings updated successfully");
    }

    // --- PCI Compliance ---

    @GetMapping("/pci")
    @PreAuthorize("hasAuthority('system:security:read')")
    @Operation(summary = "Get PCI compliance status")
    public ApiResponse<List<Map<String, Object>>> getPciCompliance() {
        return ApiResponse.success(new ArrayList<>(pciChecklist));
    }

    @PutMapping("/pci")
    @PreAuthorize("hasAuthority('system:security:manage')")
    @Operation(summary = "Update PCI compliance checklist")
    public ApiResponse<String> updatePciCompliance(
            @RequestBody List<Map<String, Object>> checklist,
            @RequestHeader("X-Admin-Id") String adminId) {
        pciChecklist.clear();
        pciChecklist.addAll(checklist);
        return ApiResponse.success("PCI compliance checklist updated successfully");
    }

    // --- Encryption Key Metadata ---

    @GetMapping("/encryption")
    @PreAuthorize("hasAuthority('system:security:read')")
    @Operation(summary = "Get encryption key metadata")
    public ApiResponse<List<Map<String, Object>>> getEncryptionKeys() {
        return ApiResponse.success(new ArrayList<>(encryptionKeys));
    }

    @PostMapping("/encryption/rotate")
    @PreAuthorize("hasAuthority('system:security:manage')")
    @Operation(summary = "Rotate encryption key")
    public ApiResponse<Map<String, Object>> rotateEncryptionKey(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Admin-Id") String adminId) {
        String purpose = (String) request.getOrDefault("purpose", "DATA_ENCRYPTION");
        String algorithm = (String) request.getOrDefault("algorithm", "AES-256-GCM");

        // Mark existing keys for the same purpose as ROTATED
        encryptionKeys.stream()
                .filter(k -> purpose.equals(k.get("purpose")) && "ACTIVE".equals(k.get("status")))
                .forEach(k -> {
                    Map<String, Object> updated = new HashMap<>(k);
                    updated.put("status", "ROTATED");
                    encryptionKeys.set(encryptionKeys.indexOf(k), updated);
                });

        // Create new key metadata
        Map<String, Object> newKey = new HashMap<>();
        newKey.put("id", "key-" + UUID.randomUUID().toString().substring(0, 8));
        newKey.put("algorithm", algorithm);
        newKey.put("status", "ACTIVE");
        newKey.put("created", Instant.now().toString());
        newKey.put("expiry", Instant.now().plusSeconds(365L * 24 * 60 * 60).toString());
        newKey.put("purpose", purpose);
        encryptionKeys.add(newKey);

        return ApiResponse.success("Encryption key rotated successfully", newKey);
    }
}
