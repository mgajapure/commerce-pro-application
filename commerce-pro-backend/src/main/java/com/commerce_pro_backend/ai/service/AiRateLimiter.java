package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiConfig;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.exception.AiRateLimitException;
import com.commerce_pro_backend.ai.repository.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AiRateLimiter — per-feature sliding-window rate limiting.
 *
 * Two windows are maintained per feature:
 *   - per-minute  (last 60 s)
 *   - per-hour    (last 3 600 s)
 *
 * Limits are read from the {@link AiConfig} entity so they can be changed at
 * runtime without restarting the application.
 */
@Service
@RequiredArgsConstructor
public class AiRateLimiter {

    private final AiConfigRepository configRepo;

    private final Map<AiFeatureType, Deque<Long>> minuteWindows = new ConcurrentHashMap<>();
    private final Map<AiFeatureType, Deque<Long>> hourWindows   = new ConcurrentHashMap<>();

    private static final long ONE_MINUTE_MS = 60_000L;
    private static final long ONE_HOUR_MS   = 3_600_000L;

    /**
     * Verify that the given feature is within its rate limits, then record the call.
     *
     * @throws AiRateLimitException if the per-minute or per-hour limit is exceeded
     */
    public synchronized void checkAndIncrement(AiFeatureType feature) {
        AiConfig config = configRepo.findByFeatureType(feature).orElseThrow();
        long now = System.currentTimeMillis();

        // ── Per-minute window ────────────────────────────────────────────────
        Deque<Long> minuteWindow = minuteWindows.computeIfAbsent(feature, k -> new ArrayDeque<>());
        evictOlderThan(minuteWindow, now - ONE_MINUTE_MS);
        if (minuteWindow.size() >= config.getRateLimitPerMinute()) {
            throw new AiRateLimitException(feature + " rate limit exceeded: "
                    + config.getRateLimitPerMinute() + " calls/min");
        }

        // ── Per-hour window ──────────────────────────────────────────────────
        Deque<Long> hourWindow = hourWindows.computeIfAbsent(feature, k -> new ArrayDeque<>());
        evictOlderThan(hourWindow, now - ONE_HOUR_MS);
        if (hourWindow.size() >= config.getRateLimitPerHour()) {
            throw new AiRateLimitException(feature + " hourly rate limit exceeded: "
                    + config.getRateLimitPerHour() + " calls/hr");
        }

        // Record the call in both windows
        minuteWindow.addLast(now);
        hourWindow.addLast(now);
    }

    private void evictOlderThan(Deque<Long> window, long cutoff) {
        while (!window.isEmpty() && window.peekFirst() < cutoff) {
            window.pollFirst();
        }
    }
}
