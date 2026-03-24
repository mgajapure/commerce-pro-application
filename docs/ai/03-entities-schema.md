# AI Entities & Database Schema

Four new JPA entities required to support all 15 AI features.

---

## Overview

| Entity | Table | Purpose |
|--------|-------|---------|
| `AiInsight` | `ai_insights` | Long-term store of every AI decision, score, and reasoning |
| `AiConversation` | `ai_conversations` | Multi-turn conversation history (chatbot, NL reports) |
| `AiUsageLog` | `ai_usage_logs` | Per-call token counts, latency, cost tracking |
| `AiConfig` | `ai_configs` | Per-feature runtime configuration (model, budget, toggle) |

---

## Entity 1: AiInsight

Stores every AI decision permanently. This is the audit trail and the source of historical context fed back into future prompts.

### Full Java Definition
```java
package com.commerce_pro_backend.ai.entity;

import com.commerce_pro_backend.ai.enums.AiFeatureType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

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
    @Index(name = "idx_ai_insight_feature",     columnList = "feature_type"),
    @Index(name = "idx_ai_insight_entity",      columnList = "entity_id, entity_type"),
    @Index(name = "idx_ai_insight_risk",        columnList = "risk_level"),
    @Index(name = "idx_ai_insight_score",       columnList = "score"),
    @Index(name = "idx_ai_insight_created",     columnList = "created_at"),
    @Index(name = "idx_ai_insight_status",      columnList = "status")
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

    // The entity this insight is about (transactionId, customerId, productId, etc.)
    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    // TRANSACTION | CUSTOMER | PRODUCT | ORDER | VENDOR | SHIPMENT | BUDGET
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    // ── AI output ─────────────────────────────────────────────────────────────
    /**
     * Numeric score 0–100. Semantics depend on feature:
     * Fraud:   0=clean, 100=certain fraud
     * Churn:   0=loyal, 100=certain churn
     * Vendor:  0=poor, 100=excellent
     * Sentiment: -100=very negative, 100=very positive
     */
    @Column(name = "score")
    private Integer score;

    // LOW | MEDIUM | HIGH | CRITICAL | POSITIVE | NEGATIVE | NEUTRAL
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    // APPROVE | REVIEW | BLOCK | HOLD | REORDER | DISCOUNT | ESCALATE | ...
    @Column(name = "recommendation", length = 50)
    private String recommendation;

    // Full plain-English reasoning from Claude (for audit + admin display)
    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    // Structured JSON output from Claude (raw, for programmatic use)
    @Column(name = "raw_output", columnDefinition = "TEXT")
    private String rawOutput;

    // Comma-separated key signals (e.g., "geo_mismatch,velocity_exceeded")
    @Column(name = "signals", length = 500)
    private String signals;

    // ── Status & lifecycle ────────────────────────────────────────────────────
    // PENDING | PROCESSED | EXPIRED | ERROR
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PROCESSED";

    // Human override: was this AI decision accepted, rejected, or overridden?
    // PENDING_REVIEW | ACCEPTED | REJECTED | OVERRIDDEN
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

    @Column(name = "tokens_cache_read")
    private Integer tokensCacheRead;

    @Column(name = "tokens_cache_write")
    private Integer tokensCacheWrite;

    // Computed at persist time: input_cost + output_cost - cache_savings (in USD, 6 decimal places)
    @Column(name = "estimated_cost_usd", precision = 12, scale = 6)
    private java.math.BigDecimal estimatedCostUsd;

    @Column(name = "latency_ms")
    private Long latencyMs;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;  // null = keep forever
}
```

### Table: ai_insights (DDL equivalent)
```sql
CREATE TABLE ai_insights (
    id                  VARCHAR(36)     PRIMARY KEY,
    feature_type        VARCHAR(50)     NOT NULL,
    entity_id           VARCHAR(36)     NOT NULL,
    entity_type         VARCHAR(30)     NOT NULL,
    score               INTEGER,
    risk_level          VARCHAR(20),
    recommendation      VARCHAR(50),
    reasoning           TEXT,
    raw_output          TEXT,
    signals             VARCHAR(500),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PROCESSED',
    review_status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by         VARCHAR(36),
    reviewed_at         TIMESTAMP,
    review_notes        VARCHAR(500),
    model_used          VARCHAR(60),
    tokens_input        INTEGER,
    tokens_output       INTEGER,
    tokens_cache_read   INTEGER,
    tokens_cache_write  INTEGER,
    estimated_cost_usd  DECIMAL(12,6),
    latency_ms          BIGINT,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          TIMESTAMP
);

CREATE INDEX idx_ai_insight_feature  ON ai_insights(feature_type);
CREATE INDEX idx_ai_insight_entity   ON ai_insights(entity_id, entity_type);
CREATE INDEX idx_ai_insight_risk     ON ai_insights(risk_level);
CREATE INDEX idx_ai_insight_score    ON ai_insights(score);
CREATE INDEX idx_ai_insight_created  ON ai_insights(created_at);
```

### Repository
```java
public interface AiInsightRepository extends JpaRepository<AiInsight, String> {

    // Load history for a specific entity to inject into future prompts
    List<AiInsight> findByEntityIdAndFeatureTypeOrderByCreatedAtDesc(
        String entityId, AiFeatureType featureType, Pageable pageable);

    // Dashboard queries
    List<AiInsight> findByFeatureTypeAndRiskLevelAndCreatedAtAfter(
        AiFeatureType featureType, String riskLevel, LocalDateTime since);

    // Cost reporting
    @Query("SELECT SUM(a.estimatedCostUsd) FROM AiInsight a WHERE a.createdAt BETWEEN :from AND :to")
    BigDecimal sumCostBetween(LocalDateTime from, LocalDateTime to);

    // Cleanup expired insights
    int deleteByExpiresAtBefore(LocalDateTime cutoff);
}
```

---

## Entity 2: AiConversation

Stores full multi-turn conversation histories for the Support Chatbot and NL Reports features.

### Full Java Definition
```java
package com.commerce_pro_backend.ai.entity;

import com.commerce_pro_backend.ai.enums.SessionType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AiConversation — stores multi-turn AI conversation history.
 *
 * The full turn history is serialised as JSON in the `turns_json` column.
 * On load, it is deserialised into a List<ConversationTurn>.
 *
 * Used by:
 *   - Support Chatbot (SUPPORT_CHAT)
 *   - Natural Language Reports (NL_REPORT)
 */
@Entity
@Table(name = "ai_conversations", indexes = {
    @Index(name = "idx_ai_conv_session_type",  columnList = "session_type"),
    @Index(name = "idx_ai_conv_customer",      columnList = "customer_id"),
    @Index(name = "idx_ai_conv_user",          columnList = "user_id"),
    @Index(name = "idx_ai_conv_expires",       columnList = "expires_at"),
    @Index(name = "idx_ai_conv_last_active",   columnList = "last_active_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversation {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    // SUPPORT_CHAT | NL_REPORT
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    // Who owns this conversation
    @Column(name = "customer_id", length = 36)
    private String customerId;          // for SUPPORT_CHAT

    @Column(name = "user_id", length = 36)
    private String userId;              // admin user for NL_REPORT

    // ── Conversation state ────────────────────────────────────────────────────
    /**
     * Full turn history stored as JSON array.
     * Each element: { "userMessage": "...", "assistantMessage": "...", "timestamp": "..." }
     * Use getTurnsAsList() / addTurn() for access — never manipulate the raw JSON directly.
     */
    @Column(name = "turns_json", columnDefinition = "TEXT")
    @Builder.Default
    private String turnsJson = "[]";

    @Column(name = "turn_count", nullable = false)
    @Builder.Default
    private Integer turnCount = 0;

    // ── Status ────────────────────────────────────────────────────────────────
    // ACTIVE | ESCALATED | RESOLVED | EXPIRED
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ── Metadata ──────────────────────────────────────────────────────────────
    @Column(name = "total_tokens_used")
    @Builder.Default
    private Integer totalTokensUsed = 0;

    // For NL Reports — the SavedReport ID if this session was saved
    @Column(name = "linked_report_id", length = 36)
    private String linkedReportId;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_active_at")
    @Builder.Default
    private LocalDateTime lastActiveAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // ── Business methods ──────────────────────────────────────────────────────

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .findAndRegisterModules();

    public List<ConversationTurn> getTurnsAsList() {
        try {
            return MAPPER.readValue(turnsJson, new TypeReference<List<ConversationTurn>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void addTurn(String userMessage, String assistantMessage) {
        List<ConversationTurn> turns = getTurnsAsList();
        turns.add(new ConversationTurn(userMessage, assistantMessage, LocalDateTime.now().toString()));
        try {
            this.turnsJson = MAPPER.writeValueAsString(turns);
            this.turnCount = turns.size();
            this.lastActiveAt = LocalDateTime.now();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise conversation turn", e);
        }
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public record ConversationTurn(String userMessage, String assistantMessage, String timestamp) {}
}
```

### Table: ai_conversations (DDL equivalent)
```sql
CREATE TABLE ai_conversations (
    id                  VARCHAR(36)     PRIMARY KEY,
    session_type        VARCHAR(20)     NOT NULL,
    customer_id         VARCHAR(36),
    user_id             VARCHAR(36),
    turns_json          TEXT            DEFAULT '[]',
    turn_count          INTEGER         NOT NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    escalated_at        TIMESTAMP,
    resolved_at         TIMESTAMP,
    total_tokens_used   INTEGER         DEFAULT 0,
    linked_report_id    VARCHAR(36),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    expires_at          TIMESTAMP
);
```

### Repository
```java
public interface AiConversationRepository extends JpaRepository<AiConversation, String> {

    // Load active chatbot session for a customer
    Optional<AiConversation> findByCustomerIdAndSessionTypeAndStatus(
        String customerId, SessionType sessionType, String status);

    // Cleanup expired conversations (nightly job)
    int deleteByExpiresAtBefore(LocalDateTime cutoff);

    // Load all active NL report sessions for a user
    List<AiConversation> findByUserIdAndSessionTypeAndStatusOrderByLastActiveAtDesc(
        String userId, SessionType sessionType, String status);
}
```

---

## Entity 3: AiUsageLog

Fine-grained per-API-call log. Used for cost reporting, debugging, and SLA monitoring.

### Full Java Definition
```java
package com.commerce_pro_backend.ai.entity;

import com.commerce_pro_backend.ai.enums.AiFeatureType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AiUsageLog — one record per Anthropic API call.
 *
 * Purpose:
 *   - Cost breakdown by feature, model, and time period
 *   - Latency monitoring and SLA alerting
 *   - Debugging failed or slow calls
 *   - Token usage trends for capacity planning
 */
@Entity
@Table(name = "ai_usage_logs", indexes = {
    @Index(name = "idx_ai_log_feature",  columnList = "feature_type"),
    @Index(name = "idx_ai_log_model",    columnList = "model"),
    @Index(name = "idx_ai_log_called",   columnList = "called_at"),
    @Index(name = "idx_ai_log_success",  columnList = "success"),
    @Index(name = "idx_ai_log_insight",  columnList = "insight_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLog {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 50)
    private AiFeatureType featureType;

    @Column(name = "model", nullable = false, length = 60)
    private String model;

    // Link to the AiInsight produced by this call (if applicable)
    @Column(name = "insight_id", length = 36)
    private String insightId;

    // Link to the AiConversation this call belongs to (for chatbot/NL reports)
    @Column(name = "conversation_id", length = 36)
    private String conversationId;

    // ── Token counts ──────────────────────────────────────────────────────────
    @Column(name = "tokens_input")
    private Integer tokensInput;

    @Column(name = "tokens_output")
    private Integer tokensOutput;

    @Column(name = "tokens_cache_read")
    @Builder.Default
    private Integer tokensCacheRead = 0;

    @Column(name = "tokens_cache_write")
    @Builder.Default
    private Integer tokensCacheWrite = 0;

    // ── Cost (in USD) ─────────────────────────────────────────────────────────
    @Column(name = "cost_input_usd", precision = 12, scale = 8)
    private BigDecimal costInputUsd;

    @Column(name = "cost_output_usd", precision = 12, scale = 8)
    private BigDecimal costOutputUsd;

    @Column(name = "cost_cache_usd", precision = 12, scale = 8)
    @Builder.Default
    private BigDecimal costCacheUsd = BigDecimal.ZERO;

    @Column(name = "total_cost_usd", precision = 12, scale = 8)
    private BigDecimal totalCostUsd;

    // ── Performance ───────────────────────────────────────────────────────────
    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "called_at", nullable = false)
    private LocalDateTime calledAt;

    // ── Status ────────────────────────────────────────────────────────────────
    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // HTTP status from Anthropic API (200, 429, 500, etc.)
    @Column(name = "http_status")
    private Integer httpStatus;

    // Anthropic request ID (for support escalation)
    @Column(name = "anthropic_request_id", length = 100)
    private String anthropicRequestId;
}
```

### Repository
```java
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, String> {

    // Daily cost report per feature
    @Query("""
        SELECT a.featureType, SUM(a.totalCostUsd), SUM(a.tokensInput), SUM(a.tokensOutput),
               COUNT(a), AVG(a.latencyMs)
        FROM AiUsageLog a
        WHERE a.calledAt BETWEEN :from AND :to
        GROUP BY a.featureType
        ORDER BY SUM(a.totalCostUsd) DESC
        """)
    List<Object[]> costSummaryByFeature(LocalDateTime from, LocalDateTime to);

    // Alert on slow calls (> 5 seconds)
    List<AiUsageLog> findByLatencyMsGreaterThanAndCalledAtAfter(Long latencyMs, LocalDateTime since);

    // Error rate monitoring
    long countBySuccessAndCalledAtAfter(Boolean success, LocalDateTime since);
}
```

---

## Entity 4: AiConfig

Runtime configuration for each AI feature. Allows toggling features, changing models, and adjusting daily budgets without redeployment.

### Full Java Definition
```java
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
```

### Default Config Records (seed data)
```java
// DataInitializer or Liquibase migration
List.of(
    AiConfig.builder().featureType(FRAUD_DETECTION)
        .model("claude-haiku-4-5-20251001").maxTokens(512)
        .dailyBudgetUsd(new BigDecimal("5.00")).rateLimitPerMinute(120).build(),

    AiConfig.builder().featureType(DEMAND_FORECAST)
        .model("claude-sonnet-4-6").maxTokens(1024)
        .dailyBudgetUsd(new BigDecimal("2.00")).rateLimitPerMinute(30).build(),

    AiConfig.builder().featureType(CHURN_PREDICTION)
        .model("claude-sonnet-4-6").maxTokens(1024)
        .dailyBudgetUsd(new BigDecimal("3.00")).rateLimitPerMinute(20).build(),

    AiConfig.builder().featureType(NL_REPORT)
        .model("claude-sonnet-4-6").maxTokens(2048)
        .dailyBudgetUsd(new BigDecimal("10.00")).rateLimitPerMinute(30).build(),

    AiConfig.builder().featureType(SUPPORT_CHATBOT)
        .model("claude-sonnet-4-6").maxTokens(1024)
        .dailyBudgetUsd(new BigDecimal("20.00")).rateLimitPerMinute(60).build(),

    AiConfig.builder().featureType(PRODUCT_DESCRIPTION)
        .model("claude-haiku-4-5-20251001").maxTokens(512)
        .dailyBudgetUsd(new BigDecimal("1.00")).rateLimitPerMinute(100).build()

    // ... one per feature type
)
```

---

## Enums

```java
// AiFeatureType.java
public enum AiFeatureType {
    FRAUD_DETECTION,
    DEMAND_FORECAST,
    CHURN_PREDICTION,
    NL_REPORT,
    SUPPORT_CHATBOT,
    PRODUCT_DESCRIPTION,
    SENTIMENT_ANALYSIS,
    PRICING_RECOMMENDATION,
    INVENTORY_OPTIMIZATION,
    BUDGET_ANOMALY,
    SEO_OPTIMIZER,
    MARKETING_PERSONALIZATION,
    RETURN_PATTERN_ANALYSIS,
    VENDOR_ANALYSIS,
    SHIPPING_OPTIMIZATION
}

// SessionType.java
public enum SessionType {
    SUPPORT_CHAT,
    NL_REPORT
}

// InsightStatus.java
public enum InsightStatus {
    PENDING,
    PROCESSED,
    EXPIRED,
    ERROR
}
```

---

## Entity Relationships to Existing Modules

```
PaymentTransaction ─────── (entity_id) ──────── AiInsight (FRAUD_DETECTION)
Customer ──────────────── (entity_id) ──────── AiInsight (CHURN_PREDICTION)
Customer ──────────────── (customer_id) ─────── AiConversation (SUPPORT_CHAT)
Product ───────────────── (entity_id) ──────── AiInsight (PRODUCT_DESCRIPTION)
Product ───────────────── (entity_id) ──────── AiInsight (SENTIMENT_ANALYSIS)
Product ───────────────── (entity_id) ──────── AiInsight (PRICING_RECOMMENDATION)
DemandForecast ─────────── (entity_id) ──────── AiInsight (DEMAND_FORECAST)
SavedReport ────────────── (linked_report_id) ── AiConversation (NL_REPORT)
Vendor ─────────────────── (entity_id) ──────── AiInsight (VENDOR_ANALYSIS)
Shipment ───────────────── (entity_id) ──────── AiInsight (SHIPPING_OPTIMIZATION)
Budget ─────────────────── (entity_id) ──────── AiInsight (BUDGET_ANOMALY)
AiInsight ──────────────── (insight_id) ─────── AiUsageLog
AiConversation ─────────── (conversation_id) ── AiUsageLog
AiConfig ───────────────── (feature_type) ────── AiFeatureType (1:1)
```

---

## Existing Entity Fields Leveraged by AI

| Existing Entity | Field | Used By |
|----------------|-------|---------|
| `PaymentTransaction` | `riskScore`, `isFlagged`, `flagReason` | Fraud Detection (written) |
| `Customer` | `riskScore`, `isFraudFlagged`, `fraudEvidence` | Fraud + Churn (written) |
| `DemandForecast` | `algorithm`, `generatedBy`, all forecast fields | Demand Forecast (written) |
| `Product` | `description` | Product Description (written) |
| `SeoMetadata` | `metaTitle`, `metaDescription`, `keywords` | SEO Optimizer (suggestions) |
| `CustomerCommunicationLog` | Full entity | Chatbot (read for context, writes new log entry) |
| `SavedReport` | `filterParams`, `description` | NL Reports (read + write) |
