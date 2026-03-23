# AI Infrastructure

Core services, configuration, cost controls, and rate limiting for the AI module.

---

## 1. Spring Configuration

### application.properties
```properties
# ============================================================
# Anthropic AI Module
# ============================================================

# API key — set via environment variable, never hardcode
anthropic.api-key=${ANTHROPIC_API_KEY}

# Default models (overridden per-feature in AiConfig entity)
anthropic.models.haiku=claude-haiku-4-5-20251001
anthropic.models.sonnet=claude-sonnet-4-6
anthropic.models.opus=claude-opus-4-6

# Global max tokens safety cap (prevents runaway costs)
anthropic.max-tokens-cap=4096

# Prompt caching
anthropic.prompt-cache.enabled=true

# Daily total budget across all features (USD)
anthropic.budget.daily-total-usd=50.00

# HTTP client timeouts
anthropic.timeout.connect-ms=5000
anthropic.timeout.read-ms=30000

# Rate limiting (global fallback — AiConfig per-feature values take precedence)
anthropic.rate-limit.calls-per-minute=200
anthropic.rate-limit.calls-per-hour=5000

# Retry on transient failures
anthropic.retry.max-attempts=3
anthropic.retry.backoff-ms=1000

# Conversation session TTLs
anthropic.session.chatbot-ttl-hours=24
anthropic.session.report-ttl-hours=2

# Scheduled jobs enable/disable (useful for dev environments)
anthropic.jobs.nightly-forecast.enabled=true
anthropic.jobs.weekly-churn.enabled=true
anthropic.jobs.weekly-inventory.enabled=true
anthropic.jobs.monthly-budget.enabled=true
anthropic.jobs.monthly-vendor.enabled=true
```

### AiModuleConfig.java
```java
@Configuration
@ConfigurationProperties(prefix = "anthropic")
@Data
public class AiModuleConfig {

    private String apiKey;
    private Map<String, String> models;
    private int maxTokensCap = 4096;
    private boolean promptCacheEnabled = true;
    private BudgetConfig budget;
    private TimeoutConfig timeout;
    private RetryConfig retry;
    private SessionConfig session;

    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .connectTimeout(Duration.ofMillis(timeout.getConnectMs()))
            .readTimeout(Duration.ofMillis(timeout.getReadMs()))
            .build();
    }

    @Data
    public static class BudgetConfig {
        private BigDecimal dailyTotalUsd = new BigDecimal("50.00");
    }

    @Data
    public static class TimeoutConfig {
        private long connectMs = 5000;
        private long readMs = 30000;
    }

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long backoffMs = 1000;
    }

    @Data
    public static class SessionConfig {
        private int chatbotTtlHours = 24;
        private int reportTtlHours = 2;
    }
}
```

---

## 2. AiOrchestrator

The single point of entry for all Anthropic API calls. Handles: config loading, feature toggle check, rate limit check, prompt cache flag, API call with retry, usage logging, and cost calculation.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrator {

    private final AnthropicClient client;
    private final AiConfigRepository configRepo;
    private final AiUsageLogRepository usageLogRepo;
    private final AiCostGuard costGuard;
    private final AiRateLimiter rateLimiter;

    // In-memory config cache (refreshed every 5 minutes)
    private final Map<AiFeatureType, AiConfig> configCache = new ConcurrentHashMap<>();

    /**
     * Execute an AI request for a given feature.
     * Handles: feature toggle, rate limiting, budget check, API call, retry, logging.
     */
    public Message execute(AiFeatureType feature, MessageCreateParams params) {
        AiConfig config = getConfig(feature);

        // 1. Feature toggle check
        if (!config.getEnabled()) {
            throw new AiFeatureDisabledException(feature + " is currently disabled");
        }

        // 2. Rate limit check
        rateLimiter.checkAndIncrement(feature);

        // 3. Budget check
        costGuard.checkDailyBudget(feature);

        // 4. Apply config overrides (max tokens cap)
        int safeMaxTokens = Math.min(params.maxTokens(), config.getMaxTokens());
        params = params.toBuilder().maxTokens(safeMaxTokens).build();

        // 5. Execute with retry
        long startMs = System.currentTimeMillis();
        Message response = executeWithRetry(params, config);
        long latencyMs = System.currentTimeMillis() - startMs;

        // 6. Log usage and cost
        logUsage(feature, config.getModel(), response, latencyMs, null);

        return response;
    }

    private Message executeWithRetry(MessageCreateParams params, AiConfig config) {
        int maxAttempts = 3;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return client.messages().create(params);
            } catch (AnthropicException e) {
                if (e.statusCode() == 429) {           // rate limited by Anthropic
                    log.warn("Anthropic rate limit hit, attempt {}/{}", attempt, maxAttempts);
                    sleepQuietly(backoffMs * attempt);
                } else if (e.statusCode() >= 500) {    // server error
                    log.warn("Anthropic server error {}, attempt {}/{}", e.statusCode(), attempt, maxAttempts);
                    sleepQuietly(backoffMs * attempt);
                } else {
                    throw e;                           // client error — don't retry
                }
                if (attempt == maxAttempts) throw e;
            }
        }
        throw new IllegalStateException("Should not reach here");
    }

    private void logUsage(AiFeatureType feature, String model, Message response,
                          long latencyMs, String conversationId) {
        Usage usage = response.usage();
        BigDecimal cost = CostCalculator.calculate(model, usage);

        usageLogRepo.save(AiUsageLog.builder()
            .featureType(feature)
            .model(model)
            .conversationId(conversationId)
            .tokensInput(usage.inputTokens())
            .tokensOutput(usage.outputTokens())
            .tokensCacheRead(usage.cacheReadInputTokens().orElse(0))
            .tokensCacheWrite(usage.cacheCreationInputTokens().orElse(0))
            .totalCostUsd(cost)
            .latencyMs(latencyMs)
            .calledAt(LocalDateTime.now())
            .success(true)
            .build());

        // Update running daily cost total for budget enforcement
        costGuard.recordSpend(feature, cost);
    }

    private AiConfig getConfig(AiFeatureType feature) {
        return configCache.computeIfAbsent(feature,
            f -> configRepo.findByFeatureType(f).orElseThrow());
    }

    @Scheduled(fixedDelay = 300_000)   // refresh config cache every 5 min
    public void refreshConfigCache() {
        configRepo.findAll().forEach(c -> configCache.put(c.getFeatureType(), c));
    }

    private void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
```

---

## 3. AiCostGuard

Tracks daily and monthly spend per feature. Auto-disables features when budget is exceeded.

```java
@Service
@Slf4j
public class AiCostGuard {

    private final AiUsageLogRepository usageLogRepo;
    private final AiConfigRepository configRepo;
    private final AiModuleConfig moduleConfig;

    // In-memory running totals reset at midnight
    private final Map<AiFeatureType, BigDecimal> dailySpend = new ConcurrentHashMap<>();
    private volatile BigDecimal globalDailySpend = BigDecimal.ZERO;

    /**
     * Check if this feature is within its daily budget.
     * Throws AiBudgetExceededException if over budget.
     */
    public void checkDailyBudget(AiFeatureType feature) {
        // Check global daily cap first
        if (globalDailySpend.compareTo(moduleConfig.getBudget().getDailyTotalUsd()) >= 0) {
            log.error("Global daily AI budget exceeded: ${}", globalDailySpend);
            throw new AiBudgetExceededException("Global daily AI budget of $"
                + moduleConfig.getBudget().getDailyTotalUsd() + " exceeded");
        }

        // Check per-feature cap
        AiConfig config = configRepo.findByFeatureType(feature).orElseThrow();
        if (config.getDailyBudgetUsd() != null) {
            BigDecimal featureSpend = dailySpend.getOrDefault(feature, BigDecimal.ZERO);
            if (featureSpend.compareTo(config.getDailyBudgetUsd()) >= 0) {
                log.warn("Feature {} daily budget of ${} exceeded", feature, config.getDailyBudgetUsd());
                throw new AiBudgetExceededException(feature + " daily budget exceeded");
            }
        }
    }

    public synchronized void recordSpend(AiFeatureType feature, BigDecimal cost) {
        dailySpend.merge(feature, cost, BigDecimal::add);
        globalDailySpend = globalDailySpend.add(cost);
    }

    // Reset counters at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyCounters() {
        dailySpend.clear();
        globalDailySpend = BigDecimal.ZERO;
        log.info("AI daily spend counters reset");
    }

    // Daily spend report (called by monitoring dashboard)
    public Map<AiFeatureType, BigDecimal> getDailySpendSummary() {
        return Collections.unmodifiableMap(dailySpend);
    }
}
```

---

## 4. AiRateLimiter

Per-feature, per-minute/per-hour rate limiting using sliding window counters.

```java
@Service
public class AiRateLimiter {

    private final AiConfigRepository configRepo;

    // Sliding window: feature -> deque of call timestamps
    private final Map<AiFeatureType, Deque<Long>> minuteWindows = new ConcurrentHashMap<>();
    private final Map<AiFeatureType, Deque<Long>> hourWindows   = new ConcurrentHashMap<>();

    public synchronized void checkAndIncrement(AiFeatureType feature) {
        AiConfig config = configRepo.findByFeatureType(feature).orElseThrow();
        long now = System.currentTimeMillis();

        // Per-minute check
        Deque<Long> minuteWindow = minuteWindows.computeIfAbsent(feature, k -> new ArrayDeque<>());
        evictOlderThan(minuteWindow, now - 60_000);
        if (minuteWindow.size() >= config.getRateLimitPerMinute()) {
            throw new AiRateLimitException(feature + " rate limit exceeded: "
                + config.getRateLimitPerMinute() + " calls/min");
        }

        // Per-hour check
        Deque<Long> hourWindow = hourWindows.computeIfAbsent(feature, k -> new ArrayDeque<>());
        evictOlderThan(hourWindow, now - 3_600_000);
        if (hourWindow.size() >= config.getRateLimitPerHour()) {
            throw new AiRateLimitException(feature + " hourly rate limit exceeded");
        }

        minuteWindow.addLast(now);
        hourWindow.addLast(now);
    }

    private void evictOlderThan(Deque<Long> window, long cutoff) {
        while (!window.isEmpty() && window.peekFirst() < cutoff) {
            window.pollFirst();
        }
    }
}
```

---

## 5. CostCalculator

Computes exact USD cost from token counts and model pricing.

```java
public class CostCalculator {

    // Prices per 1M tokens in USD (update when Anthropic changes pricing)
    private static final Map<String, ModelPricing> PRICING = Map.of(
        "claude-haiku-4-5-20251001",  new ModelPricing(0.80,  4.00,  0.08, 0.40),
        "claude-sonnet-4-6",          new ModelPricing(3.00,  15.00, 0.30, 1.50),
        "claude-opus-4-6",            new ModelPricing(15.00, 75.00, 1.50, 7.50)
    );

    /**
     * Calculate total cost for an API call.
     *
     * @param model   the model ID string
     * @param usage   Anthropic Usage object from the response
     * @return        total cost in USD
     */
    public static BigDecimal calculate(String model, Usage usage) {
        ModelPricing p = PRICING.getOrDefault(model, PRICING.get("claude-sonnet-4-6"));

        BigDecimal inputCost  = costForTokens(usage.inputTokens(), p.inputPricePer1M());
        BigDecimal outputCost = costForTokens(usage.outputTokens(), p.outputPricePer1M());

        // Cache read tokens are billed at cache hit price (10% of input)
        BigDecimal cacheReadCost = costForTokens(
            usage.cacheReadInputTokens().orElse(0), p.cacheHitPricePer1M());

        // Cache write tokens are billed at cache write price (125% of input)
        BigDecimal cacheWriteCost = costForTokens(
            usage.cacheCreationInputTokens().orElse(0), p.cacheWritePricePer1M());

        return inputCost.add(outputCost).add(cacheReadCost).add(cacheWriteCost)
            .setScale(8, RoundingMode.HALF_UP);
    }

    private static BigDecimal costForTokens(int tokens, double pricePer1M) {
        return BigDecimal.valueOf(tokens)
            .multiply(BigDecimal.valueOf(pricePer1M))
            .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
    }

    record ModelPricing(double inputPricePer1M, double outputPricePer1M,
                        double cacheHitPricePer1M, double cacheWritePricePer1M) {}
}
```

---

## 6. Admin REST Controller

```java
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AiAdminController {

    private final AiConfigRepository configRepo;
    private final AiUsageLogRepository usageLogRepo;
    private final AiInsightRepository insightRepo;
    private final AiCostGuard costGuard;
    private final AiOrchestrator orchestrator;

    // ── Config management ───────────────────────────────────────────────────
    @GetMapping("/config")
    public List<AiConfig> getAllConfigs() {
        return configRepo.findAll();
    }

    @PatchMapping("/config/{featureType}/toggle")
    public AiConfig toggleFeature(@PathVariable AiFeatureType featureType,
                                  @RequestParam boolean enabled) {
        AiConfig config = configRepo.findByFeatureType(featureType).orElseThrow();
        config.setEnabled(enabled);
        AiConfig saved = configRepo.save(config);
        orchestrator.refreshConfigCache();    // immediate effect
        return saved;
    }

    @PatchMapping("/config/{featureType}/model")
    public AiConfig changeModel(@PathVariable AiFeatureType featureType,
                                @RequestParam String model) {
        AiConfig config = configRepo.findByFeatureType(featureType).orElseThrow();
        config.setModel(model);
        return configRepo.save(config);
    }

    @PatchMapping("/config/{featureType}/budget")
    public AiConfig updateBudget(@PathVariable AiFeatureType featureType,
                                 @RequestParam BigDecimal dailyBudgetUsd) {
        AiConfig config = configRepo.findByFeatureType(featureType).orElseThrow();
        config.setDailyBudgetUsd(dailyBudgetUsd);
        return configRepo.save(config);
    }

    // ── Cost monitoring ─────────────────────────────────────────────────────
    @GetMapping("/cost/today")
    public Map<AiFeatureType, BigDecimal> getTodaysCost() {
        return costGuard.getDailySpendSummary();
    }

    @GetMapping("/cost/range")
    public List<Object[]> getCostByRange(
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime to) {
        return usageLogRepo.costSummaryByFeature(from, to);
    }

    // ── Insight management ──────────────────────────────────────────────────
    @GetMapping("/insights")
    public Page<AiInsight> getInsights(
            @RequestParam(required = false) AiFeatureType featureType,
            @RequestParam(required = false) String riskLevel,
            Pageable pageable) {
        if (featureType != null && riskLevel != null) {
            return insightRepo.findByFeatureTypeAndRiskLevel(featureType, riskLevel, pageable);
        }
        return insightRepo.findAll(pageable);
    }

    @PatchMapping("/insights/{id}/review")
    public AiInsight reviewInsight(@PathVariable String id,
                                   @RequestParam String reviewStatus,
                                   @RequestParam(required = false) String notes) {
        AiInsight insight = insightRepo.findById(id).orElseThrow();
        insight.setReviewStatus(reviewStatus);
        insight.setReviewNotes(notes);
        insight.setReviewedAt(LocalDateTime.now());
        return insightRepo.save(insight);
    }

    // ── Usage logs ──────────────────────────────────────────────────────────
    @GetMapping("/usage/slow-calls")
    public List<AiUsageLog> getSlowCalls(@RequestParam(defaultValue = "5000") long thresholdMs) {
        return usageLogRepo.findByLatencyMsGreaterThanAndCalledAtAfter(
            thresholdMs, LocalDateTime.now().minusDays(1));
    }
}
```

---

## 7. Exception Hierarchy

```java
// Base
public class AiModuleException extends RuntimeException { ... }

// Feature is toggled off
public class AiFeatureDisabledException extends AiModuleException { ... }

// Daily/monthly budget cap hit
public class AiBudgetExceededException extends AiModuleException { ... }

// Per-feature rate limit hit
public class AiRateLimitException extends AiModuleException { ... }

// Claude returned unparseable JSON
public class AiResponseParseException extends AiModuleException { ... }

// Multi-turn session expired
public class ConversationExpiredException extends AiModuleException { ... }
```

### Global exception handler
```java
@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(AiFeatureDisabledException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleDisabled(AiFeatureDisabledException e) {
        return new ApiError("AI_DISABLED", e.getMessage());
    }

    @ExceptionHandler(AiBudgetExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiError handleBudget(AiBudgetExceededException e) {
        return new ApiError("AI_BUDGET_EXCEEDED", e.getMessage());
    }

    @ExceptionHandler(AiRateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiError handleRateLimit(AiRateLimitException e) {
        return new ApiError("AI_RATE_LIMITED", e.getMessage());
    }

    @ExceptionHandler(ConversationExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiError handleExpired(ConversationExpiredException e) {
        return new ApiError("SESSION_EXPIRED", e.getMessage());
    }
}
```

---

## 8. Scheduled Jobs Summary

| Job | Cron | Feature | What it does |
|-----|------|---------|--------------|
| `NightlyForecastJob` | `0 2 * * *` | Demand Forecast | Runs AI forecast for all active products |
| `WeeklyChurnJob` | `0 3 * * 0` | Churn Prediction | Scores all active customers |
| `WeeklyInventoryJob` | `0 4 * * 1` | Inventory Optimization | Generates reorder recommendations |
| `WeeklyReturnPatternJob` | `0 5 * * 1` | Return Pattern | Flags high-return-rate products |
| `MonthlyBudgetJob` | `0 6 1 * *` | Budget Anomaly | Analyses full period financials |
| `MonthlyVendorJob` | `0 7 1 * *` | Vendor Analysis | Scores all vendors |
| `ConversationCleanupJob` | `0 1 * * *` | All sessions | Deletes expired conversations |
| `InsightCleanupJob` | `0 2 1 * *` | All insights | Deletes expired insights (if expires_at set) |

---

## 9. Observability

### Metrics to expose via Spring Actuator / Micrometer
```java
@Component
public class AiMetrics {

    private final MeterRegistry registry;

    // Counter: total API calls per feature
    public void recordCall(AiFeatureType feature, boolean success) {
        registry.counter("ai.calls",
            "feature", feature.name(),
            "success", String.valueOf(success)
        ).increment();
    }

    // Histogram: latency per feature
    public void recordLatency(AiFeatureType feature, long latencyMs) {
        registry.timer("ai.latency", "feature", feature.name())
            .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    // Gauge: daily spend
    public void registerSpendGauge(AiFeatureType feature, Supplier<Double> spendSupplier) {
        Gauge.builder("ai.spend.daily", spendSupplier, Supplier::get)
            .tag("feature", feature.name())
            .description("Daily USD spend for this AI feature")
            .register(registry);
    }
}
```

### Recommended Alerts
| Alert | Threshold | Action |
|-------|-----------|--------|
| Daily global spend > 80% of budget | $40 of $50 | Slack warning |
| Any call latency > 10s | 10,000 ms | PagerDuty |
| Error rate > 5% in last 10 min | 5% | PagerDuty |
| Feature budget exhausted | 100% | Auto-disable + alert |
| Fraud CRITICAL score detected | score ≥ 86 | Immediate Slack + email |
| Churn HIGH spike | >20 HIGH in 1 hour | Slack notification |
