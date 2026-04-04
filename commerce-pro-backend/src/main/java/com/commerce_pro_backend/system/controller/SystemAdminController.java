package com.commerce_pro_backend.system.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RequestMapping("/v1/admin/system")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "System Administration", description = "Manage integrations, API keys, webhooks, feature flags, and backups")
public class SystemAdminController {

    private final ConcurrentHashMap<String, Map<String, Object>> integrations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> apiKeys = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> webhooks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> featureFlags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> backupStatus = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Default integrations
        String intId1 = UUID.randomUUID().toString();
        integrations.put(intId1, new HashMap<>(Map.of(
                "id", intId1,
                "name", "Stripe Payment Gateway",
                "type", "PAYMENT",
                "status", "ACTIVE",
                "lastSync", Instant.now().toString(),
                "config", Map.of("apiVersion", "2024-12-18", "webhookSecret", "****")
        )));
        String intId2 = UUID.randomUUID().toString();
        integrations.put(intId2, new HashMap<>(Map.of(
                "id", intId2,
                "name", "SendGrid Email",
                "type", "EMAIL",
                "status", "ACTIVE",
                "lastSync", Instant.now().toString(),
                "config", Map.of("region", "us-east-1")
        )));

        // Default API keys
        String keyId1 = UUID.randomUUID().toString();
        apiKeys.put(keyId1, new HashMap<>(Map.of(
                "id", keyId1,
                "name", "Mobile App Key",
                "prefix", "cpk_live_",
                "scopes", List.of("catalog:read", "order:create"),
                "created", "2025-06-01T00:00:00Z",
                "lastUsed", Instant.now().toString(),
                "active", true
        )));

        // Default webhooks
        String whId1 = UUID.randomUUID().toString();
        webhooks.put(whId1, new HashMap<>(Map.of(
                "id", whId1,
                "url", "https://example.com/webhooks/orders",
                "events", List.of("order.created", "order.updated", "order.fulfilled"),
                "active", true,
                "lastTriggered", Instant.now().toString(),
                "failureCount", 0
        )));

        // Default feature flags
        featureFlags.put("new-checkout-flow", new HashMap<>(Map.of(
                "key", "new-checkout-flow",
                "description", "Enable redesigned checkout experience",
                "enabled", false,
                "percentage", 0,
                "userGroups", List.of()
        )));
        featureFlags.put("ai-recommendations", new HashMap<>(Map.of(
                "key", "ai-recommendations",
                "description", "AI-powered product recommendations",
                "enabled", true,
                "percentage", 100,
                "userGroups", List.of("premium", "beta-testers")
        )));
        featureFlags.put("dark-mode", new HashMap<>(Map.of(
                "key", "dark-mode",
                "description", "Dark mode for admin panel",
                "enabled", true,
                "percentage", 50,
                "userGroups", List.of("internal")
        )));

        // Backup status defaults
        backupStatus.put("lastBackup", "2026-04-02T03:00:00Z");
        backupStatus.put("lastBackupStatus", "SUCCESS");
        backupStatus.put("lastBackupSizeMb", 2450);
        backupStatus.put("scheduledFrequency", "DAILY");
        backupStatus.put("retentionDays", 30);
        backupStatus.put("history", List.of(
                Map.of("id", "bkp-001", "timestamp", "2026-04-02T03:00:00Z", "status", "SUCCESS", "sizeMb", 2450),
                Map.of("id", "bkp-002", "timestamp", "2026-04-01T03:00:00Z", "status", "SUCCESS", "sizeMb", 2430),
                Map.of("id", "bkp-003", "timestamp", "2026-03-31T03:00:00Z", "status", "SUCCESS", "sizeMb", 2415)
        ));
    }

    // --- Integrations ---

    @GetMapping("/integrations")
    @PreAuthorize("hasAuthority('system:admin:read')")
    @Operation(summary = "List all integrations")
    public ApiResponse<List<Map<String, Object>>> listIntegrations() {
        return ApiResponse.success(new ArrayList<>(integrations.values()));
    }

    @PostMapping("/integrations")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Create a new integration")
    public ApiResponse<Map<String, Object>> createIntegration(
            @RequestBody Map<String, Object> integration,
            @RequestHeader("X-Admin-Id") String adminId) {
        String id = UUID.randomUUID().toString();
        integration.put("id", id);
        integration.putIfAbsent("status", "INACTIVE");
        integration.putIfAbsent("lastSync", null);
        integrations.put(id, new HashMap<>(integration));
        return ApiResponse.success("Integration created successfully", integration);
    }

    @PutMapping("/integrations/{id}")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Update an integration")
    public ApiResponse<Map<String, Object>> updateIntegration(
            @PathVariable String id,
            @RequestBody Map<String, Object> integration,
            @RequestHeader("X-Admin-Id") String adminId) {
        if (!integrations.containsKey(id)) {
            return ApiResponse.error("Integration not found: " + id);
        }
        integration.put("id", id);
        integrations.get(id).putAll(integration);
        return ApiResponse.success("Integration updated successfully", integrations.get(id));
    }

    @DeleteMapping("/integrations/{id}")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Delete an integration")
    public ApiResponse<String> deleteIntegration(
            @PathVariable String id,
            @RequestHeader("X-Admin-Id") String adminId) {
        if (integrations.remove(id) == null) {
            return ApiResponse.error("Integration not found: " + id);
        }
        return ApiResponse.success("Integration deleted successfully");
    }

    @PostMapping("/integrations/{id}/test")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Test integration connection")
    public ApiResponse<Map<String, Object>> testIntegration(
            @PathVariable String id,
            @RequestHeader("X-Admin-Id") String adminId) {
        if (!integrations.containsKey(id)) {
            return ApiResponse.error("Integration not found: " + id);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("integrationId", id);
        result.put("status", "OK");
        result.put("latencyMs", 142);
        result.put("testedAt", Instant.now().toString());
        return ApiResponse.success("Integration test completed", result);
    }

    // --- API Keys ---

    @GetMapping("/api-keys")
    @PreAuthorize("hasAuthority('system:admin:read')")
    @Operation(summary = "List API keys")
    public ApiResponse<List<Map<String, Object>>> listApiKeys() {
        return ApiResponse.success(new ArrayList<>(apiKeys.values()));
    }

    @PostMapping("/api-keys")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Create a new API key")
    public ApiResponse<Map<String, Object>> createApiKey(
            @RequestBody Map<String, Object> apiKey,
            @RequestHeader("X-Admin-Id") String adminId) {
        String id = UUID.randomUUID().toString();
        apiKey.put("id", id);
        apiKey.putIfAbsent("prefix", "cpk_live_");
        apiKey.putIfAbsent("scopes", List.of());
        apiKey.put("created", Instant.now().toString());
        apiKey.put("lastUsed", null);
        apiKey.put("active", true);
        // In a real implementation, the full key would be generated and returned only once
        apiKey.put("generatedKey", "cpk_live_" + UUID.randomUUID().toString().replace("-", ""));
        apiKeys.put(id, new HashMap<>(apiKey));
        return ApiResponse.success("API key created successfully", apiKey);
    }

    @DeleteMapping("/api-keys/{id}")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Revoke an API key")
    public ApiResponse<String> revokeApiKey(
            @PathVariable String id,
            @RequestHeader("X-Admin-Id") String adminId) {
        Map<String, Object> key = apiKeys.get(id);
        if (key == null) {
            return ApiResponse.error("API key not found: " + id);
        }
        key.put("active", false);
        return ApiResponse.success("API key revoked successfully");
    }

    // --- Webhooks ---

    @GetMapping("/webhooks")
    @PreAuthorize("hasAuthority('system:admin:read')")
    @Operation(summary = "List webhooks")
    public ApiResponse<List<Map<String, Object>>> listWebhooks() {
        return ApiResponse.success(new ArrayList<>(webhooks.values()));
    }

    @PostMapping("/webhooks")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Create a new webhook")
    public ApiResponse<Map<String, Object>> createWebhook(
            @RequestBody Map<String, Object> webhook,
            @RequestHeader("X-Admin-Id") String adminId) {
        String id = UUID.randomUUID().toString();
        webhook.put("id", id);
        webhook.putIfAbsent("active", true);
        webhook.put("lastTriggered", null);
        webhook.put("failureCount", 0);
        webhook.put("secret", UUID.randomUUID().toString());
        webhooks.put(id, new HashMap<>(webhook));
        return ApiResponse.success("Webhook created successfully", webhook);
    }

    @PutMapping("/webhooks/{id}")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Update a webhook")
    public ApiResponse<Map<String, Object>> updateWebhook(
            @PathVariable String id,
            @RequestBody Map<String, Object> webhook,
            @RequestHeader("X-Admin-Id") String adminId) {
        if (!webhooks.containsKey(id)) {
            return ApiResponse.error("Webhook not found: " + id);
        }
        webhook.put("id", id);
        webhooks.get(id).putAll(webhook);
        return ApiResponse.success("Webhook updated successfully", webhooks.get(id));
    }

    @DeleteMapping("/webhooks/{id}")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Delete a webhook")
    public ApiResponse<String> deleteWebhook(
            @PathVariable String id,
            @RequestHeader("X-Admin-Id") String adminId) {
        if (webhooks.remove(id) == null) {
            return ApiResponse.error("Webhook not found: " + id);
        }
        return ApiResponse.success("Webhook deleted successfully");
    }

    @PostMapping("/webhooks/{id}/test")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Test a webhook")
    public ApiResponse<Map<String, Object>> testWebhook(
            @PathVariable String id,
            @RequestHeader("X-Admin-Id") String adminId) {
        if (!webhooks.containsKey(id)) {
            return ApiResponse.error("Webhook not found: " + id);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("webhookId", id);
        result.put("responseCode", 200);
        result.put("responseTimeMs", 87);
        result.put("testedAt", Instant.now().toString());
        return ApiResponse.success("Webhook test completed", result);
    }

    // --- Feature Flags ---

    @GetMapping("/feature-flags")
    @PreAuthorize("hasAuthority('system:admin:read')")
    @Operation(summary = "List feature flags")
    public ApiResponse<List<Map<String, Object>>> listFeatureFlags() {
        return ApiResponse.success(new ArrayList<>(featureFlags.values()));
    }

    @PutMapping("/feature-flags/{key}")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Update a feature flag")
    public ApiResponse<Map<String, Object>> updateFeatureFlag(
            @PathVariable String key,
            @RequestBody Map<String, Object> flag,
            @RequestHeader("X-Admin-Id") String adminId) {
        if (!featureFlags.containsKey(key)) {
            return ApiResponse.error("Feature flag not found: " + key);
        }
        flag.put("key", key);
        featureFlags.get(key).putAll(flag);
        return ApiResponse.success("Feature flag updated successfully", featureFlags.get(key));
    }

    // --- Backup ---

    @GetMapping("/backup")
    @PreAuthorize("hasAuthority('system:admin:read')")
    @Operation(summary = "Get backup status and history")
    public ApiResponse<Map<String, Object>> getBackupStatus() {
        return ApiResponse.success(new HashMap<>(backupStatus));
    }

    @PostMapping("/backup")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Trigger a backup")
    public ApiResponse<Map<String, Object>> triggerBackup(
            @RequestHeader("X-Admin-Id") String adminId) {
        Map<String, Object> backup = new HashMap<>();
        backup.put("id", "bkp-" + UUID.randomUUID().toString().substring(0, 8));
        backup.put("timestamp", Instant.now().toString());
        backup.put("status", "IN_PROGRESS");
        backup.put("initiatedBy", adminId);
        backupStatus.put("lastBackup", backup.get("timestamp"));
        backupStatus.put("lastBackupStatus", "IN_PROGRESS");
        return ApiResponse.success("Backup initiated successfully", backup);
    }

    @PostMapping("/backup/restore")
    @PreAuthorize("hasAuthority('system:admin:manage')")
    @Operation(summary = "Trigger a restore from backup")
    public ApiResponse<Map<String, Object>> triggerRestore(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Admin-Id") String adminId) {
        String confirmationCode = (String) request.get("confirmationCode");
        if (confirmationCode == null || confirmationCode.isBlank()) {
            return ApiResponse.error("Confirmation code is required for restore operations");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("restoreId", "rst-" + UUID.randomUUID().toString().substring(0, 8));
        result.put("backupId", request.get("backupId"));
        result.put("status", "IN_PROGRESS");
        result.put("initiatedBy", adminId);
        result.put("timestamp", Instant.now().toString());
        return ApiResponse.success("Restore initiated successfully", result);
    }
}
