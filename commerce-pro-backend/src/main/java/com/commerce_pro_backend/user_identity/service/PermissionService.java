// PermissionService.java
package com.commerce_pro_backend.user_identity.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.config.PermissionRegistry;
import com.commerce_pro_backend.user_identity.dto.CreatePermissionRequest;
import com.commerce_pro_backend.user_identity.dto.PermissionDTO;
import com.commerce_pro_backend.user_identity.dto.SyncResult;
import com.commerce_pro_backend.user_identity.dto.UpdatePermissionRequest;
import com.commerce_pro_backend.user_identity.entity.Permission;
import com.commerce_pro_backend.user_identity.entity.User;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.enums.PermissionCategory;
import com.commerce_pro_backend.user_identity.repository.PermissionRepository;
import com.commerce_pro_backend.user_identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionRegistry permissionRegistry;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<PermissionDTO> findPermissions(PermissionCategory category, Boolean systemOnly, Pageable pageable) {
        return permissionRepository.findWithFilters(category, systemOnly, pageable)
            .map(this::mapToDTO);
    }

    @Transactional
    public PermissionDTO createPermission(CreatePermissionRequest request, String adminId) {
        if (permissionRepository.existsById(request.getCode())) {
            throw ApiException.conflict("Permission code already exists");
        }

        Permission permission = Permission.builder()
            .code(request.getCode())
            .name(request.getName())
            .description(request.getDescription())
            .category(request.getCategory())
            .isSystem(false)
            .requiresApproval(request.isRequiresApproval())
            .riskLevel(request.getRiskLevel())
            .applicableScopes(new HashSet<>(request.getApplicableScopes()))
            .createdBy(adminId)
            .build();

        Permission saved = permissionRepository.save(permission);

        auditService.log(adminId, AuditAction.PERMISSION_CREATED, "PERMISSION", saved.getCode(), 
            saved.getName(), resolveUsername(adminId), null, "Created custom permission", true);

        return mapToDTO(saved);
    }

    @Transactional
    public PermissionDTO updatePermission(String code, UpdatePermissionRequest request, String adminId) {
        Permission permission = permissionRepository.findById(code)
            .orElseThrow(() -> ApiException.notFound("Permission", code));

        if (permission.getIsSystem()) {
            throw ApiException.forbidden("Cannot modify system permission");
        }

        if (request.getName() != null) permission.setName(request.getName());
        if (request.getDescription() != null) permission.setDescription(request.getDescription());
        if (request.getRequiresApproval() != null) permission.setRequiresApproval(request.getRequiresApproval());
        if (request.getRiskLevel() != null) permission.setRiskLevel(request.getRiskLevel());
        if (request.getApplicableScopes() != null) permission.setApplicableScopes(new HashSet<>(request.getApplicableScopes()));

        permission.setUpdatedBy(adminId);
        permission.setUpdatedAt(LocalDateTime.now());

        Permission saved = permissionRepository.save(permission);

        auditService.log(adminId, AuditAction.PERMISSION_UPDATED, "PERMISSION", code, 
            saved.getName(), resolveUsername(adminId), null, "Updated permission", true);

        return mapToDTO(saved);
    }

    @Transactional
    public void deletePermission(String code, String adminId) {
        Permission permission = permissionRepository.findById(code)
            .orElseThrow(() -> ApiException.notFound("Permission", code));

        if (permission.getIsSystem()) {
            throw ApiException.forbidden("Cannot delete system permission");
        }

        long roleCount = permissionRepository.countRolesUsingPermission(code);
        if (roleCount > 0) {
            throw ApiException.conflict("Permission is used by " + roleCount + " roles");
        }

        permissionRepository.delete(permission);

        auditService.log(adminId, AuditAction.PERMISSION_DELETED, "PERMISSION", code, 
            permission.getName(), resolveUsername(adminId), null, "Deleted permission", true);
    }

    @Transactional
    public SyncResult syncFromRegistry(String adminId) {
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        for (var entry : permissionRegistry.getSystemPermissions().entrySet()) {
            String code = entry.getKey();
            PermissionRegistry.PermissionDefinition def = entry.getValue();

            Optional<Permission> existing = permissionRepository.findById(code);

            if (existing.isEmpty()) {
                Permission perm = Permission.builder()
                    .code(code)
                    .name(def.name())
                    .description("System permission: " + def.name())
                    .category(def.category())
                    .isSystem(true)
                    .requiresApproval(def.requiresApproval())
                    .riskLevel(def.riskLevel())
                    .applicableScopes(java.util.Set.of("any", "own", "system"))
                    .createdBy("SYSTEM_SYNC")
                    .build();
                permissionRepository.save(perm);
                created++;
            } else {
                Permission perm = existing.get();
                boolean changed = false;

                if (!perm.getName().equals(def.name())) {
                    perm.setName(def.name());
                    changed = true;
                }
                if (perm.getRiskLevel() != def.riskLevel()) {
                    perm.setRiskLevel(def.riskLevel());
                    changed = true;
                }
                if (perm.getRequiresApproval() != def.requiresApproval()) {
                    perm.setRequiresApproval(def.requiresApproval());
                    changed = true;
                }

                if (changed) {
                    perm.setUpdatedBy("SYSTEM_SYNC");
                    perm.setUpdatedAt(LocalDateTime.now());
                    permissionRepository.save(perm);
                    updated++;
                } else {
                    unchanged++;
                }
            }
        }

        auditService.log(adminId, AuditAction.CONFIG_UPDATED, "SYSTEM", "PERMISSION_REGISTRY", 
            "Sync", resolveUsername(adminId), null, String.format("Created: %d, Updated: %d, Unchanged: %d", created, updated, unchanged), true);

        return SyncResult.builder()
            .created(created)
            .updated(updated)
            .unchanged(unchanged)
            .failed(0)
            .executionTimeMs(0) // Calculate if needed
            .build();
    }

    private String resolveUsername(String userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
            .map(User::getUsername)
            .orElse(userId);
    }

    private PermissionDTO mapToDTO(Permission permission) {
        return PermissionDTO.builder()
            .code(permission.getCode())
            .name(permission.getName())
            .description(permission.getDescription())
            .category(permission.getCategory().name())
            .system(permission.getIsSystem())
            .requiresApproval(permission.getRequiresApproval())
            .riskLevel(permission.getRiskLevel())
            .applicableScopes(permission.getApplicableScopes())
            .roleCount((int) permissionRepository.countRolesUsingPermission(permission.getCode()))
            .createdAt(permission.getCreatedAt())
            .build();
    }
}
