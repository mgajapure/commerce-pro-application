package com.commerce_pro_backend.ai.dto.response;

import com.commerce_pro_backend.ai.entity.AiConfig;
import com.commerce_pro_backend.ai.enums.AiFeatureType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read model for a single {@link AiConfig} row, returned to admin callers. */
public record AiConfigResponse(
    String id,
    AiFeatureType featureType,
    String model,
    Integer maxTokens,
    Boolean enabled,
    BigDecimal dailyBudgetUsd,
    BigDecimal monthlyBudgetUsd,
    Integer rateLimitPerMinute,
    Integer rateLimitPerHour,
    String fallbackStrategy,
    Integer defaultScore,
    LocalDateTime updatedAt,
    String updatedBy
) {
    public static AiConfigResponse from(AiConfig c) {
        return new AiConfigResponse(
            c.getId(),
            c.getFeatureType(),
            c.getModel(),
            c.getMaxTokens(),
            c.getEnabled(),
            c.getDailyBudgetUsd(),
            c.getMonthlyBudgetUsd(),
            c.getRateLimitPerMinute(),
            c.getRateLimitPerHour(),
            c.getFallbackStrategy(),
            c.getDefaultScore(),
            c.getUpdatedAt(),
            c.getUpdatedBy()
        );
    }
}
