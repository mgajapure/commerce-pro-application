package com.commerce_pro_backend.user_identity.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.commerce_pro_backend.user_identity.enums.PermissionCategory;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Getter
public class PermissionRegistry {

    private final Map<String, PermissionDefinition> systemPermissions = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // Identity Management Permissions
        register("identity:user:create", "Create User", PermissionCategory.IDENTITY_MANAGEMENT, 2);
        register("identity:user:read", "View User Details", PermissionCategory.IDENTITY_MANAGEMENT, 1);
        register("identity:user:update", "Update User", PermissionCategory.IDENTITY_MANAGEMENT, 2);
        register("identity:user:delete", "Delete User", PermissionCategory.IDENTITY_MANAGEMENT, 5, true);
        register("identity:user:activate", "Activate/Deactivate User", PermissionCategory.IDENTITY_MANAGEMENT, 3);
        register("identity:user:reset-password", "Reset User Password", PermissionCategory.IDENTITY_MANAGEMENT, 3);
        register("identity:user:manage-roles", "Assign/Revoke User Roles", PermissionCategory.IDENTITY_MANAGEMENT, 4);
        register("identity:user:view-audit", "View User Audit History", PermissionCategory.AUDIT_AND_COMPLIANCE, 2);
        register("identity:user:impersonate", "Impersonate User", PermissionCategory.SECURITY_OPERATIONS, 5, true);

        // Role Management Permissions
        register("identity:role:create", "Create Role", PermissionCategory.IDENTITY_MANAGEMENT, 3);
        register("identity:role:read", "View Roles", PermissionCategory.IDENTITY_MANAGEMENT, 1);
        register("identity:role:update", "Update Role", PermissionCategory.IDENTITY_MANAGEMENT, 3);
        register("identity:role:delete", "Delete Role", PermissionCategory.IDENTITY_MANAGEMENT, 4, true);
        register("identity:role:manage-permissions", "Modify Role Permissions", PermissionCategory.IDENTITY_MANAGEMENT, 4);
        register("identity:role:manage-hierarchy", "Modify Role Hierarchy", PermissionCategory.IDENTITY_MANAGEMENT, 4);

        // Permission Management Permissions
        register("identity:permission:create", "Create Custom Permission", PermissionCategory.IDENTITY_MANAGEMENT, 3);
        register("identity:permission:read", "View Permissions", PermissionCategory.IDENTITY_MANAGEMENT, 1);
        register("identity:permission:update", "Update Permission", PermissionCategory.IDENTITY_MANAGEMENT, 3);
        register("identity:permission:delete", "Delete Permission", PermissionCategory.IDENTITY_MANAGEMENT, 4);
        register("identity:permission:system-modify", "Modify System Permissions", PermissionCategory.SYSTEM_CONFIGURATION, 5, true);

        // System Configuration Permissions
        register("identity:config:read", "View System Configuration", PermissionCategory.SYSTEM_CONFIGURATION, 1);
        register("identity:config:update", "Update System Configuration", PermissionCategory.SYSTEM_CONFIGURATION, 4);
        register("identity:config:security-policy", "Modify Security Policies", PermissionCategory.SYSTEM_CONFIGURATION, 5, true);
        register("identity:config:password-policy", "Modify Password Policies", PermissionCategory.SYSTEM_CONFIGURATION, 4);

        // Audit & Compliance Permissions
        register("identity:audit:read", "View Audit Logs", PermissionCategory.AUDIT_AND_COMPLIANCE, 2);
        register("identity:audit:export", "Export Audit Logs", PermissionCategory.AUDIT_AND_COMPLIANCE, 3);
        register("identity:audit:purge", "Purge Audit Logs", PermissionCategory.AUDIT_AND_COMPLIANCE, 5, true);
        register("identity:audit:configure", "Configure Audit Settings", PermissionCategory.AUDIT_AND_COMPLIANCE, 4);

        // Security Operations Permissions
        register("identity:security:session-manage", "Manage User Sessions", PermissionCategory.SECURITY_OPERATIONS, 3);
        register("identity:security:force-logout", "Force Logout Users", PermissionCategory.SECURITY_OPERATIONS, 3);
        register("identity:security:block-ip", "Block IP Addresses", PermissionCategory.SECURITY_OPERATIONS, 4);
        register("identity:security:unlock-account", "Unlock Locked Accounts", PermissionCategory.SECURITY_OPERATIONS, 3);

        // API Management Permissions
        register("identity:api-key:create", "Create API Keys", PermissionCategory.API_MANAGEMENT, 3);
        register("identity:api-key:revoke", "Revoke API Keys", PermissionCategory.API_MANAGEMENT, 3);
        register("identity:webhook:configure", "Configure Webhooks", PermissionCategory.API_MANAGEMENT, 3);

        // Integration Permissions
        register("identity:integration:sso-config", "Configure SSO", PermissionCategory.INTEGRATION_CONFIG, 4);
        register("identity:integration:ldap-config", "Configure LDAP", PermissionCategory.INTEGRATION_CONFIG, 4);

        // Order Management Permissions
        register("order:order:read",          "View Orders",                   PermissionCategory.ORDER_MANAGEMENT, 1);
        register("order:order:create",        "Create Order",                  PermissionCategory.ORDER_MANAGEMENT, 2);
        register("order:order:update",        "Update Order",                  PermissionCategory.ORDER_MANAGEMENT, 2);
        register("order:order:cancel",        "Cancel Order",                  PermissionCategory.ORDER_MANAGEMENT, 3);
        register("order:order:manage-status", "Manage Order Status",           PermissionCategory.ORDER_MANAGEMENT, 3);
        register("order:order:bulk-action",   "Bulk Order Actions",            PermissionCategory.ORDER_MANAGEMENT, 3);
        register("order:order:export",        "Export Orders",                 PermissionCategory.ORDER_MANAGEMENT, 2);
        register("order:order:flag",          "Flag / Unflag Orders",          PermissionCategory.ORDER_MANAGEMENT, 3);
        register("order:order:view-financials","View Order Financials",        PermissionCategory.ORDER_MANAGEMENT, 2);
        register("order:order:stats",         "View Order Statistics",         PermissionCategory.ORDER_MANAGEMENT, 1);

        log.info("Registered {} system permissions", systemPermissions.size());
    }

    private void register(String code, String name, PermissionCategory category, int riskLevel) {
        register(code, name, category, riskLevel, false);
    }

    private void register(String code, String name, PermissionCategory category, int riskLevel, boolean requiresApproval) {
        systemPermissions.put(code, new PermissionDefinition(code, name, category, riskLevel, requiresApproval));
    }

    public List<String> getAllPermissionCodes() {
        return new ArrayList<>(systemPermissions.keySet());
    }

    public List<String> getPermissionsByCategory(PermissionCategory category) {
        return systemPermissions.values().stream()
            .filter(p -> p.category() == category)
            .map(PermissionDefinition::code)
            .toList();
    }

    public record PermissionDefinition(
        String code,
        String name,
        PermissionCategory category,
        int riskLevel,
        boolean requiresApproval
    ) {}
}