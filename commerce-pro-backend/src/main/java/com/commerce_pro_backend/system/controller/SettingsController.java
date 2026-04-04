package com.commerce_pro_backend.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/v1/admin/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Settings", description = "Manage application settings")
public class SettingsController {

    private final ConcurrentHashMap<String, Object> generalSettings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> storeSettings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> checkoutSettings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> notificationSettings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> billingInfo = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        generalSettings.put("siteName", "Commerce Pro");
        generalSettings.put("tagline", "Enterprise E-Commerce Platform");
        generalSettings.put("timezone", "UTC");
        generalSettings.put("dateFormat", "yyyy-MM-dd");
        generalSettings.put("defaultLanguage", "en");
        generalSettings.put("maintenanceMode", false);
        generalSettings.put("maintenanceMessage", "");

        storeSettings.put("storeName", "Commerce Pro Store");
        storeSettings.put("description", "Your one-stop shop for everything");
        storeSettings.put("street", "123 Commerce St");
        storeSettings.put("city", "San Francisco");
        storeSettings.put("state", "CA");
        storeSettings.put("zip", "94102");
        storeSettings.put("country", "US");
        storeSettings.put("phone", "+1-555-123-4567");
        storeSettings.put("email", "store@commercepro.com");
        storeSettings.put("logoUrl", "");
        storeSettings.put("currency", "USD");
        storeSettings.put("taxIncluded", false);
        storeSettings.put("weightUnit", "kg");
        storeSettings.put("dimensionUnit", "cm");

        checkoutSettings.put("guestCheckoutEnabled", true);
        checkoutSettings.put("minOrderAmount", 0);
        checkoutSettings.put("maxOrderAmount", 50000);
        checkoutSettings.put("paymentMethods", List.of("credit_card", "paypal", "bank_transfer"));
        checkoutSettings.put("shippingMethods", List.of("standard", "express", "overnight"));
        checkoutSettings.put("orderConfirmationEmail", true);
        checkoutSettings.put("termsRequired", true);
        checkoutSettings.put("orderNotesEnabled", true);

        notificationSettings.put("emailEnabled", true);
        notificationSettings.put("emailFrom", "noreply@commercepro.com");
        notificationSettings.put("emailFromName", "Commerce Pro");
        notificationSettings.put("smtpHost", "smtp.example.com");
        notificationSettings.put("smtpPort", 587);
        notificationSettings.put("smtpUseTls", true);
        notificationSettings.put("smsEnabled", false);
        notificationSettings.put("smsProvider", "twilio");
        notificationSettings.put("pushEnabled", false);
        notificationSettings.put("templates", Map.of(
                "orderConfirmation", true,
                "shippingUpdate", true,
                "passwordReset", true,
                "welcomeEmail", true
        ));

        billingInfo.put("plan", "Enterprise");
        billingInfo.put("price", 299);
        billingInfo.put("billingCycle", "monthly");
        billingInfo.put("nextBillingDate", "2026-05-01");
        billingInfo.put("status", "active");
        billingInfo.put("usage", Map.of(
                "ordersThisMonth", 1250,
                "products", 4500,
                "storageUsedMb", 8200,
                "apiCalls", 45000
        ));
        billingInfo.put("paymentMethod", Map.of(
                "type", "Visa",
                "last4", "4242",
                "expiry", "12/27"
        ));
        billingInfo.put("history", List.of(
                Map.of("date", "2026-04-01", "description", "Enterprise Plan - Monthly", "amount", 299, "status", "paid"),
                Map.of("date", "2026-03-01", "description", "Enterprise Plan - Monthly", "amount", 299, "status", "paid"),
                Map.of("date", "2026-02-01", "description", "Enterprise Plan - Monthly", "amount", 299, "status", "paid")
        ));
    }

    @GetMapping("/general")
    @PreAuthorize("hasAuthority('system:settings:read')")
    @Operation(summary = "Get general settings")
    public ApiResponse<Map<String, Object>> getGeneralSettings() {
        return ApiResponse.success(new HashMap<>(generalSettings));
    }

    @PutMapping("/general")
    @PreAuthorize("hasAuthority('system:settings:manage')")
    @Operation(summary = "Update general settings")
    public ApiResponse<String> updateGeneralSettings(
            @RequestBody Map<String, Object> settings,
            @RequestHeader("X-Admin-Id") String adminId) {
        generalSettings.putAll(settings);
        return ApiResponse.success("General settings updated successfully");
    }

    @GetMapping("/store")
    @PreAuthorize("hasAuthority('system:settings:read')")
    @Operation(summary = "Get store settings")
    public ApiResponse<Map<String, Object>> getStoreSettings() {
        return ApiResponse.success(new HashMap<>(storeSettings));
    }

    @PutMapping("/store")
    @PreAuthorize("hasAuthority('system:settings:manage')")
    @Operation(summary = "Update store settings")
    public ApiResponse<String> updateStoreSettings(
            @RequestBody Map<String, Object> settings,
            @RequestHeader("X-Admin-Id") String adminId) {
        storeSettings.putAll(settings);
        return ApiResponse.success("Store settings updated successfully");
    }

    @GetMapping("/checkout")
    @PreAuthorize("hasAuthority('system:settings:read')")
    @Operation(summary = "Get checkout settings")
    public ApiResponse<Map<String, Object>> getCheckoutSettings() {
        return ApiResponse.success(new HashMap<>(checkoutSettings));
    }

    @PutMapping("/checkout")
    @PreAuthorize("hasAuthority('system:settings:manage')")
    @Operation(summary = "Update checkout settings")
    public ApiResponse<String> updateCheckoutSettings(
            @RequestBody Map<String, Object> settings,
            @RequestHeader("X-Admin-Id") String adminId) {
        checkoutSettings.putAll(settings);
        return ApiResponse.success("Checkout settings updated successfully");
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAuthority('system:settings:read')")
    @Operation(summary = "Get notification settings")
    public ApiResponse<Map<String, Object>> getNotificationSettings() {
        return ApiResponse.success(new HashMap<>(notificationSettings));
    }

    @PutMapping("/notifications")
    @PreAuthorize("hasAuthority('system:settings:manage')")
    @Operation(summary = "Update notification settings")
    public ApiResponse<String> updateNotificationSettings(
            @RequestBody Map<String, Object> settings,
            @RequestHeader("X-Admin-Id") String adminId) {
        notificationSettings.putAll(settings);
        return ApiResponse.success("Notification settings updated successfully");
    }

    @GetMapping("/billing")
    @PreAuthorize("hasAuthority('system:settings:read')")
    @Operation(summary = "Get billing info")
    public ApiResponse<Map<String, Object>> getBillingInfo() {
        return ApiResponse.success(new HashMap<>(billingInfo));
    }

    @PutMapping("/billing")
    @PreAuthorize("hasAuthority('system:settings:manage')")
    @Operation(summary = "Update billing info")
    public ApiResponse<String> updateBillingInfo(
            @RequestBody Map<String, Object> settings,
            @RequestHeader("X-Admin-Id") String adminId) {
        billingInfo.putAll(settings);
        return ApiResponse.success("Billing info updated successfully");
    }
}
