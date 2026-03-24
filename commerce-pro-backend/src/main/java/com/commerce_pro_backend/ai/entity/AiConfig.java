package com.commerce_pro_backend.ai.entity;

import com.commerce_pro_backend.ai.enums.AiFeatureType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AiConfig — per-feature runtime configuration.
 *
 * One record per AiFeatureType. Loaded at startup and cached in-memory.
 * Admins can change settings via /api/v1/admin/ai/config without restarting.
 */
@Entity
@Table(name = "ai_configs", indexes = {
    @Index(name = "idx_ai_cfg_feature", columnList = "feature_type", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfig {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, unique = true, length = 50)
    private AiFeatureType featureType;

    // ── Model selection ───────────────────────────────────────────────────────
    @Column(name = "model", nullable = false, length = 60)
    private String model;

    @Column(name = "max_tokens", nullable = false)
    @Builder.Default
    private Integer maxTokens = 1024;

    // ── Feature toggle ────────────────────────────────────────────────────────
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    // ── Budget controls ───────────────────────────────────────────────────────
    // Daily spend cap in USD — feature auto-disables if exceeded
    @Column(name = "daily_budget_usd", precision = 10, scale = 4)
    private BigDecimal dailyBudgetUsd;

    // Monthly spend cap in USD
    @Column(name = "monthly_budget_usd", precision = 10, scale = 4)
    private BigDecimal monthlyBudgetUsd;

    // ── Rate limiting ─────────────────────────────────────────────────────────
    // Max API calls per minute for this feature
    @Column(name = "rate_limit_per_minute")
    @Builder.Default
    private Integer rateLimitPerMinute = 60;

    // Max API calls per hour for this feature
    @Column(name = "rate_limit_per_hour")
    @Builder.Default
    private Integer rateLimitPerHour = 1000;

    // ── Caching ───────────────────────────────────────────────────────────────
    @Column(name = "prompt_cache_enabled", nullable = false)
    @Builder.Default
    private Boolean promptCacheEnabled = true;

    // ── Fallback behaviour ────────────────────────────────────────────────────
    // What to do when AI is unavailable: SKIP | DEFAULT_SCORE | QUEUE_RETRY
    @Column(name = "fallback_strategy", length = 20)
    @Builder.Default
    private String fallbackStrategy = "SKIP";

    @Column(name = "default_score")
    @Builder.Default
    private Integer defaultScore = 50;   // used when fallback = DEFAULT_SCORE

    // ── Audit ─────────────────────────────────────────────────────────────────
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
