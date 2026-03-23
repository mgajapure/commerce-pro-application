package com.commerce_pro_backend.ai.dto.response;

import com.commerce_pro_backend.ai.enums.AiFeatureType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Live counters snapshot served by GET /api/v1/admin/ai/dashboard.
 * All spend figures are in-memory (reset at midnight); call counts
 * are pulled from AiUsageLogRepository for the current calendar day.
 */
public record AiDashboardResponse(

    /** Current global daily spend in USD. */
    BigDecimal globalDailySpendUsd,

    /** Configured global daily spend cap in USD. */
    BigDecimal globalDailyCap,

    /** Per-feature spend breakdown for today (USD). */
    Map<AiFeatureType, BigDecimal> featureSpendUsd,

    /** Total successful calls in the last 24 h. */
    long successfulCallsLast24h,

    /** Total failed calls in the last 24 h. */
    long failedCallsLast24h,

    /** Number of slow calls (>3 s) in the last 24 h. */
    long slowCallsLast24h
) {}
