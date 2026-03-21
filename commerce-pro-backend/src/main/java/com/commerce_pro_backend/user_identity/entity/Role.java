package com.commerce_pro_backend.user_identity.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.commerce_pro_backend.user_identity.enums.RoleType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;  // SUPER_ADMIN, USER_ADMIN, etc.

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RoleType type;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_super_admin", nullable = false)
    @Builder.Default
    private Boolean isSuperAdmin = false;  // Special flag for SUPER_ADMIN role

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    private Role parentRole;  // For hierarchy

    @OneToMany(mappedBy = "parentRole", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Role> childRoles = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_code")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = "role_constraints",
        joinColumns = @JoinColumn(name = "role_id")
    )
    @Column(name = "constraint_rule")
    @Builder.Default
    private Set<String> constraints = new HashSet<>();  // JSON rules for conditional access

    @Column(name = "max_assignment_duration_days")
    private Integer maxAssignmentDurationDays;  // Null = unlimited

    @Column(name = "requires_mfa", nullable = false)
    @Builder.Default
    private Boolean requiresMfa = false;

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes;  // Override default session timeout

    @Column(name = "allowed_ip_patterns", length = 1000)
    private String allowedIpPatterns;  // CIDR notation, comma-separated

    @Column(name = "time_restrictions", length = 500)
    private String timeRestrictions;  // Cron expression or JSON schedule

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
    public boolean isAssignableBy(Role assignerRole) {
        if (assignerRole.getIsSuperAdmin()) return true;
        // Super admin can assign any role
        // Others can only assign roles below them in hierarchy
        return false; // Simplified - actual logic traverses hierarchy
    }

    public Set<Permission> getAllPermissions() {
        return getAllPermissionsInternal(new java.util.HashSet<>(), 0);
    }

    private Set<Permission> getAllPermissionsInternal(java.util.Set<String> visitedRoleIds, int depth) {
        if (depth > 10) {
            throw new IllegalStateException(
                "Role hierarchy depth exceeds maximum (10) at role: " + code +
                ". Possible cycle in role hierarchy.");
        }
        if (!visitedRoleIds.add(this.id)) {
            // Already visited — cycle detected, stop recursion
            return new HashSet<>();
        }
        Set<Permission> allPerms = new HashSet<>(permissions);
        if (parentRole != null) {
            allPerms.addAll(parentRole.getAllPermissionsInternal(visitedRoleIds, depth + 1));
        }
        return allPerms;
    }
}