// PermissionCategory.java
package com.commerce_pro_backend.user_identity.enums;

public enum PermissionCategory {
    IDENTITY_MANAGEMENT,    // Users, roles, permissions
    SYSTEM_CONFIGURATION,   // Security settings, password policies
    AUDIT_AND_COMPLIANCE,   // Viewing audit logs, reports
    SECURITY_OPERATIONS,    // Session management, force logout
    API_MANAGEMENT,         // API keys, webhooks
    INTEGRATION_CONFIG,     // SSO, LDAP, external IdPs
    ORDER_MANAGEMENT,       // Orders, fulfilment, cancellations
    FULFILLMENT_MANAGEMENT, // Pick lists, shipments, carriers, shipping rules
    CUSTOMER_MANAGEMENT,     //
    ANALYTICS_REPORTING     // Analytics, reports, KPIs
}