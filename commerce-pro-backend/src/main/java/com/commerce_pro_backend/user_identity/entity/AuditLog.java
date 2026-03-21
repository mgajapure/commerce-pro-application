package com.commerce_pro_backend.user_identity.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.commerce_pro_backend.user_identity.enums.AuditAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @Column(name = "actor_id", nullable = false)
    private String actorId;  // Who performed the action

    @Column(name = "actor_username", length = 50)
    private String actorUsername;  // Snapshot of username at time of action

    @Column(name = "actor_role")
    private String actorRole;  // Role at time of action

    @Column(name = "target_type", nullable = false)
    private String targetType;  // USER, ROLE, PERMISSION, etc.

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "target_identifier")  // Human-readable (username, role code)
    private String targetIdentifier;

    @Column(name = "action_description", length = 1000)
    private String actionDescription;

    @Column(name = "old_value", length = 4000)
    private String oldValue;  // JSON of previous state

    @Column(name = "new_value", length = 4000)
    private String newValue;  // JSON of new state

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "request_id")
    private String requestId;  // For tracing across services

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
}