package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.repository.CustomerRepository;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ChurnPredictionService — uses AI to predict whether a customer is at risk of churning.
 *
 * <h3>Definition of "churn" in this context</h3>
 * A customer is considered churned when they stop making purchases after a period of
 * regular buying behaviour.  The model is given behavioural signals (recency, frequency,
 * monetary value, return rate) and asked to produce a churn score and retention strategy.
 *
 * <h3>Processing pipeline</h3>
 * <ol>
 *   <li>Load the {@link Customer} record with all aggregated stats (totalOrders, lifetimeSpend, etc.)
 *   <li>Load the customer's last 12 orders to derive recency and frequency signals.
 *   <li>Compute derived metrics (days since last order, AOV trend, return rate).
 *   <li>Build a structured JSON prompt and call the AI model.
 *   <li>Parse the {@link ChurnResult} and persist an {@link AiInsight}.
 * </ol>
 *
 * <h3>Example prompt payload</h3>
 * <pre>
 * {
 *   "customerId":       "cust_xyz",
 *   "tier":             "SILVER",
 *   "totalOrders":      8,
 *   "lifetimeSpend":    1240.00,
 *   "averageOrderValue": 155.00,
 *   "daysSinceLastOrder": 75,
 *   "returnRate":       0.25,
 *   "marketingOptIn":   true,
 *   "loyaltyPoints":    320,
 *   "recentOrderGaps":  [14, 21, 30, 60, 75],
 *   "analysisDate":     "2025-01-15"
 * }
 * </pre>
 *
 * <h3>Example model response</h3>
 * <pre>
 * {
 *   "churnScore":      72,
 *   "riskLevel":       "HIGH",
 *   "recommendation":  "DISCOUNT",
 *   "signals":         ["no_order_75_days", "increasing_order_gap", "high_return_rate"],
 *   "reasoning":       "Customer has not ordered in 75 days despite averaging orders
 *                       every 21 days historically. Return rate (25%) is above the
 *                       10% healthy threshold. A targeted discount code is recommended."
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChurnPredictionService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final CustomerRepository  customerRepo;
    private final OrderRepository     orderRepo;

    /** Feature constant — controls model, budget, and rate limits for churn prediction. */
    private static final AiFeatureType FEATURE = AiFeatureType.CHURN_PREDICTION;

    /**
     * System prompt for churn prediction.
     *
     * <p>Key design choices:
     * - Explicitly names the RFM (Recency, Frequency, Monetary) framework so the
     *   model applies a well-known analytical lens.
     * - Recommendation values are constrained to an enumerated set matching
     *   AiInsight.recommendation so no parsing normalisation is needed.
     * - churnScore 0–100 maps directly to AiInsight.score.
     */
    private static final String SYSTEM_PROMPT = """
            You are a customer retention analyst specialising in e-commerce churn prediction.
            Use the RFM framework (Recency, Frequency, Monetary value) to evaluate the
            customer data provided.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "churnScore":     <integer 0-100, where 0=highly loyal, 100=already churned>,
              "riskLevel":      <"LOW" | "MEDIUM" | "HIGH" | "CRITICAL">,
              "recommendation": <"NONE" | "LOYALTY_REWARD" | "DISCOUNT" | "WIN_BACK_EMAIL" | "PERSONAL_OUTREACH">,
              "signals":        [<short snake_case signals, e.g. "no_order_60_days">],
              "reasoning":      <plain-English explanation, 2-4 sentences>
            }

            Recommendation guidelines:
              0–29  = LOW      → NONE or LOYALTY_REWARD
              30–59 = MEDIUM   → DISCOUNT or WIN_BACK_EMAIL
              60–84 = HIGH     → WIN_BACK_EMAIL or PERSONAL_OUTREACH
              85–100 = CRITICAL → PERSONAL_OUTREACH (customer likely already gone)
            """;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Predict churn risk for a specific customer.
     *
     * @param customerId  UUID of the {@link Customer} to evaluate
     * @return populated {@link ChurnResult}
     * @throws EntityNotFoundException  if the customer does not exist
     *
     * <p>Example:
     * <pre>
     *   ChurnResult result = churnService.predict("cust-uuid");
     *   // result.churnScore()     → 72
     *   // result.riskLevel()      → "HIGH"
     *   // result.recommendation() → "DISCOUNT"
     * </pre>
     */
    @Transactional
    public ChurnResult predict(String customerId) {
        log.info("Starting churn prediction for customer: {}", customerId);

        // 1. Load the customer — throws EntityNotFoundException if not found
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        // 2. Load the last 12 orders to compute recency, frequency, and order gaps.
        //    Limiting to 12 keeps the prompt size small while still showing meaningful trends.
        List<Order> recentOrders = orderRepo.findByCustomerId(customerId)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(12)
                .collect(Collectors.toList());

        // 3. Derive computed metrics from the raw order list
        long daysSinceLastOrder = computeDaysSinceLastOrder(customer, recentOrders);
        String orderGaps        = computeOrderGaps(recentOrders);
        double returnRate       = computeReturnRate(customer);

        // 4. Build the structured prompt payload
        String userMessage = buildChurnPayload(customer, recentOrders, daysSinceLastOrder,
                orderGaps, returnRate);

        // 5. Call the orchestrator (enforces rate-limit + budget checks)
        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);

        // 6. Parse the JSON response into a typed record
        ChurnResult result = PromptJsonExtractor.parse(rawResponse, ChurnResult.class, FEATURE.name());

        // 7. Persist the insight for the audit trail and dashboard
        persistInsight(customer, result);

        log.info("Churn prediction complete for {}: score={}, risk={}, recommendation={}",
                customerId, result.churnScore(), result.riskLevel(), result.recommendation());

        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build the JSON payload that will be sent to the model as the user message.
     *
     * <p>Includes:
     * - Static customer profile fields (tier, order stats, loyalty points).
     * - Derived metrics (days since last order, return rate).
     * - Order gap sequence so the model can spot a widening gap trend.
     */
    private String buildChurnPayload(Customer customer, List<Order> recentOrders,
                                     long daysSinceLastOrder, String orderGaps, double returnRate) {
        return """
                {
                  "customerId":          "%s",
                  "tier":                "%s",
                  "status":              "%s",
                  "totalOrders":          %d,
                  "lifetimeSpend":        %s,
                  "averageOrderValue":    %s,
                  "daysSinceLastOrder":   %d,
                  "returnRate":           %.2f,
                  "marketingOptIn":       %b,
                  "loyaltyPoints":        %d,
                  "recentOrderCount":     %d,
                  "recentOrderGapsDays":  %s,
                  "isBlacklisted":        %b,
                  "isFraudFlagged":       %b,
                  "analysisDate":        "%s"
                }
                """.formatted(
                customer.getId(),
                nvl(customer.getTier()),
                nvl(customer.getStatus()),
                nvlInt(customer.getTotalOrders()),
                nvlDec(customer.getLifetimeSpend()),
                nvlDec(customer.getAverageOrderValue()),
                daysSinceLastOrder,
                returnRate,
                Boolean.TRUE.equals(customer.getMarketingOptIn()),
                nvlInt(customer.getLoyaltyPoints()),
                recentOrders.size(),
                orderGaps,
                Boolean.TRUE.equals(customer.getIsBlacklisted()),
                Boolean.TRUE.equals(customer.getIsFraudFlagged()),
                LocalDateTime.now().toLocalDate()
        );
    }

    /**
     * Calculate the number of days since the customer's most recent order.
     *
     * <p>Falls back to the customer entity's {@code lastOrderAt} field, then to
     * the most recent order in the provided list.
     *
     * <p>Example: lastOrderAt = 2025-01-01, today = 2025-03-15 → returns 73 days.
     */
    private long computeDaysSinceLastOrder(Customer customer, List<Order> recentOrders) {
        LocalDateTime lastOrderDate = customer.getLastOrderAt();
        if (lastOrderDate == null && !recentOrders.isEmpty()) {
            lastOrderDate = recentOrders.get(0).getCreatedAt();
        }
        if (lastOrderDate == null) return 999; // no orders ever → treat as maximum recency gap
        return ChronoUnit.DAYS.between(lastOrderDate, LocalDateTime.now());
    }

    /**
     * Compute the list of gaps (in days) between consecutive recent orders.
     *
     * <p>A growing gap sequence signals decreasing engagement.
     *
     * <p>Example: orders on Jan 1, Jan 22, Feb 15 → gaps are [21, 24].
     * If the gaps start small and grow large, churn risk is higher.
     */
    private String computeOrderGaps(List<Order> orders) {
        if (orders.size() < 2) return "[]";
        var sb = new StringBuilder("[");
        for (int i = 0; i < orders.size() - 1; i++) {
            long gap = ChronoUnit.DAYS.between(
                    orders.get(i + 1).getCreatedAt(),
                    orders.get(i).getCreatedAt());
            sb.append(gap);
            if (i < orders.size() - 2) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    /**
     * Compute the customer's return rate as a decimal (0.0 to 1.0).
     *
     * <p>Example: 3 returns out of 12 orders → returnRate = 0.25 (25%).
     * A rate above ~0.20 is a churn signal.
     */
    private double computeReturnRate(Customer customer) {
        int orders  = nvlInt(customer.getTotalOrders());
        int returns = nvlInt(customer.getTotalReturns());
        if (orders == 0) return 0.0;
        return BigDecimal.valueOf(returns)
                .divide(BigDecimal.valueOf(orders), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Save an {@link AiInsight} for this churn prediction.
     * The insight can be queried later to show a customer's risk history over time.
     */
    private void persistInsight(Customer customer, ChurnResult result) {
        String signalsCsv = result.signals() != null ? String.join(",", result.signals()) : "";

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(customer.getId())
                .entityType("CUSTOMER")
                .score(result.churnScore())
                .riskLevel(result.riskLevel())
                .recommendation(result.recommendation())
                .signals(signalsCsv)
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());
    }

    // ── Null-safe helpers ─────────────────────────────────────────────────────

    private String nvl(Object o)            { return o != null ? o.toString() : "UNKNOWN"; }
    private int    nvlInt(Integer i)         { return i != null ? i : 0; }
    private String nvlDec(BigDecimal d)      { return d != null ? d.toPlainString() : "0.00"; }

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * ChurnResult — immutable record holding the model's churn assessment.
     *
     * <p>Example JSON → record mapping:
     * <pre>
     * JSON:   {"churnScore":68,"riskLevel":"HIGH","recommendation":"WIN_BACK_EMAIL",
     *          "signals":["no_order_75_days"],"reasoning":"Customer inactive..."}
     * Record: ChurnResult(churnScore=68, riskLevel="HIGH",
     *                     recommendation="WIN_BACK_EMAIL", ...)
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChurnResult(
        /** 0 = highly loyal, 100 = already churned. Maps to AiInsight.score. */
        int churnScore,

        /** LOW | MEDIUM | HIGH | CRITICAL */
        String riskLevel,

        /** NONE | LOYALTY_REWARD | DISCOUNT | WIN_BACK_EMAIL | PERSONAL_OUTREACH */
        String recommendation,

        /** Signal list, e.g. ["no_order_75_days", "increasing_order_gap"]. */
        List<String> signals,

        /** Plain-English explanation for the marketing team. */
        String reasoning
    ) {}
}
