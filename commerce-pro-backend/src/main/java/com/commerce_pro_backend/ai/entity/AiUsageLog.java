package com.commerce_pro_backend.ai.entity;

import com.commerce_pro_backend.ai.enums.AiFeatureType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AiUsageLog — one record per Groq API call.
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

    /** Link to the AiInsight produced by this call (if applicable) */
    @Column(name = "insight_id", length = 36)
    private String insightId;

    /** Link to the AiConversation this call belongs to (chatbot / NL reports) */
    @Column(name = "conversation_id", length = 36)
    private String conversationId;

    // ── Token counts ──────────────────────────────────────────────────────────
    @Column(name = "tokens_input")
    private Integer tokensInput;

    @Column(name = "tokens_output")
    private Integer tokensOutput;

    // ── Cost (USD) ────────────────────────────────────────────────────────────
    @Column(name = "cost_input_usd", precision = 12, scale = 8)
    private BigDecimal costInputUsd;

    @Column(name = "cost_output_usd", precision = 12, scale = 8)
    private BigDecimal costOutputUsd;

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

    /** HTTP status returned by the Groq API (200, 429, 500, etc.) */
    @Column(name = "http_status")
    private Integer httpStatus;
}
