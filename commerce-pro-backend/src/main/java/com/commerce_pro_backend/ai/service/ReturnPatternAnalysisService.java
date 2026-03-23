package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.catalog.product.entity.Product;
import com.commerce_pro_backend.catalog.product.repository.ProductRepository;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * ReturnPatternAnalysisService — uses AI to detect why a product has an
 * elevated return rate and recommend a corrective action.
 *
 * <h3>Why return analysis matters</h3>
 * A 10%+ return rate on a product erodes margin and often signals one of these root causes:
 * <ul>
 *   <li>Product description mismatch — photos or specs don't match the physical item.</li>
 *   <li>Quality issue — manufacturing defect or supplier problem.</li>
 *   <li>Sizing / fit — especially relevant for apparel and footwear.</li>
 *   <li>Wrong product — fulfilment errors picking the wrong SKU.</li>
 *   <li>Buyer remorse — product is fine but over-marketed.</li>
 * </ul>
 *
 * <h3>Data the model receives</h3>
 * <ul>
 *   <li>Product metadata (name, category, rating).</li>
 *   <li>Order-level return rate computed from {@code returnedQuantity / quantity}.</li>
 *   <li>Recent order summaries showing individual return quantities.</li>
 * </ul>
 *
 * <h3>Example model response → {@link ReturnAnalysisResult}</h3>
 * <pre>
 * {
 *   "returnScore":      72,
 *   "riskLevel":        "HIGH",
 *   "suspectedCauses":  ["description_mismatch", "sizing_issue"],
 *   "recommendation":   "UPDATE_LISTING",
 *   "reasoning":        "The 28% return rate far exceeds the 10% healthy threshold.
 *                        The low rating (2.8★) combined with high return volume
 *                        suggests the product description may not match reality.
 *                        Recommend reviewing product photos and size guide."
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReturnPatternAnalysisService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final ProductRepository   productRepo;
    private final OrderRepository     orderRepo;

    private static final AiFeatureType FEATURE = AiFeatureType.RETURN_PATTERN_ANALYSIS;

    private static final String SYSTEM_PROMPT = """
            You are an e-commerce operations analyst specialising in product return patterns.
            Analyse the return data and identify the most likely root cause.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "returnScore":     <integer 0-100, where 0=excellent, 100=critical return problem>,
              "riskLevel":       <"LOW" | "MEDIUM" | "HIGH" | "CRITICAL">,
              "suspectedCauses": [<snake_case causes: "description_mismatch" | "quality_issue" |
                                   "sizing_issue" | "fulfillment_error" | "buyer_remorse" |
                                   "damaged_in_transit">],
              "recommendation":  <"MONITOR" | "CONTACT_SUPPLIER" | "UPDATE_LISTING" |
                                   "SUSPEND_PRODUCT" | "REVIEW_FULFILLMENT">,
              "reasoning":       <plain-English explanation, 2-4 sentences>
            }
            """;

    /**
     * Analyse the return pattern for a specific product.
     *
     * @param productId   UUID of the product to analyse
     * @param lookbackDays how many days of order history to include (e.g. 90)
     * @return populated {@link ReturnAnalysisResult}
     *
     * <p>Example:
     * <pre>
     *   ReturnAnalysisResult r = returnService.analyse("prod-uuid", 90);
     *   // r.returnScore()     → 72
     *   // r.riskLevel()       → "HIGH"
     *   // r.recommendation()  → "UPDATE_LISTING"
     * </pre>
     */
    @Transactional
    public ReturnAnalysisResult analyse(String productId, int lookbackDays) {
        log.info("Return pattern analysis for product {} over {} days", productId, lookbackDays);

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        LocalDateTime since = LocalDateTime.now().minusDays(lookbackDays);

        // Pull orders that contain this product and have at least one returned item
        // We compute totalSold and totalReturned from the OrderItem list
        List<Order> recentOrders = orderRepo.findAllByCreatedAtBetween(since, LocalDateTime.now())
                .stream()
                .filter(o -> o.getItems() != null && o.getItems().stream()
                        .anyMatch(i -> productId.equals(i.getProductId())))
                .limit(50)
                .collect(Collectors.toList());

        int totalSold = 0, totalReturned = 0;
        for (Order order : recentOrders) {
            if (order.getItems() == null) continue;
            for (var item : order.getItems()) {
                if (!productId.equals(item.getProductId())) continue;
                totalSold     += nvlInt(item.getQuantity());
                totalReturned += nvlInt(item.getReturnedQuantity());
            }
        }

        // returnRatePct = (returned / sold) × 100; e.g. 7 returns / 25 sold = 28%
        double returnRatePct = totalSold > 0
                ? BigDecimal.valueOf(totalReturned)
                        .divide(BigDecimal.valueOf(totalSold), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        // Build order-level summary showing return quantity per order
        String orderSummary = recentOrders.stream()
                .map(o -> {
                    int qty = 0, ret = 0;
                    if (o.getItems() != null) {
                        for (var i : o.getItems()) {
                            if (productId.equals(i.getProductId())) {
                                qty += nvlInt(i.getQuantity());
                                ret += nvlInt(i.getReturnedQuantity());
                            }
                        }
                    }
                    return String.format("{\"orderId\":\"%s\",\"sold\":%d,\"returned\":%d}",
                            o.getId(), qty, ret);
                })
                .filter(s -> !s.contains("\"sold\":0"))
                .limit(20)
                .collect(Collectors.joining(",\n    ", "[\n    ", "\n  ]"));

        String userMessage = """
                {
                  "productId":        "%s",
                  "productName":      "%s",
                  "category":         "%s",
                  "rating":            %s,
                  "reviewCount":       %d,
                  "lookbackDays":      %d,
                  "totalSold":         %d,
                  "totalReturned":     %d,
                  "returnRatePct":     %.1f,
                  "orderSummary":      %s
                }
                """.formatted(
                productId,
                nvl(product.getName()),
                nvl(product.getCategory()),
                nvlDec(product.getRating()),
                nvlInt(product.getReviewCount()),
                lookbackDays,
                totalSold,
                totalReturned,
                returnRatePct,
                orderSummary);

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        ReturnAnalysisResult result = PromptJsonExtractor.parse(
                rawResponse, ReturnAnalysisResult.class, FEATURE.name());

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(productId)
                .entityType("PRODUCT")
                .score(result.returnScore())
                .riskLevel(result.riskLevel())
                .recommendation(result.recommendation())
                .signals(result.suspectedCauses() != null ? String.join(",", result.suspectedCauses()) : "")
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());

        return result;
    }

    private int    nvlInt(Integer i)              { return i != null ? i : 0; }
    private String nvl(Object o)                  { return o != null ? o.toString() : ""; }
    private String nvlDec(java.math.BigDecimal d) { return d != null ? d.toPlainString() : "0.0"; }
    private String nvlDec(Double d)               { return d != null ? d.toString() : "0.0"; }

    /** ReturnAnalysisResult — the model's return pattern assessment. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReturnAnalysisResult(
        /** 0 = excellent return rate, 100 = critical problem. Maps to AiInsight.score. */
        int returnScore,
        /** LOW | MEDIUM | HIGH | CRITICAL */
        String riskLevel,
        /** Most likely root causes, e.g. ["description_mismatch", "sizing_issue"]. */
        List<String> suspectedCauses,
        /** MONITOR | CONTACT_SUPPLIER | UPDATE_LISTING | SUSPEND_PRODUCT | REVIEW_FULFILLMENT */
        String recommendation,
        /** Plain-English explanation for the operations and merchandising teams. */
        String reasoning
    ) {}
}
