// UserService.java
package com.commerce_pro_backend.user_identity.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.dto.AuditLogDTO;
import com.commerce_pro_backend.user_identity.dto.CreateUserRequest;
import com.commerce_pro_backend.user_identity.dto.ImpersonationToken;
import com.commerce_pro_backend.user_identity.dto.RoleAssignmentDTO;
import com.commerce_pro_backend.user_identity.dto.RoleAssignmentRequest;
import com.commerce_pro_backend.user_identity.dto.UpdateUserRequest;
import com.commerce_pro_backend.user_identity.dto.UserDTO;
import com.commerce_pro_backend.user_identity.dto.UserDetailDTO;
import com.commerce_pro_backend.user_identity.entity.Role;
import com.commerce_pro_backend.user_identity.entity.User;
import com.commerce_pro_backend.user_identity.entity.UserRoleAssignment;
import com.commerce_pro_backend.user_identity.enums.AssignmentStatus;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.repository.RoleRepository;
import com.commerce_pro_backend.user_identity.repository.UserRepository;
import com.commerce_pro_backend.user_identity.repository.UserRoleAssignmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public UserDTO createUser(CreateUserRequest request, String adminId) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ApiException.conflict("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Email already exists");
        }

        // Issue 9 FIX: removed manual .id(UUID...) — @GeneratedValue(UUID) handles this
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .isActive(true)
            .isEmailVerified(false)
            .mustChangePassword(true)  // Force password change on first login
            .createdBy(adminId)
            .build();

        User saved = userRepository.save(user);

        // Assign initial roles if specified
        if (request.getInitialRoleCodes() != null) {
            for (String roleCode : request.getInitialRoleCodes()) {
                Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> ApiException.badRequest("Role not found: " + roleCode));
                saved.assignRole(role, adminId, null, null);
            }
        }

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.USER_CREATED, "USER", saved.getId(),
            request.getUsername(), adminUsername, null, "Created user: " + request.getUsername(), true);

        // Issue 10 FIX: send welcome email with temporary password
        emailNotificationService.sendWelcomeEmail(
            saved.getEmail(), saved.getUsername(), request.getPassword());

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> findUsers(Boolean active, String roleCode, String search, Pageable pageable) {
        return userRepository.findWithFilters(active, search, pageable)
            .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public UserDetailDTO getUserDetail(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User", id));
        return mapToDetailDTO(user);
    }

    @Transactional
    public UserDTO updateUser(String id, UpdateUserRequest request, String adminId) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User", id));

        if (request.getEmail() != null
                && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Email already exists");
        }

        String oldValue = String.format("email=%s, firstName=%s, lastName=%s, active=%s",
            user.getEmail(), user.getFirstName(), user.getLastName(), user.getIsActive());

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getActive() != null) user.setIsActive(request.getActive());
        if (request.getMfaEnabled() != null) user.setMfaEnabled(request.getMfaEnabled());

        user.setUpdatedBy(adminId);
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        String newValue = String.format("email=%s, firstName=%s, lastName=%s, active=%s",
            saved.getEmail(), saved.getFirstName(), saved.getLastName(), saved.getIsActive());

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.USER_UPDATED, "USER", id,
            saved.getUsername(), adminUsername, oldValue, newValue, true);

        return mapToDTO(saved);
    }

    @Transactional
    public void deleteUser(String id, String adminId, String reason) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User", id));

        user.setIsActive(false);
        user.setUpdatedBy(adminId);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.USER_DELETED, "USER", id,
            user.getUsername(), adminUsername, null, "Reason: " + reason, true);
    }

    @Transactional
    public void activateUser(String id, String adminId) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User", id));
        user.setIsActive(true);
        user.setUpdatedBy(adminId);
        userRepository.save(user);

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.USER_ACTIVATED, "USER", id,
            user.getUsername(), adminUsername, null, null, true);
    }

    @Transactional
    public void deactivateUser(String id, String adminId, String reason) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User", id));
        user.setIsActive(false);
        user.setUpdatedBy(adminId);
        userRepository.save(user);

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.USER_DEACTIVATED, "USER", id,
            user.getUsername(), adminUsername, null, "Reason: " + reason, true);
    }

    @Transactional
    public void resetPassword(String id, String adminId, boolean notifyUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User", id));

        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setUpdatedBy(adminId);
        userRepository.save(user);

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.PASSWORD_RESET, "USER", id,
            user.getUsername(), adminUsername, null, "Password reset by admin", true);

        // Issue 10 FIX: Send email with temp password
        if (notifyUser) {
            emailNotificationService.sendPasswordResetNotification(
                user.getEmail(), user.getUsername(), tempPassword);
        }
    }

    @Transactional
    public void unlockAccount(String id, String adminId) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("User", id));
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        user.setUpdatedBy(adminId);
        userRepository.save(user);

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.USER_UNLOCKED, "USER", id,
            user.getUsername(), adminUsername, null, null, true);
    }

    @Transactional
    public void assignRole(String userId, RoleAssignmentRequest request, String adminId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User", userId));

        Role role = roleRepository.findByCode(request.getRoleCode())
            .orElseThrow(() -> ApiException.badRequest("Role not found: " + request.getRoleCode()));

        // Issue 6 FIX: Prevent duplicate active role assignments
        boolean alreadyAssigned = user.getRoleAssignments().stream()
            .anyMatch(a -> a.getRole().getCode().equals(request.getRoleCode())
                       && a.getStatus() == AssignmentStatus.ACTIVE);
        if (alreadyAssigned) {
            throw ApiException.conflict("Role " + request.getRoleCode() + " is already actively assigned to this user");
        }

        LocalDateTime validFrom = parseIsoDateTimeOrNull(request.getValidFrom(), "validFrom");
        LocalDateTime validUntil = parseIsoDateTimeOrNull(request.getValidUntil(), "validUntil");

        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw ApiException.badRequest("validUntil must be after validFrom");
        }

        user.assignRole(role, adminId, validFrom, validUntil);
        userRepository.save(user);

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.ROLE_ASSIGNED, "USER", userId,
            user.getUsername(), adminUsername, null, "Assigned role: " + role.getCode(), true);
    }

    @Transactional
    public void revokeRole(String userId, String assignmentId, String adminId, String reason) {
        UserRoleAssignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> ApiException.notFound("User role assignment", assignmentId));

        if (!assignment.getUser().getId().equals(userId)) {
            throw ApiException.badRequest("Assignment does not belong to user: " + userId);
        }

        assignment.setStatus(AssignmentStatus.REVOKED);
        assignment.setRevokedBy(adminId);
        assignment.setRevokedAt(LocalDateTime.now());
        assignment.setRevocationReason(reason);
        assignmentRepository.save(assignment);

        String adminUsername = resolveUsername(adminId);
        auditService.log(adminId, AuditAction.ROLE_REVOKED, "USER", userId,
            assignment.getUser().getUsername(), adminUsername, null,
            "Revoked role: " + assignment.getRole().getCode() + ". Reason: " + reason, true);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getUserAuditHistory(String userId, Pageable pageable) {
        return auditService.getUserLogs(userId, pageable);
    }

    @Transactional
    public ImpersonationToken startImpersonation(String targetUserId, String adminId) {
        User target = userRepository.findById(targetUserId)
            .orElseThrow(() -> ApiException.notFound("User", targetUserId));

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> ApiException.notFound("Admin", adminId));

        if (target.getId().equals(adminId)) {
            throw ApiException.badRequest("Cannot impersonate self");
        }

        String token = tokenProvider.generateImpersonationToken(
            admin.getUsername(), targetUserId, target.getUsername());

        auditService.log(adminId, AuditAction.USER_IMPERSONATED, "USER", targetUserId,
            target.getUsername(), admin.getUsername(), null, "Started impersonation session", true);

        return ImpersonationToken.builder()
            .token(token)
            .targetUserId(targetUserId)
            .targetUsername(target.getUsername())
            .originalAdminId(adminId)
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolve admin username from their ID for audit log snapshots.
     * Falls back to the ID string if the user cannot be found.
     */
    private String resolveUsername(String userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
            .map(User::getUsername)
            .orElse(userId);
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .active(user.getIsActive())
            .emailVerified(user.getIsEmailVerified())
            .mfaEnabled(user.getMfaEnabled())
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .roleCodes(user.getActiveRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet()))
            .build();
    }

    private UserDetailDTO mapToDetailDTO(User user) {
        UserDetailDTO dto = new UserDetailDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setActive(user.getIsActive());
        dto.setEmailVerified(user.getIsEmailVerified());
        dto.setMfaEnabled(user.getMfaEnabled());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRoleCodes(user.getActiveRoles().stream()
            .map(Role::getCode)
            .collect(Collectors.toSet()));
        
        // Detail fields
        dto.setMustChangePassword(user.getMustChangePassword());
        dto.setFailedLoginAttempts(user.getFailedLoginAttempts());
        dto.setCreatedBy(user.getCreatedBy());
        
        dto.setRoleAssignments(user.getRoleAssignments().stream()
            .map(this::mapToAssignmentDTO)
            .collect(Collectors.toList()));
        
        return dto;
    }

    private RoleAssignmentDTO mapToAssignmentDTO(UserRoleAssignment assignment) {
        return RoleAssignmentDTO.builder()
            .assignmentId(assignment.getId())
            .roleId(assignment.getRole().getId())
            .roleCode(assignment.getRole().getCode())
            .roleName(assignment.getRole().getName())
            .status(assignment.getStatus().name())
            .validFrom(assignment.getValidFrom())
            .validUntil(assignment.getValidUntil())
            .assignedBy(assignment.getAssignedBy())
            .assignedAt(assignment.getAssignedAt())
            .scopeContext(assignment.getScopeContext())
            .build();
    }

    private String generateTempPassword() {
        // Use UUID-based secure random — sufficient for a one-time temp password
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private LocalDateTime parseIsoDateTimeOrNull(String value, String fieldName) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest("Invalid date format for " + fieldName + ". Expected ISO_DATE_TIME");
        }
    }
}
