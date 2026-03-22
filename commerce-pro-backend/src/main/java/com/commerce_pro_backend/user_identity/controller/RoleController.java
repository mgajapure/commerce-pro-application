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
import com.commerce_pro_backend.user_identity.dto.CreateRoleRequest;
import com.commerce_pro_backend.user_identity.dto.RoleDTO;
import com.commerce_pro_backend.user_identity.dto.RoleDetailDTO;
import com.commerce_pro_backend.user_identity.dto.RoleHierarchyDTO;
import com.commerce_pro_backend.user_identity.dto.RoleTemplateDTO;
import com.commerce_pro_backend.user_identity.dto.UpdateRoleRequest;
import com.commerce_pro_backend.user_identity.service.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/identity/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Role Management", description = "Manage roles and permissions")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasAuthority('identity:role:create')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new role")
    public ApiResponse<RoleDTO> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @RequestHeader("X-Admin-Id") String adminId) {
        return ApiResponse.success(roleService.createRole(request, adminId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('identity:role:read')")
    @Operation(summary = "List all roles")
    public ApiResponse<Page<RoleDTO>> listRoles(
            @RequestParam(name = "systemOnly", required = false) Boolean systemOnly,
            Pageable pageable) {
        return ApiResponse.success(roleService.findRoles(systemOnly, pageable));
    }

    @GetMapping("/hierarchy")
    @PreAuthorize("hasAuthority('identity:role:read')")
    @Operation(summary = "Get role hierarchy tree")
    public ApiResponse<List<RoleHierarchyDTO>> getRoleHierarchy() {
        return ApiResponse.success(roleService.getRoleHierarchy());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('identity:role:read')")
    @Operation(summary = "Get role details")
    public ApiResponse<RoleDetailDTO> getRole(@PathVariable String id) {
        return ApiResponse.success(roleService.getRoleDetail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('identity:role:update')")
    @Operation(summary = "Update role")
    public ApiResponse<RoleDTO> updateRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request,
            @RequestHeader("X-Admin-Id") String adminId) {
        return ApiResponse.success(roleService.updateRole(id, request, adminId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('identity:role:delete')")
    @Operation(summary = "Delete role")
    public ApiResponse<String> deleteRole(
            @PathVariable String id,
            @RequestHeader("X-Admin-Id") String adminId,
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force) {
        roleService.deleteRole(id, adminId, force);
        return ApiResponse.success("Role deleted successfully");
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('identity:role:manage-permissions')")
    @Operation(summary = "Grant permissions to role")
    public ApiResponse<String> grantPermissions(
            @PathVariable String id,
            @RequestBody List<String> permissionCodes,
            @RequestHeader("X-Admin-Id") String adminId) {
        roleService.grantPermissions(id, permissionCodes, adminId);
        return ApiResponse.success("Permissions granted");
    }

    @DeleteMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('identity:role:manage-permissions')")
    @Operation(summary = "Revoke permissions from role")
    public ApiResponse<String> revokePermissions(
            @PathVariable String id,
            @RequestBody List<String> permissionCodes,
            @RequestHeader("X-Admin-Id") String adminId) {
        roleService.revokePermissions(id, permissionCodes, adminId);
        return ApiResponse.success("Permissions revoked");
    }

    @PostMapping("/{id}/hierarchy/parent")
    @PreAuthorize("hasAuthority('identity:role:manage-hierarchy')")
    @Operation(summary = "Set parent role")
    public ApiResponse<String> setParentRole(
            @PathVariable String id,
            @RequestParam(name = "parentRoleId") String parentRoleId,
            @RequestHeader("X-Admin-Id") String adminId) {
        roleService.setParentRole(id, parentRoleId, adminId);
        return ApiResponse.success("Parent role updated");
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('identity:role:read')")
    @Operation(summary = "Get available role templates")
    public ApiResponse<List<RoleTemplateDTO>> getRoleTemplates() {
        return ApiResponse.success(roleService.getRoleTemplates());
    }
}