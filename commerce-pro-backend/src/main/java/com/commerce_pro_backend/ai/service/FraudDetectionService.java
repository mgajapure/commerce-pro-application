package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.exception.AiResponseParseException;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.payment.entity.PaymentTransaction;
import com.commerce_pro_backend.payment.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FraudDetectionService — uses AI to score a payment transaction for fraud risk.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Load the target {@link PaymentTransaction} by ID.
 *   <li>Load the customer's 10 most recent transactions for velocity context.
 *   <li>Build a structured JSON prompt containing all fraud-relevant fields.
 *   <li>Ask the AI model to return a risk score (0–100), level, recommendation,
 *       signals, and plain-English reasoning — all as a JSON object.
 *   <li>Parse the response and persist an {@link AiInsight} so future calls and
 *       the audit trail can reference the decision.
 *   <li>Copy the risk score back onto the {@link PaymentTransaction} record.
 * </ol>
 *
 * <h3>Example prompt payload sent to the model</h3>
 * <pre>
 * {
 *   "transactionId": "txn_abc123",
 *   "amount": 999.99,
 *   "currency": "USD",
 *   "paymentMethod": "CARD",
 *   "cardLast4": "4242",
 *   "cardBrand": "VISA",
 *   "ipAddress": "1.2.3.4",
 *   "customerId": "cust_xyz",
 *   "customerEmail": "john@example.com",
 *   "customerOrderCount": 1,
 *   "customerLifetimeSpend": 0.00,
 *   "recentTransactionAmounts": [],
 *   "analysisTime": "2025-01-15T14:30:00"
 * }
 * </pre>
 *
 * <h3>Example model response (parsed into {@link FraudResult})</h3>
 * <pre>
 * {
 *   "riskScore": 85,
 *   "riskLevel": "HIGH",
 *   "recommendation": "REVIEW",
 *   "signals": ["large_amount_first_order", "suspicious_ip_geo"],
 *   "reasoning": "First-ever transaction for this customer totals $999.99 — well above
 *                 typical first-order averages. The IP geolocation does not match the
 *                 billing country. Manual review recommended before capture."
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final AiOrchestrator          orchestrator;
    private final AiInsightRepository     insightRepo;
    private final PaymentTransactionRepository txnRepo;

    // ── Feature constant ──────────────────────────────────────────────────────

    /** The AiFeatureType that controls enable/disable, model, budget, and rate limits for this service. */
    private static final AiFeatureType FEATURE = AiFeatureType.FRAUD_DETECTION;

    // ── System prompt ─────────────────────────────────────────────────────────

    /**
     * System prompt sent before every fraud analysis request.
     *
     * <p>Key instructions:
     * - Role definition keeps the model focused on fraud patterns only.
     * - "ONLY a JSON object" eliminates prose wrapping and markdown fences.
     * - Explicit schema definition prevents hallucinated field names.
     * - riskScore 0–100 maps directly to AiInsight.score.
     * - recommendation values (APPROVE / REVIEW / BLOCK) map to AiInsight.recommendation.
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert payment fraud detection analyst.
            Analyse the transaction data provided and assess its fraud risk.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "riskScore":      <integer 0-100, where 0=no risk, 100=certain fraud>,
              "riskLevel":      <"LOW" | "MEDIUM" | "HIGH" | "CRITICAL">,
              "recommendation": <"APPROVE" | "REVIEW" | "BLOCK">,
              "signals":        [<list of short snake_case signal strings, e.g. "large_first_order">],
              "reasoning":      <plain-English explanation, 2-4 sentences>
            }

            Risk level guidelines:
              0–24  = LOW      → APPROVE
              25–59 = MEDIUM   → REVIEW
              60–84 = HIGH     → REVIEW or BLOCK
              85–100 = CRITICAL → BLOCK
            """;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Analyse a payment transaction for fraud and return the AI's assessment.
     *
     * <p>This method is transactional so that the AiInsight insert and the
     * riskScore update on PaymentTransaction happen atomically.
     *
     * @param transactionId  UUID of the {@link PaymentTransaction} to evaluate
     * @return populated {@link FraudResult} containing score, level, recommendation and reasoning
     * @throws jakarta.persistence.EntityNotFoundException if the transaction does not exist
     * @throws AiResponseParseException if the model returns an unparseable response
     *
     * <p>Example:
     * <pre>
     *   FraudResult result = fraudService.analyse("txn-uuid-here");
     *   // result.riskScore()      → 85
     *   // result.riskLevel()      → "HIGH"
     *   // result.recommendation() → "REVIEW"
     *   // result.signals()        → ["large_amount_first_order"]
     * </pre>
     */
    @Transactional
    public FraudResult analyse(String transactionId) {
        log.info("Starting fraud analysis for transaction: {}", transactionId);

        // 1. Load the transaction being evaluated
        PaymentTransaction txn = txnRepo.findById(transactionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Transaction not found: " + transactionId));

        // 2. Load recent transactions for this customer to provide velocity context.
        //    Velocity = how many transactions has this customer made recently?
        //    A sudden spike in transaction frequency is a key fraud signal.
        List<PaymentTransaction> recentTxns = txnRepo
                .findByCustomerId(txn.getCustomerId(), PageRequest.of(0, 10))
                .getContent();

        // 3. Build the user message that the model will analyse.
        //    We send structured JSON so the model can reason about specific fields.
        String userMessage = buildFraudPayload(txn, recentTxns);

        // 4. Call the AI model through the orchestrator.
        //    The orchestrator enforces: enabled check → rate limit → budget → Groq call → usage log.
        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);

        // 5. Parse the JSON object from the model's response.
        //    PromptJsonExtractor handles markdown fences and leading/trailing text.
        FraudResult result = PromptJsonExtractor.parse(rawResponse, FraudResult.class, FEATURE.name());

        // 6. Persist an AiInsight row for the audit trail.
        //    Future calls can load prior insights via AiInsightRepository
        //    to show risk history for a transaction or customer.
        persistInsight(txn, result);

        // 7. Write the risk score back to the transaction record so it is
        //    immediately visible on the payment dashboard without a separate lookup.
        txn.setRiskScore(result.riskScore());
        if (result.riskScore() >= 60) {
            // Flag the transaction for human review when risk is HIGH or CRITICAL
            txn.setIsFlagged(true);
            txn.setFlagReason("AI fraud score: " + result.riskScore() + " — " + result.riskLevel());
        }
        txnRepo.save(txn);

        log.info("Fraud analysis complete for {}: score={}, level={}, recommendation={}",
                transactionId, result.riskScore(), result.riskLevel(), result.recommendation());

        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build the JSON payload sent to the model as the user message.
     *
     * <p>We include:
     * - Transaction-level fields: amount, method, IP, card details.
     * - Customer-level aggregates: lifetime spend, order count.
     * - Velocity signal: list of recent transaction amounts for the same customer.
     *
     * <p>Example output:
     * <pre>
     * {
     *   "transactionId": "txn_abc",
     *   "amount": 450.00,
     *   "currency": "USD",
     *   "paymentMethod": "CARD",
     *   "cardLast4": "9999",
     *   "cardBrand": "MASTERCARD",
     *   "ipAddress": "203.0.113.42",
     *   "customerId": "cust_123",
     *   "customerEmail": "user@example.com",
     *   "customerOrderCount": 3,
     *   "customerLifetimeSpend": 300.00,
     *   "recentTransactionAmounts": [50.00, 75.00, 80.00],
     *   "analysisTime": "2025-01-15T14:30:00"
     * }
     * </pre>
     */
    private String buildFraudPayload(PaymentTransaction txn, List<PaymentTransaction> recentTxns) {
        // Extract recent amounts for velocity analysis (exclude the current transaction)
        String recentAmounts = recentTxns.stream()
                .filter(t -> !t.getId().equals(txn.getId()))
                .map(t -> t.getAmount() != null ? t.getAmount().toPlainString() : "0")
                .collect(Collectors.joining(", ", "[", "]"));

        return """
                {
                  "transactionId":             "%s",
                  "amount":                     %s,
                  "currency":                  "%s",
                  "paymentMethod":             "%s",
                  "cardLast4":                 "%s",
                  "cardBrand":                 "%s",
                  "ipAddress":                 "%s",
                  "customerId":                "%s",
                  "customerEmail":             "%s",
                  "customerOrderCount":         %d,
                  "customerLifetimeSpend":      %s,
                  "recentTransactionAmounts":   %s,
                  "analysisTime":              "%s"
                }
                """.formatted(
                nvl(txn.getId()),
                nvl(txn.getAmount(), BigDecimal.ZERO),
                nvl(txn.getCurrency()),
                nvl(txn.getPaymentMethodType()),
                nvl(txn.getCardLast4()),
                nvl(txn.getCardBrand()),
                nvl(txn.getIpAddress()),
                nvl(txn.getCustomerId()),
                nvl(txn.getCustomerEmail()),
                recentTxns.size(),
                nvl(txn.getAmount(), BigDecimal.ZERO), // proxy for lifetime spend when unavailable
                recentAmounts,
                LocalDateTime.now()
        );
    }

    /**
     * Save a fully-populated {@link AiInsight} row for this fraud decision.
     *
     * <p>Stored fields of note:
     * - {@code score}          → riskScore (0–100)
     * - {@code riskLevel}      → LOW / MEDIUM / HIGH / CRITICAL
     * - {@code recommendation} → APPROVE / REVIEW / BLOCK
     * - {@code signals}        → comma-joined signal strings for quick filtering
     * - {@code reasoning}      → full model explanation for the audit trail
     */
    private void persistInsight(PaymentTransaction txn, FraudResult result) {
        // Join the signal list into a CSV string (stored in AiInsight.signals column)
        // Example: "large_first_order,suspicious_ip_geo"
        String signalsCsv = result.signals() != null
                ? String.join(",", result.signals())
                : "";

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(txn.getId())
                .entityType("TRANSACTION")
                .score(result.riskScore())
                .riskLevel(result.riskLevel())
                .recommendation(result.recommendation())
                .signals(signalsCsv)
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .modelUsed("groq")           // updated by orchestrator post-persist if needed
                .status("PROCESSED")
                .build());
    }

    /** Null-safe string coercion — returns "(unknown)" for null objects to avoid JSON gaps. */
    private String nvl(Object o) {
        return o != null ? o.toString() : "(unknown)";
    }

    /** Null-safe BigDecimal coercion — returns fallback when value is null. */
    private BigDecimal nvl(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * FraudResult — immutable record that holds the model's fraud assessment.
     *
     * <p>Jackson maps the AI's JSON response directly onto this record.
     * Fields that the model might omit (e.g. {@code signals} on a low-risk transaction)
     * default to null; callers should null-check before iterating.
     *
     * <p>Example JSON → record mapping:
     * <pre>
     * JSON:   {"riskScore":72,"riskLevel":"HIGH","recommendation":"REVIEW",
     *          "signals":["no_prior_orders"],"reasoning":"First-time buyer..."}
     * Record: FraudResult(riskScore=72, riskLevel="HIGH", recommendation="REVIEW",
     *                     signals=["no_prior_orders"], reasoning="First-time buyer...")
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FraudResult(
        /** 0 = definitely clean, 100 = certain fraud. Maps to AiInsight.score. */
        int riskScore,

        /** LOW | MEDIUM | HIGH | CRITICAL */
        String riskLevel,

        /** APPROVE | REVIEW | BLOCK */
        String recommendation,

        /** Key signals that drove the score, e.g. ["large_first_order", "ip_mismatch"]. */
        List<String> signals,

        /** Plain-English explanation for human reviewers and audit logs. */
        String reasoning
    ) {}
}
