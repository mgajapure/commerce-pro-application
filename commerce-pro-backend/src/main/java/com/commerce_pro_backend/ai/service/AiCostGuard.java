package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.config.AiModuleConfig;
import com.commerce_pro_backend.ai.entity.AiConfig;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.exception.AiBudgetExceededException;
import com.commerce_pro_backend.ai.repository.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AiCostGuard — enforces daily spend limits at both the global and per-feature level.
 *
 * Counters are held in memory and reset at midnight by {@link #resetDailyCounters()}.
 * On application restart the counters start at zero (acceptable — worst case is a single
 * day's worth of spend is under-counted after a restart).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiCostGuard {

    private final AiConfigRepository configRepo;
    private final AiModuleConfig moduleConfig;

    /** Running per-feature daily spend totals (USD). */
    private final Map<AiFeatureType, BigDecimal> dailySpend = new ConcurrentHashMap<>();

    /** Running global daily spend total (USD). */
    private volatile BigDecimal globalDailySpend = BigDecimal.ZERO;

    /**
     * Check whether this feature is still within its budget before making an API call.
     *
     * @throws AiBudgetExceededException if the global cap or the per-feature cap is exhausted
     */
    public void checkDailyBudget(AiFeatureType feature) {
        // 1. Global cap
        BigDecimal globalCap = moduleConfig.getBudget().getDailyTotalUsd();
        if (globalDailySpend.compareTo(globalCap) >= 0) {
            log.error("Global daily AI budget exhausted: ${} / ${}", globalDailySpend, globalCap);
            throw new AiBudgetExceededException(
                    "Global daily AI budget of $" + globalCap + " exceeded");
        }

        // 2. Per-feature cap (optional — null means no per-feature limit)
        AiConfig config = configRepo.findByFeatureType(feature).orElseThrow();
        BigDecimal featureCap = config.getDailyBudgetUsd();
        if (featureCap != null) {
            BigDecimal featureSpend = dailySpend.getOrDefault(feature, BigDecimal.ZERO);
            if (featureSpend.compareTo(featureCap) >= 0) {
                log.warn("Feature {} daily budget exhausted: ${} / ${}", feature, featureSpend, featureCap);
                throw new AiBudgetExceededException(feature + " daily budget of $" + featureCap + " exceeded");
            }
        }
    }

    /**
     * Record spend after a successful API call.
     * Called by the orchestrator once the response (and token counts) are available.
     */
    public synchronized void recordSpend(AiFeatureType feature, BigDecimal cost) {
        dailySpend.merge(feature, cost, BigDecimal::add);
        globalDailySpend = globalDailySpend.add(cost);
        log.debug("AI spend recorded — feature={}, cost=${}, globalTotal=${}", feature, cost, globalDailySpend);
    }

    /** Reset all counters at midnight every day. */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyCounters() {
        dailySpend.clear();
        globalDailySpend = BigDecimal.ZERO;
        log.info("AI daily spend counters reset");
    }

    /** Snapshot of current daily spend per feature — used by the admin dashboard. */
    public Map<AiFeatureType, BigDecimal> getDailySpendSummary() {
        return Collections.unmodifiableMap(dailySpend);
    }

    /** Current global daily spend — used by the admin dashboard. */
    public BigDecimal getGlobalDailySpend() {
        return globalDailySpend;
    }
}
