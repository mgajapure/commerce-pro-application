package com.commerce_pro_backend.ai.dto.response;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AiInsightResponse — read-only projection of an {@link AiInsight} for the admin UI.
 */
public record AiInsightResponse(
        String          id,
        AiFeatureType   featureType,
        String          entityId,
        String          entityType,
        Integer         score,
        String          riskLevel,
        String          recommendation,
        String          reasoning,
        String          signals,
        String          status,
        String          reviewStatus,
        String          modelUsed,
        Integer         tokensInput,
        Integer         tokensOutput,
        BigDecimal      estimatedCostUsd,
        Long            latencyMs,
        LocalDateTime   createdAt
) {
    public static AiInsightResponse from(AiInsight insight) {
        return new AiInsightResponse(
                insight.getId(),
                insight.getFeatureType(),
                insight.getEntityId(),
                insight.getEntityType(),
                insight.getScore(),
                insight.getRiskLevel(),
                insight.getRecommendation(),
                insight.getReasoning(),
                insight.getSignals(),
                insight.getStatus(),
                insight.getReviewStatus(),
                insight.getModelUsed(),
                insight.getTokensInput(),
                insight.getTokensOutput(),
                insight.getEstimatedCostUsd(),
                insight.getLatencyMs(),
                insight.getCreatedAt()
        );
    }
}
