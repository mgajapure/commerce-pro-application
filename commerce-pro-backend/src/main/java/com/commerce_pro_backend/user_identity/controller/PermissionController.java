package com.commerce_pro_backend.user_identity.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.user_identity.config.PermissionRegistry;
import com.commerce_pro_backend.user_identity.dto.CreatePermissionRequest;
import com.commerce_pro_backend.user_identity.dto.PermissionDTO;
import com.commerce_pro_backend.user_identity.dto.SyncResult;
import com.commerce_pro_backend.user_identity.dto.UpdatePermissionRequest;
import com.commerce_pro_backend.user_identity.enums.PermissionCategory;
import com.commerce_pro_backend.user_identity.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/identity/permissions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Permission Management", description = "Manage system permissions")
public class PermissionController {

    private final PermissionService permissionService;
    private final PermissionRegistry permissionRegistry;

    @GetMapping("/registry")
    @PreAuthorize("hasAuthority('identity:permission:read')")
    @Operation(summary = "Get all registered system permissions")
    public ApiResponse<List<PermissionRegistry.PermissionDefinition>> getPermissionRegistry() {
        return ApiResponse.success(permissionRegistry.getSystemPermissions().values().stream().toList());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('identity:permission:read')")
    @Operation(summary = "List all permissions in database")
    public ApiResponse<Page<PermissionDTO>> listPermissions(
            @RequestParam(name = "category", required = false) PermissionCategory category,
            @RequestParam(name = "systemOnly", required = false) Boolean systemOnly,
            Pageable pageable) {
        return ApiResponse.success(permissionService.findPermissions(category, systemOnly, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('identity:permission:create')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create custom permission")
    public ApiResponse<PermissionDTO> createPermission(
            @Valid @RequestBody CreatePermissionRequest request,
            @RequestHeader("X-Admin-Id") String adminId) {
        return ApiResponse.success(permissionService.createPermission(request, adminId));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('identity:permission:update')")
    @Operation(summary = "Update permission")
    public ApiResponse<PermissionDTO> updatePermission(
            @PathVariable String code,
            @Valid @RequestBody UpdatePermissionRequest request,
            @RequestHeader("X-Admin-Id") String adminId) {
        return ApiResponse.success(permissionService.updatePermission(code, request, adminId));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAuthority('identity:permission:delete')")
    @Operation(summary = "Delete permission")
    public ApiResponse<String> deletePermission(
            @PathVariable String code,
            @RequestHeader("X-Admin-Id") String adminId) {
        permissionService.deletePermission(code, adminId);
        return ApiResponse.success("Permission deleted");
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('identity:permission:read')")
    @Operation(summary = "Get permission categories")
    public ApiResponse<List<PermissionCategory>> getCategories() {
        return ApiResponse.success(List.of(PermissionCategory.values()));
    }

    @PostMapping("/sync-registry")
    @PreAuthorize("hasAuthority('identity:permission:system-modify')")
    @Operation(summary = "Sync system permissions from registry to database")
    public ApiResponse<SyncResult> syncPermissions(@RequestHeader("X-Admin-Id") String adminId) {
        return ApiResponse.success(permissionService.syncFromRegistry(adminId));
    }
}