package com.commerce_pro_backend.user_identity.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.user_identity.dto.AuditLogDTO;
import com.commerce_pro_backend.user_identity.dto.CreateUserRequest;
import com.commerce_pro_backend.user_identity.dto.ImpersonationToken;
import com.commerce_pro_backend.user_identity.dto.RoleAssignmentRequest;
import com.commerce_pro_backend.user_identity.dto.UpdateUserRequest;
import com.commerce_pro_backend.user_identity.dto.UserDTO;
import com.commerce_pro_backend.user_identity.dto.UserDetailDTO;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import com.commerce_pro_backend.user_identity.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/identity/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "Manage users in the system")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @PreAuthorize("hasAuthority('identity:user:create')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new user")
    public ApiResponse<UserDTO> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request, currentUserService.getCurrentUserId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('identity:user:read')")
    @Operation(summary = "List all users with filtering and pagination")
    public ApiResponse<Page<UserDTO>> listUsers(
            @Parameter(description = "Filter by active status") 
            @RequestParam(name = "active", required = false) Boolean active,
            @Parameter(description = "Filter by role") 
            @RequestParam(name = "roleCode", required = false) String roleCode,
            @Parameter(description = "Search by username/email") 
            @RequestParam(name = "search", required = false) String search,
            Pageable pageable) {
        return ApiResponse.success(userService.findUsers(active, roleCode, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('identity:user:read')")
    @Operation(summary = "Get user details by ID")
    public ApiResponse<UserDetailDTO> getUser(@PathVariable String id) {
        return ApiResponse.success(userService.getUserDetail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('identity:user:update')")
    @Operation(summary = "Update user information")
    public ApiResponse<UserDTO> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(id, request, currentUserService.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('identity:user:delete')")
    @Operation(summary = "Delete user (soft delete with audit)")
    public ApiResponse<String> deleteUser(
            @PathVariable String id,
            @RequestParam(name = "reason", required = false) String reason) {
        userService.deleteUser(id, currentUserService.getCurrentUserId(), reason);
        return ApiResponse.success("User deleted successfully");
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('identity:user:activate')")
    @Operation(summary = "Activate user account")
    public ApiResponse<String> activateUser(
            @PathVariable String id) {
        userService.activateUser(id, currentUserService.getCurrentUserId());
        return ApiResponse.success("User activated");
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('identity:user:activate')")
    @Operation(summary = "Deactivate user account")
    public ApiResponse<String> deactivateUser(
            @PathVariable String id,
            @RequestParam(name = "reason") String reason) {
        userService.deactivateUser(id, currentUserService.getCurrentUserId(), reason);
        return ApiResponse.success("User deactivated");
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('identity:user:reset-password')")
    @Operation(summary = "Force password reset")
    public ApiResponse<String> resetPassword(
            @PathVariable String id,
            @RequestParam(name="notifyUser", required = false, defaultValue = "true") boolean notifyUser) {
        userService.resetPassword(id, currentUserService.getCurrentUserId(), notifyUser);
        return ApiResponse.success("Password reset initiated");
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('identity:security:unlock-account')")
    @Operation(summary = "Unlock locked account")
    public ApiResponse<String> unlockAccount(
            @PathVariable String id) {
        userService.unlockAccount(id, currentUserService.getCurrentUserId());
        return ApiResponse.success("Account unlocked");
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('identity:user:manage-roles')")
    @Operation(summary = "Assign role to user")
    public ApiResponse<String> assignRole(
            @PathVariable String id,
            @Valid @RequestBody RoleAssignmentRequest request) {
        userService.assignRole(id, request, currentUserService.getCurrentUserId());
        return ApiResponse.success("Role assigned successfully");
    }

    @DeleteMapping("/{id}/roles/{assignmentId}")
    @PreAuthorize("hasAuthority('identity:user:manage-roles')")
    @Operation(summary = "Revoke role from user")
    public ApiResponse<String> revokeRole(
            @PathVariable String id,
            @PathVariable String assignmentId,
            @RequestParam(name="reason") String reason) {
        userService.revokeRole(id, assignmentId, currentUserService.getCurrentUserId(), reason);
        return ApiResponse.success("Role revoked successfully");
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('identity:user:view-audit')")
    @Operation(summary = "Get user audit history")
    public ApiResponse<Page<AuditLogDTO>> getUserAudit(
            @PathVariable String id,
            Pageable pageable) {
        return ApiResponse.success(userService.getUserAuditHistory(id, pageable));
    }

    @PostMapping("/{id}/impersonate")
    @PreAuthorize("hasAuthority('identity:user:impersonate')")
    @Operation(summary = "Start impersonation session")
    public ApiResponse<ImpersonationToken> impersonateUser(
            @PathVariable String id) {
        return ApiResponse.success(userService.startImpersonation(id, currentUserService.getCurrentUserId()));
    }
}
