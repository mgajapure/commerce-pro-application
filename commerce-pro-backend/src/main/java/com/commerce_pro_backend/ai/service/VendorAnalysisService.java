package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.finance.entity.Vendor;
import com.commerce_pro_backend.finance.repository.VendorRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * VendorAnalysisService — uses AI to evaluate a vendor's overall performance
 * and produce a scorecard with strengths, weaknesses, and a strategic recommendation.
 *
 * <h3>Evaluation dimensions</h3>
 * The model is given financial and relationship data and asked to assess:
 * <ul>
 *   <li><b>Payment health</b>    — outstanding payables vs total paid history.</li>
 *   <li><b>Reliability signals</b> — vendor status (ACTIVE / SUSPENDED), payment terms.</li>
 *   <li><b>Financial exposure</b> — total payable amount and payment terms days.</li>
 *   <li><b>Diversification risk</b> — if payable is a large fraction of the category spend,
 *       this vendor is a single point of failure.</li>
 * </ul>
 *
 * <h3>Example model response → {@link VendorAnalysisResult}</h3>
 * <pre>
 * {
 *   "performanceScore":  72,
 *   "riskLevel":         "MEDIUM",
 *   "strengths":         ["long_payment_history", "large_volume_partner"],
 *   "weaknesses":        ["high_outstanding_payable", "extended_payment_terms"],
 *   "recommendation":    "RENEGOTIATE_TERMS",
 *   "reasoning":         "The vendor has a strong 3-year payment history but the
 *                         current outstanding balance of $42,000 on 60-day terms
 *                         creates cash-flow risk. Renegotiating to net-30 would
 *                         reduce exposure without threatening the relationship."
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VendorAnalysisService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final VendorRepository    vendorRepo;

    private static final AiFeatureType FEATURE = AiFeatureType.VENDOR_ANALYSIS;

    private static final String SYSTEM_PROMPT = """
            You are a strategic procurement analyst.
            Evaluate the vendor's performance and financial relationship based on the data provided.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "performanceScore": <integer 0-100, where 0=poor, 100=excellent>,
              "riskLevel":        <"LOW" | "MEDIUM" | "HIGH" | "CRITICAL">,
              "strengths":        [<snake_case strength identifiers, e.g. "on_time_delivery">],
              "weaknesses":       [<snake_case weakness identifiers, e.g. "high_outstanding_payable">],
              "recommendation":   <"PREFERRED_VENDOR" | "MAINTAIN" | "RENEGOTIATE_TERMS" |
                                    "INCREASE_ORDERS" | "REDUCE_DEPENDENCY" | "SUSPEND">,
              "reasoning":        <plain-English evaluation, 2-4 sentences>
            }
            """;

    /**
     * Analyse a vendor and produce a performance scorecard.
     *
     * @param vendorId  UUID of the {@link Vendor}
     * @return populated {@link VendorAnalysisResult}
     *
     * <p>Example:
     * <pre>
     *   VendorAnalysisResult r = vendorService.analyse("vendor-uuid");
     *   // r.performanceScore() → 72
     *   // r.riskLevel()        → "MEDIUM"
     *   // r.recommendation()   → "RENEGOTIATE_TERMS"
     * </pre>
     */
    @Transactional
    public VendorAnalysisResult analyse(String vendorId) {
        log.info("Vendor analysis requested for vendor: {}", vendorId);

        Vendor vendor = vendorRepo.findById(vendorId)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + vendorId));

        // payableRatio = outstanding / (paid + outstanding) — high ratio = risk
        // Example: totalPayable=$42,000, totalPaid=$158,000 → ratio = 0.21 (21%)
        double payableRatio = computePayableRatio(vendor);

        String userMessage = """
                {
                  "vendorId":            "%s",
                  "vendorName":          "%s",
                  "status":              "%s",
                  "category":            "%s",
                  "country":             "%s",
                  "paymentTerms":        "%s",
                  "paymentTermsDays":     %d,
                  "preferredCurrency":   "%s",
                  "totalPayable":         %s,
                  "totalPaid":            %s,
                  "payableRatioPct":      %.1f
                }
                """.formatted(
                vendor.getId(),
                nvl(vendor.getName()),
                nvl(vendor.getStatus()),
                nvl(vendor.getCategory()),
                nvl(vendor.getCountry()),
                nvl(vendor.getPaymentTerms()),
                nvlInt(vendor.getPaymentTermsDays()),
                nvl(vendor.getPreferredCurrency()),
                nvlDec(vendor.getTotalPayable()),
                nvlDec(vendor.getTotalPaid()),
                payableRatio * 100);

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        VendorAnalysisResult result = PromptJsonExtractor.parse(
                rawResponse, VendorAnalysisResult.class, FEATURE.name());

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(vendorId)
                .entityType("VENDOR")
                .score(result.performanceScore())
                .riskLevel(result.riskLevel())
                .recommendation(result.recommendation())
                .signals(result.weaknesses() != null ? String.join(",", result.weaknesses()) : "")
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());

        return result;
    }

    /**
     * Compute the ratio of outstanding payables to total vendor spend.
     *
     * <p>Example: totalPayable=$42k, totalPaid=$158k
     * → totalSpend=$200k → ratio = 42/200 = 0.21 (21% outstanding)
     * A ratio above 30% is flagged as a risk signal.
     */
    private double computePayableRatio(Vendor vendor) {
        var payable = vendor.getTotalPayable();
        var paid    = vendor.getTotalPaid();
        if (payable == null || paid == null) return 0.0;
        var total = payable.add(paid);
        if (total.compareTo(java.math.BigDecimal.ZERO) == 0) return 0.0;
        return payable.divide(total, 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private String nvl(Object o)                     { return o != null ? o.toString() : ""; }
    private int    nvlInt(Integer i)                  { return i != null ? i : 0; }
    private String nvlDec(java.math.BigDecimal d)     { return d != null ? d.toPlainString() : "0.00"; }

    /** VendorAnalysisResult — the model's vendor performance scorecard. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VendorAnalysisResult(
        /** 0 = poor vendor, 100 = excellent. Maps to AiInsight.score. */
        int performanceScore,
        /** LOW | MEDIUM | HIGH | CRITICAL */
        String riskLevel,
        /** Vendor strengths, e.g. ["long_payment_history", "large_volume_partner"]. */
        List<String> strengths,
        /** Vendor weaknesses, e.g. ["high_outstanding_payable", "extended_payment_terms"]. */
        List<String> weaknesses,
        /** PREFERRED_VENDOR | MAINTAIN | RENEGOTIATE_TERMS | INCREASE_ORDERS | REDUCE_DEPENDENCY | SUSPEND */
        String recommendation,
        /** Plain-English evaluation for the procurement team. */
        String reasoning
    ) {}
}
