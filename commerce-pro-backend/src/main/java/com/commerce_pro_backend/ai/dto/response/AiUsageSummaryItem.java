package com.commerce_pro_backend.ai.dto.response;

import com.commerce_pro_backend.ai.enums.AiFeatureType;

import java.math.BigDecimal;

/**
 * One row of the cost/token breakdown returned by the usage-summary endpoint.
 * Mapped from the {@code Object[]} projection in {@code AiUsageLogRepository#costSummaryByFeature}.
 */
public record AiUsageSummaryItem(
    AiFeatureType featureType,
    BigDecimal totalCostUsd,
    Long totalTokensInput,
    Long totalTokensOutput,
    Long callCount,
    Double avgLatencyMs
) {
    /** Factory from the raw {@code Object[]} row returned by JPQL aggregation. */
    public static AiUsageSummaryItem fromRow(Object[] row) {
        return new AiUsageSummaryItem(
            (AiFeatureType) row[0],
            row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
            row[2] != null ? ((Number) row[2]).longValue() : 0L,
            row[3] != null ? ((Number) row[3]).longValue() : 0L,
            row[4] != null ? ((Number) row[4]).longValue() : 0L,
            row[5] != null ? ((Number) row[5]).doubleValue() : 0.0
        );
    }
}
