package com.commerce_pro_backend.user_identity.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.commerce_pro_backend.user_identity.enums.AssignmentStatus;

import com.commerce_pro_backend.user_identity.config.MfaSecretEncryptor;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Convert(converter = MfaSecretEncryptor.class)
    @Column(name = "mfa_secret", length = 512) // longer to accommodate encrypted+base64 value
    private String mfaSecret;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRoleAssignment> roleAssignments = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Business methods
    public boolean hasSuperAdminRole() {
        return roleAssignments.stream()
            .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
            .anyMatch(assignment -> assignment.getRole().getIsSuperAdmin());
    }

    public Set<Role> getActiveRoles() {
        return roleAssignments.stream()
            .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
            .filter(a -> a.getValidFrom() == null || !a.getValidFrom().isAfter(LocalDateTime.now()))
            .filter(a -> a.getValidUntil() == null || a.getValidUntil().isAfter(LocalDateTime.now()))
            .map(UserRoleAssignment::getRole)
            .collect(java.util.stream.Collectors.toSet());
    }

    public void assignRole(Role role, String assignedBy, LocalDateTime validFrom, LocalDateTime validUntil) {
        UserRoleAssignment assignment = UserRoleAssignment.builder()
            .user(this)
            .role(role)
            .assignedBy(assignedBy)
            .assignedAt(LocalDateTime.now())
            .validFrom(validFrom)
            .validUntil(validUntil)
            .status(AssignmentStatus.ACTIVE)
            .build();
        roleAssignments.add(assignment);
    }
}