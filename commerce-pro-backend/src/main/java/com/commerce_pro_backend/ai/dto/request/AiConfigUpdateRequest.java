package com.commerce_pro_backend.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Partial-update payload for PATCH /api/v1/admin/ai/configs/{feature}.
 * All fields are optional — only non-null values are applied.
 */
public record AiConfigUpdateRequest(

    /** Toggle the feature on / off. */
    Boolean enabled,

    /** Groq model ID to use for this feature. */
    String model,

    /** Maximum tokens for a single completion. */
    @Positive
    @Max(8192)
    Integer maxTokens,

    /** Daily spend cap in USD — null removes the per-feature cap. */
    @Positive
    BigDecimal dailyBudgetUsd,

    /** Monthly spend cap in USD — null removes the per-feature cap. */
    @Positive
    BigDecimal monthlyBudgetUsd,

    /** Max API calls per minute (sliding window). */
    @Min(1)
    @Max(1000)
    Integer rateLimitPerMinute,

    /** Max API calls per hour (sliding window). */
    @Min(1)
    @Max(10000)
    Integer rateLimitPerHour,

    /** Fallback strategy when AI is unavailable: SKIP | DEFAULT_SCORE | QUEUE_RETRY */
    String fallbackStrategy,

    /** Fallback score (0–100) used when fallbackStrategy = DEFAULT_SCORE. */
    @Min(0)
    @Max(100)
    Integer defaultScore
) {}
