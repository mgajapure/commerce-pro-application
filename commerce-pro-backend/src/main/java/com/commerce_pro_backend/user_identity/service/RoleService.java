// RoleService.java
package com.commerce_pro_backend.user_identity.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.config.RoleHierarchyConfig;
import com.commerce_pro_backend.user_identity.dto.CreateRoleRequest;
import com.commerce_pro_backend.user_identity.dto.PermissionDTO;
import com.commerce_pro_backend.user_identity.dto.RoleDTO;
import com.commerce_pro_backend.user_identity.dto.RoleDetailDTO;
import com.commerce_pro_backend.user_identity.dto.RoleHierarchyDTO;
import com.commerce_pro_backend.user_identity.dto.RoleTemplateDTO;
import com.commerce_pro_backend.user_identity.dto.UpdateRoleRequest;
import com.commerce_pro_backend.user_identity.entity.Permission;
import com.commerce_pro_backend.user_identity.entity.Role;
import com.commerce_pro_backend.user_identity.entity.User;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.enums.RoleType;
import com.commerce_pro_backend.user_identity.repository.PermissionRepository;
import com.commerce_pro_backend.user_identity.repository.RoleRepository;
import com.commerce_pro_backend.user_identity.repository.UserRepository;
import com.commerce_pro_backend.user_identity.repository.UserRoleAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final AuditService auditService;
    private final RoleHierarchyConfig hierarchyConfig;

    @Transactional
    public RoleDTO createRole(CreateRoleRequest request, String adminId) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw ApiException.conflict("Role code already exists");
        }

        Role role = Role.builder()
            .code(request.getCode())
            .name(request.getName())
            .description(request.getDescription())
            .type(determineRoleType(request))
            .isSystem(false)
            .requiresMfa(request.isRequiresMfa())
            .sessionTimeoutMinutes(request.getSessionTimeoutMinutes())
            .allowedIpPatterns(request.getAllowedIpPatterns())
            .createdBy(adminId)
            .build();

        if (request.getParentRoleId() != null) {
            Role parent = roleRepository.findById(request.getParentRoleId())
                .orElseThrow(() -> ApiException.notFound("Parent role", request.getParentRoleId()));
            role.setParentRole(parent);
        }

        if (request.getPermissionCodes() != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionCodes()));
            role.setPermissions(permissions);
        }

        Role saved = roleRepository.save(role);

        auditService.log(adminId, AuditAction.ROLE_CREATED, "ROLE", saved.getId(), 
            saved.getCode(), resolveUsername(adminId), null, "Created role: " + saved.getName(), true);

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<RoleDTO> findRoles(Boolean systemOnly, Pageable pageable) {
        return roleRepository.findWithFilter(systemOnly, pageable)
            .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public RoleDetailDTO getRoleDetail(String id) {
        Role role = roleRepository.findWithDetailsById(id)
            .orElseThrow(() -> ApiException.notFound("Role", id));
        return mapToDetailDTO(role);
    }

    @Transactional
    public RoleDTO updateRole(String id, UpdateRoleRequest request, String adminId) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Role", id));

        if (role.getIsSystem() && !hierarchyConfig.getHierarchyRules().getImmutableSystemRoles().contains(role.getCode())) {
            throw ApiException.forbidden("Cannot modify system role");
        }

        String oldValue = String.format("name=%s, mfa=%s, timeout=%s",
            role.getName(), role.getRequiresMfa(), role.getSessionTimeoutMinutes());

        if (request.getName() != null) role.setName(request.getName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        if (request.getRequiresMfa() != null) role.setRequiresMfa(request.getRequiresMfa());
        if (request.getSessionTimeoutMinutes() != null) role.setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
        if (request.getAllowedIpPatterns() != null) role.setAllowedIpPatterns(request.getAllowedIpPatterns());
        if (request.getConstraints() != null) role.setConstraints(new HashSet<>(request.getConstraints()));

        role.setUpdatedBy(adminId);
        role.setUpdatedAt(LocalDateTime.now());

        Role saved = roleRepository.save(role);

        String newValue = String.format("name=%s, mfa=%s, timeout=%s",
            saved.getName(), saved.getRequiresMfa(), saved.getSessionTimeoutMinutes());

        auditService.log(adminId, AuditAction.ROLE_UPDATED, "ROLE", id, 
            saved.getCode(), resolveUsername(adminId), oldValue, newValue, true);

        return mapToDTO(saved);
    }

    @Transactional
    public void deleteRole(String id, String adminId, boolean force) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Role", id));

        if (role.getIsSystem()) {
            throw ApiException.forbidden("Cannot delete system role");
        }

        long assignmentCount = assignmentRepository.countByRoleId(id);
        if (assignmentCount > 0 && !force) {
            throw ApiException.conflict("Role has active assignments. Use force=true to delete.");
        }

        roleRepository.delete(role);

        auditService.log(adminId, AuditAction.ROLE_DELETED, "ROLE", id, 
            role.getCode(), resolveUsername(adminId), null, "Deleted role with " + assignmentCount + " assignments", true);
    }

    @Transactional
    public void grantPermissions(String roleId, List<String> permissionCodes, String adminId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> ApiException.notFound("Role", roleId));

        Set<Permission> newPermissions = new HashSet<>(permissionRepository.findAllById(permissionCodes));
        role.getPermissions().addAll(newPermissions);
        
        role.setUpdatedBy(adminId);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        auditService.log(adminId, AuditAction.PERMISSION_GRANTED, "ROLE", roleId, 
            role.getCode(), resolveUsername(adminId), null, "Granted permissions: " + String.join(", ", permissionCodes), true);
    }

    @Transactional
    public void revokePermissions(String roleId, List<String> permissionCodes, String adminId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> ApiException.notFound("Role", roleId));

        role.getPermissions().removeIf(p -> permissionCodes.contains(p.getCode()));
        
        role.setUpdatedBy(adminId);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        auditService.log(adminId, AuditAction.PERMISSION_REVOKED, "ROLE", roleId, 
            role.getCode(), resolveUsername(adminId), null, "Revoked permissions: " + String.join(", ", permissionCodes), true);
    }

    @Transactional
    public void setParentRole(String roleId, String parentRoleId, String adminId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> ApiException.notFound("Role", roleId));
        
        Role parent = roleRepository.findById(parentRoleId)
            .orElseThrow(() -> ApiException.notFound("Parent role", parentRoleId));

        // Check for cycles
        if (wouldCreateCycle(role, parent)) {
            throw ApiException.badRequest("Setting this parent would create a cycle in role hierarchy");
        }

        role.setParentRole(parent);
        role.setUpdatedBy(adminId);
        roleRepository.save(role);

        auditService.log(adminId, AuditAction.ROLE_UPDATED, "ROLE", roleId, 
            role.getCode(), resolveUsername(adminId), null, "Set parent role to: " + parent.getCode(), true);
    }

    @Transactional(readOnly = true)
    public List<RoleHierarchyDTO> getRoleHierarchy() {
        List<Role> rootRoles = roleRepository.findRootRoles();
        return rootRoles.stream()
            .map(role -> buildHierarchyTree(role, 0))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoleTemplateDTO> getRoleTemplates() {
        return hierarchyConfig.getTemplates().entrySet().stream()
            .map(entry -> RoleTemplateDTO.builder()
                .templateCode(entry.getKey())
                .name(entry.getValue().getName())
                .description(entry.getValue().getDescription())
                .suggestedPermissions(entry.getValue().getPermissions())
                .requiresApproval(entry.getValue().isRequiresApproval())
                .maxAssignments(entry.getValue().getMaxAssignments())
                .build())
            .collect(Collectors.toList());
    }

    private String resolveUsername(String userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
            .map(User::getUsername)
            .orElse(userId);
    }

    private boolean wouldCreateCycle(Role role, Role newParent) {
        Set<String> visited = new HashSet<>();
        Role current = newParent;
        while (current != null) {
            if (current.getId().equals(role.getId())) return true;
            if (!visited.add(current.getId())) break; // Already visited
            current = current.getParentRole();
        }
        return false;
    }

    private RoleHierarchyDTO buildHierarchyTree(Role role, int depth) {
        RoleHierarchyDTO dto = RoleHierarchyDTO.builder()
            .roleId(role.getId())
            .roleCode(role.getCode())
            .roleName(role.getName())
            .parentRoleId(role.getParentRole() != null ? role.getParentRole().getId() : null)
            .depth(depth)
            .children(new ArrayList<>())
            .build();

        for (Role child : role.getChildRoles()) {
            dto.getChildren().add(buildHierarchyTree(child, depth + 1));
        }

        return dto;
    }

    private RoleDTO mapToDTO(Role role) {
        return RoleDTO.builder()
            .id(role.getId())
            .code(role.getCode())
            .name(role.getName())
            .description(role.getDescription())
            .type(role.getType().name())
            .system(role.getIsSystem())
            .superAdmin(role.getIsSuperAdmin())
            .parentRoleId(role.getParentRole() != null ? role.getParentRole().getId() : null)
            .parentRoleName(role.getParentRole() != null ? role.getParentRole().getName() : null)
            .permissionCount(role.getPermissions().size())
            .assignedUserCount((int) assignmentRepository.countByRoleId(role.getId()))
            .createdAt(role.getCreatedAt())
            .build();
    }

    private RoleDetailDTO mapToDetailDTO(Role role) {
        RoleDetailDTO dto = new RoleDetailDTO();
        // Copy base fields
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setType(role.getType().name());
        dto.setSystem(role.getIsSystem());
        dto.setSuperAdmin(role.getIsSuperAdmin());
        dto.setParentRoleId(role.getParentRole() != null ? role.getParentRole().getId() : null);
        dto.setParentRoleName(role.getParentRole() != null ? role.getParentRole().getName() : null);
        dto.setPermissionCount(role.getPermissions().size());
        dto.setAssignedUserCount((int) assignmentRepository.countByRoleId(role.getId()));
        dto.setCreatedAt(role.getCreatedAt());
        
        // Detail fields
        dto.setPermissions(role.getPermissions().stream()
            .map(this::mapPermissionToDTO)
            .collect(Collectors.toSet()));
        dto.setChildRoles(role.getChildRoles().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList()));
        dto.setConstraints(role.getConstraints().stream().toList());
        dto.setRequiresMfa(role.getRequiresMfa());
        dto.setSessionTimeoutMinutes(role.getSessionTimeoutMinutes());
        dto.setAllowedIpPatterns(role.getAllowedIpPatterns());
        
        return dto;
    }

    private PermissionDTO mapPermissionToDTO(Permission permission) {
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

    private RoleType determineRoleType(CreateRoleRequest request) {
        if (request.getParentRoleId() == null) {
            return RoleType.GLOBAL;
        }
        return RoleType.TENANT;
    }
}
