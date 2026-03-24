package com.commerce_pro_backend.ai.entity;

import com.commerce_pro_backend.ai.enums.AiFeatureType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AiInsight — persists every AI-generated decision, score, and reasoning.
 *
 * Used for:
 *   - Audit trail (regulatory compliance)
 *   - Feeding prior decisions as context into future prompts
 *   - Cost tracking (tokensUsed per call)
 *   - Dashboards (fraud score trends, churn risk distribution, etc.)
 */
@Entity
@Table(name = "ai_insights", indexes = {
    @Index(name = "idx_ai_insight_feature",  columnList = "feature_type"),
    @Index(name = "idx_ai_insight_entity",   columnList = "entity_id, entity_type"),
    @Index(name = "idx_ai_insight_risk",     columnList = "risk_level"),
    @Index(name = "idx_ai_insight_score",    columnList = "score"),
    @Index(name = "idx_ai_insight_created",  columnList = "created_at"),
    @Index(name = "idx_ai_insight_status",   columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsight {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    // ── Feature identity ──────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 50)
    private AiFeatureType featureType;

    /** The entity this insight is about (transactionId, customerId, productId, etc.) */
    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    /** TRANSACTION | CUSTOMER | PRODUCT | ORDER | VENDOR | SHIPMENT | BUDGET */
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    // ── AI output ─────────────────────────────────────────────────────────────
    /**
     * Numeric score 0–100. Semantics depend on feature:
     *   Fraud:     0=clean,   100=certain fraud
     *   Churn:     0=loyal,   100=certain churn
     *   Vendor:    0=poor,    100=excellent
     *   Sentiment: -100=very negative, 100=very positive
     */
    @Column(name = "score")
    private Integer score;

    /** LOW | MEDIUM | HIGH | CRITICAL | POSITIVE | NEGATIVE | NEUTRAL */
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    /** APPROVE | REVIEW | BLOCK | HOLD | REORDER | DISCOUNT | ESCALATE | ... */
    @Column(name = "recommendation", length = 50)
    private String recommendation;

    /** Full plain-English reasoning from the model (for audit + admin display) */
    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    /** Structured JSON output from the model (raw, for programmatic use) */
    @Column(name = "raw_output", columnDefinition = "TEXT")
    private String rawOutput;

    /** Comma-separated key signals (e.g. "geo_mismatch,velocity_exceeded") */
    @Column(name = "signals", length = 500)
    private String signals;

    // ── Status & lifecycle ────────────────────────────────────────────────────
    /** PENDING | PROCESSED | EXPIRED | ERROR */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PROCESSED";

    /**
     * Human override status.
     * PENDING_REVIEW | ACCEPTED | REJECTED | OVERRIDDEN
     */
    @Column(name = "review_status", length = 20)
    @Builder.Default
    private String reviewStatus = "PENDING_REVIEW";

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    // ── Model metadata ────────────────────────────────────────────────────────
    @Column(name = "model_used", length = 60)
    private String modelUsed;

    @Column(name = "tokens_input")
    private Integer tokensInput;

    @Column(name = "tokens_output")
    private Integer tokensOutput;

    /** Estimated cost in USD (computed at persist time) */
    @Column(name = "estimated_cost_usd", precision = 12, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(name = "latency_ms")
    private Long latencyMs;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /** null = keep forever */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
