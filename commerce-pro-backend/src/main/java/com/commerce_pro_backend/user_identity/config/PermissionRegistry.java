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

        // Fulfillment & WMS Permissions
        register("fulfillment:picklist:read",    "View Fulfillment Queue & Pick Lists", PermissionCategory.FULFILLMENT_MANAGEMENT, 1);
        register("fulfillment:picklist:create",  "Generate Pick Lists",                 PermissionCategory.FULFILLMENT_MANAGEMENT, 2);
        register("fulfillment:picklist:manage",  "Manage Pick Lists (assign/complete)",  PermissionCategory.FULFILLMENT_MANAGEMENT, 3);
        register("fulfillment:shipment:read",    "View Shipments & Tracking",            PermissionCategory.FULFILLMENT_MANAGEMENT, 1);
        register("fulfillment:shipment:create",  "Create Shipments",                    PermissionCategory.FULFILLMENT_MANAGEMENT, 2);
        register("fulfillment:shipment:update",  "Update Shipments & Add Tracking",     PermissionCategory.FULFILLMENT_MANAGEMENT, 2);
        register("fulfillment:shipment:manage",  "Manage Shipment Status",              PermissionCategory.FULFILLMENT_MANAGEMENT, 3);
        register("fulfillment:shipment:delete",  "Delete Shipments",                    PermissionCategory.FULFILLMENT_MANAGEMENT, 4);
        register("fulfillment:carrier:read",     "View Carriers",                       PermissionCategory.FULFILLMENT_MANAGEMENT, 1);
        register("fulfillment:carrier:manage",   "Manage Carriers",                     PermissionCategory.FULFILLMENT_MANAGEMENT, 3);
        register("fulfillment:rules:read",       "View Shipping Rules",                 PermissionCategory.FULFILLMENT_MANAGEMENT, 1);
        register("fulfillment:rules:manage",     "Manage Shipping Rules",               PermissionCategory.FULFILLMENT_MANAGEMENT, 3);
        register("fulfillment:stats",            "View Fulfillment Statistics",         PermissionCategory.FULFILLMENT_MANAGEMENT, 1);

         // Customer Management Permissions
         register("customer:customer:read",           "View Customers",               PermissionCategory.CUSTOMER_MANAGEMENT, 1);
         register("customer:customer:create",         "Create Customer",              PermissionCategory.CUSTOMER_MANAGEMENT, 2);
         register("customer:customer:update",         "Update Customer",              PermissionCategory.CUSTOMER_MANAGEMENT, 2);
         register("customer:customer:delete",         "Delete Customer",              PermissionCategory.CUSTOMER_MANAGEMENT, 4, true);
         register("customer:customer:stats",          "View Customer Statistics",     PermissionCategory.CUSTOMER_MANAGEMENT, 1);
         register("customer:customer:manage-status",  "Change Customer Status",       PermissionCategory.CUSTOMER_MANAGEMENT, 3);
         register("customer:customer:manage-tier",    "Override Customer Tier",       PermissionCategory.CUSTOMER_MANAGEMENT, 3);
         register("customer:customer:blacklist",      "Blacklist / Unblacklist",      PermissionCategory.CUSTOMER_MANAGEMENT, 4, true);
         register("customer:customer:fraud-flag",     "Flag / Resolve Fraud",         PermissionCategory.CUSTOMER_MANAGEMENT, 4, true);
         register("customer:customer:manage-loyalty", "Adjust Loyalty Points",        PermissionCategory.CUSTOMER_MANAGEMENT, 3);
         register("customer:communication:create",    "Log Customer Communication",   PermissionCategory.CUSTOMER_MANAGEMENT, 2);
         register("customer:group:read",              "View Customer Groups",         PermissionCategory.CUSTOMER_MANAGEMENT, 1);
         register("customer:group:create",            "Create Customer Group",        PermissionCategory.CUSTOMER_MANAGEMENT, 2);
         register("customer:group:update",            "Update Customer Group",        PermissionCategory.CUSTOMER_MANAGEMENT, 2);
         register("customer:group:delete",            "Delete Customer Group",        PermissionCategory.CUSTOMER_MANAGEMENT, 3);

         // Analytics & Reporting Permissions
        register("analytics:dashboard:read",    "View Analytics Dashboard",          PermissionCategory.ANALYTICS_REPORTING, 1);
        register("analytics:sales:read",        "View Sales Reports",                PermissionCategory.ANALYTICS_REPORTING, 1);
        register("analytics:inventory:read",    "View Inventory Reports",            PermissionCategory.ANALYTICS_REPORTING, 1);
        register("analytics:orders:read",       "View Order Reports",                PermissionCategory.ANALYTICS_REPORTING, 1);
        register("analytics:customers:read",    "View Customer Reports",             PermissionCategory.ANALYTICS_REPORTING, 1);
        register("analytics:financial:read",    "View Financial Reports",            PermissionCategory.ANALYTICS_REPORTING, 2);
        register("analytics:shipping:read",     "View Shipping Reports",             PermissionCategory.ANALYTICS_REPORTING, 1);
        register("analytics:returns:read",      "View Returns Reports",              PermissionCategory.ANALYTICS_REPORTING, 1);
        register("analytics:reports:read",      "View Saved & Scheduled Reports",    PermissionCategory.ANALYTICS_REPORTING, 1);
        register("analytics:reports:run",       "Run / Export Reports",              PermissionCategory.ANALYTICS_REPORTING, 2);
        register("analytics:reports:manage",    "Create & Manage Saved/Scheduled Reports", PermissionCategory.ANALYTICS_REPORTING, 2);


        // Catalog Management Permissions
        register("catalog:read",  "View Catalog (Products, Categories, Brands, etc.)", PermissionCategory.CATALOG_MANAGEMENT, 1);
        register("catalog:write", "Manage Catalog (Create/Update/Delete)",             PermissionCategory.CATALOG_MANAGEMENT, 2);

        // Inventory Management Permissions
        register("inventory:read",  "View Inventory & Stock Levels",   PermissionCategory.INVENTORY_MANAGEMENT, 1);
        register("inventory:write", "Manage Inventory & Stock Levels", PermissionCategory.INVENTORY_MANAGEMENT, 2);

        // Dashboard Permissions
        register("dashboard:read", "View Dashboard & KPIs", PermissionCategory.ANALYTICS_REPORTING, 1);

        // Notification Permissions
        register("notification:read",  "View Notifications",   PermissionCategory.SYSTEM_CONFIGURATION, 1);
        register("notification:write", "Manage Notifications", PermissionCategory.SYSTEM_CONFIGURATION, 2);

        // Payment Permissions
        register("payment:transaction:read",      "View Transactions",                PermissionCategory.PAYMENT_MANAGEMENT, 1);
        register("payment:transaction:create",    "Create Payments",                  PermissionCategory.PAYMENT_MANAGEMENT, 3);
        register("payment:transaction:capture",   "Capture Authorized Payments",      PermissionCategory.PAYMENT_MANAGEMENT, 3);
        register("payment:transaction:void",      "Void Authorizations",              PermissionCategory.PAYMENT_MANAGEMENT, 3);
        register("payment:transaction:flag",      "Flag Suspicious Transactions",     PermissionCategory.PAYMENT_MANAGEMENT, 2);
        register("payment:refund:read",           "View Refund Requests",             PermissionCategory.PAYMENT_MANAGEMENT, 1);
        register("payment:refund:request",        "Submit Refund Requests",           PermissionCategory.PAYMENT_MANAGEMENT, 2);
        register("payment:refund:approve",        "Approve / Reject Refunds",         PermissionCategory.PAYMENT_MANAGEMENT, 3);
        register("payment:refund:process",        "Process Refund on Gateway",        PermissionCategory.PAYMENT_MANAGEMENT, 4);
        register("payment:chargeback:read",       "View Chargebacks & Disputes",      PermissionCategory.PAYMENT_MANAGEMENT, 1);
        register("payment:chargeback:manage",     "Manage Chargebacks & Disputes",    PermissionCategory.PAYMENT_MANAGEMENT, 3);
        register("payment:reconciliation:read",   "View Reconciliation Reports",      PermissionCategory.PAYMENT_MANAGEMENT, 2);
        register("payment:reconciliation:manage", "Generate Reconciliation Reports",  PermissionCategory.PAYMENT_MANAGEMENT, 3);
        register("payment:payout:read",           "View Payouts",                     PermissionCategory.PAYMENT_MANAGEMENT, 2);
        register("payment:payout:create",         "Create Payout Batches",            PermissionCategory.PAYMENT_MANAGEMENT, 3);
        register("payment:payout:approve",        "Approve Payouts",                  PermissionCategory.PAYMENT_MANAGEMENT, 4);
        register("payment:payout:process",        "Initiate Payout Transfers",        PermissionCategory.PAYMENT_MANAGEMENT, 4);
        register("payment:link:read",             "View Payment Links",               PermissionCategory.PAYMENT_MANAGEMENT, 1);
        register("payment:link:create",           "Create & Manage Payment Links",    PermissionCategory.PAYMENT_MANAGEMENT, 2);
        register("payment:method:read",           "View Saved Payment Methods",       PermissionCategory.PAYMENT_MANAGEMENT, 1);
        register("payment:method:manage",         "Manage Payment Methods",           PermissionCategory.PAYMENT_MANAGEMENT, 2);
        register("payment:gateway:read",          "View Gateway Configurations",      PermissionCategory.PAYMENT_MANAGEMENT, 2);
        register("payment:gateway:manage",        "Manage Gateway Configurations",    PermissionCategory.PAYMENT_MANAGEMENT, 5, true);

        // ── Finance Permissions ────────────────────────────────────
        register("finance:revenue:read",   "View Revenue Overview & Reports",         PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:invoice:read",   "View Customer Invoices",                  PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:invoice:create", "Create & Update Customer Invoices",       PermissionCategory.FINANCE_MANAGEMENT, 2);
        register("finance:invoice:payment","Record Invoice Payments",                 PermissionCategory.FINANCE_MANAGEMENT, 3);
        register("finance:invoice:void",   "Void & Write Off Invoices",               PermissionCategory.FINANCE_MANAGEMENT, 4);
        register("finance:ar:read",        "View Accounts Receivable & Aging",        PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:vendor:read",    "View Vendors",                             PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:vendor:manage",  "Create & Manage Vendors",                 PermissionCategory.FINANCE_MANAGEMENT, 2);
        register("finance:ap:read",        "View Accounts Payable & Vendor Bills",    PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:ap:create",      "Record Vendor Invoices",                  PermissionCategory.FINANCE_MANAGEMENT, 2);
        register("finance:ap:approve",     "Approve & Schedule AP Payments",          PermissionCategory.FINANCE_MANAGEMENT, 3);
        register("finance:ap:pay",         "Record AP Payments",                      PermissionCategory.FINANCE_MANAGEMENT, 4);
        register("finance:tax:read",       "View Tax Rates & Reports",                PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:tax:manage",     "Create & Manage Tax Rates",               PermissionCategory.FINANCE_MANAGEMENT, 3);
        register("finance:expense:read",   "View Expenses",                           PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:expense:create", "Create & Submit Expenses",                PermissionCategory.FINANCE_MANAGEMENT, 2);
        register("finance:expense:approve","Approve & Pay Expenses",                  PermissionCategory.FINANCE_MANAGEMENT, 3);
        register("finance:expense:manage", "Manage Expense Categories",               PermissionCategory.FINANCE_MANAGEMENT, 3);
        register("finance:journal:read",   "View Journal Entries",                    PermissionCategory.FINANCE_MANAGEMENT, 2);
        register("finance:journal:post",   "Post Journal Entries",                    PermissionCategory.FINANCE_MANAGEMENT, 4, true);
        register("finance:period:read",    "View Financial Periods",                  PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:period:manage",  "Create Financial Periods",                PermissionCategory.FINANCE_MANAGEMENT, 3);
        register("finance:period:close",   "Close & Lock Financial Periods",          PermissionCategory.FINANCE_MANAGEMENT, 5, true);
        register("finance:budget:read",    "View Budgets",                            PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:budget:manage",  "Create & Update Budgets",                 PermissionCategory.FINANCE_MANAGEMENT, 3);
        register("finance:budget:approve", "Approve Budgets",                         PermissionCategory.FINANCE_MANAGEMENT, 4);
        register("finance:pnl:read",       "View P&L Statements",                     PermissionCategory.FINANCE_MANAGEMENT, 2);
        register("finance:cashflow:read",  "View Cash Flow Forecasts",                PermissionCategory.FINANCE_MANAGEMENT, 2);
        register("finance:rates:read",     "View Exchange Rates",                     PermissionCategory.FINANCE_MANAGEMENT, 1);
        register("finance:rates:manage",   "Manage Exchange Rates",                   PermissionCategory.FINANCE_MANAGEMENT, 2);

        // AI Features
        register("ai:chatbot:use",            "Use AI Support Chatbot",                  PermissionCategory.AI_FEATURES, 1);
        register("ai:nl-report:use",          "Use AI NL Business Analyst",              PermissionCategory.AI_FEATURES, 2);
        register("ai:fraud:analyse",          "Run AI Fraud Analysis",                   PermissionCategory.AI_FEATURES, 3);
        register("ai:churn:analyse",          "Run AI Churn Prediction",                 PermissionCategory.AI_FEATURES, 2);
        register("ai:forecast:run",           "Run AI Demand Forecast",                  PermissionCategory.AI_FEATURES, 2);
        register("ai:sentiment:analyse",      "Run AI Sentiment Analysis",               PermissionCategory.AI_FEATURES, 2);
        register("ai:pricing:run",            "Run AI Pricing Recommendations",          PermissionCategory.AI_FEATURES, 3);
        register("ai:product:generate",       "Generate AI Product Content",             PermissionCategory.AI_FEATURES, 2);
        register("ai:seo:optimise",           "Run AI SEO Optimisation",                 PermissionCategory.AI_FEATURES, 2);
        register("ai:inventory:optimise",     "Run AI Inventory Optimisation",           PermissionCategory.AI_FEATURES, 2);
        register("ai:marketing:personalise",  "Run AI Marketing Personalisation",        PermissionCategory.AI_FEATURES, 2);
        register("ai:returns:analyse",        "Run AI Returns Analysis",                 PermissionCategory.AI_FEATURES, 2);
        register("ai:shipping:optimise",      "Run AI Shipping Optimisation",            PermissionCategory.AI_FEATURES, 2);
        register("ai:vendor:analyse",         "Run AI Vendor Analysis",                  PermissionCategory.AI_FEATURES, 2);
        register("ai:budget:analyse",         "Run AI Budget Anomaly Detection",         PermissionCategory.AI_FEATURES, 3);
        register("ai:admin",                  "View AI Sessions & Insights",             PermissionCategory.AI_FEATURES, 3);
        register("ai:config:manage",          "Manage AI Configuration & Budgets",       PermissionCategory.AI_FEATURES, 4, true);

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