package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.catalog.product.entity.Product;
import com.commerce_pro_backend.catalog.product.repository.ProductRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * PricingRecommendationService — uses AI to recommend an optimal retail price
 * for a product based on margin, sales velocity, and market positioning.
 *
 * <h3>Pricing strategy the model applies</h3>
 * The model is instructed to balance three competing objectives:
 * <ul>
 *   <li><b>Margin protection</b>  — recommended price ≥ cost × minimum margin multiplier.</li>
 *   <li><b>Sales velocity</b>     — if sales are slowing, recommend a small price reduction.</li>
 *   <li><b>Competitive positioning</b> — use the compareAtPrice (was-price) to anchor value.</li>
 * </ul>
 *
 * <h3>Example prompt payload</h3>
 * <pre>
 * {
 *   "productId":         "prod_abc",
 *   "productName":       "Wireless Headphones Pro",
 *   "currentPrice":      199.99,
 *   "costPrice":          80.00,
 *   "compareAtPrice":    249.99,
 *   "currentMarginPct":  60.0,
 *   "salesCount":        342,
 *   "revenue":           68396.58,
 *   "rating":            4.3,
 *   "reviewCount":       87,
 *   "stockStatus":       "IN_STOCK",
 *   "stock":             45
 * }
 * </pre>
 *
 * <h3>Example model response → {@link PricingResult}</h3>
 * <pre>
 * {
 *   "recommendedPrice":  189.99,
 *   "minAcceptablePrice": 160.00,
 *   "maxRecommendedPrice": 219.99,
 *   "expectedMarginPct":  57.9,
 *   "rationale":         "Reducing price by ~5% to $189.99 improves value perception
 *                         while maintaining a healthy 58% margin. The $249.99 compare-at
 *                         price creates a strong anchor effect.",
 *   "signals":           ["healthy_margin", "strong_reviews", "mid_velocity"]
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PricingRecommendationService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final ProductRepository   productRepo;

    private static final AiFeatureType FEATURE = AiFeatureType.PRICING_RECOMMENDATION;

    /**
     * System prompt for pricing recommendations.
     *
     * <p>Design notes:
     * - "never recommend below cost × 1.2" is a hard floor rule that prevents
     *   the model from suggesting a loss-making price.
     * - minAcceptablePrice and maxRecommendedPrice give the admin a sensible range
     *   so they can set a price manually within the AI-recommended band.
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert e-commerce pricing strategist.
            Recommend an optimal retail price for the product based on the data provided.

            Rules:
            - Never recommend a price below cost × 1.20 (minimum 20% margin).
            - Balance margin, sales velocity, and competitive positioning.
            - If sales are declining and margin is high, suggest a moderate reduction.
            - If stock is low and sales are strong, suggest a slight increase.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "recommendedPrice":    <decimal — the single best price to set>,
              "minAcceptablePrice":  <decimal — lowest price that still makes business sense>,
              "maxRecommendedPrice": <decimal — highest price the market will likely accept>,
              "expectedMarginPct":   <decimal — margin % at the recommended price>,
              "rationale":           <plain-English explanation, 2-3 sentences>,
              "signals":             [<pricing signals, e.g. "strong_reviews", "low_velocity">]
            }
            """;

    /**
     * Generate a pricing recommendation for a product and persist the insight.
     *
     * @param productId  UUID of the product to price
     * @return populated {@link PricingResult}
     *
     * <p>Example:
     * <pre>
     *   PricingResult r = pricingService.recommend("prod-uuid");
     *   // r.recommendedPrice()  → 189.99
     *   // r.expectedMarginPct() → 57.9
     * </pre>
     */
    @Transactional
    public PricingResult recommend(String productId) {
        log.info("Pricing recommendation requested for product: {}", productId);

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        // Compute current margin % = (price - cost) / price × 100
        // Example: price=$199.99, cost=$80 → margin = 60.0%
        double marginPct = computeMarginPct(product.getPrice(), product.getCost());

        String userMessage = """
                {
                  "productId":          "%s",
                  "productName":        "%s",
                  "currentPrice":        %s,
                  "costPrice":           %s,
                  "compareAtPrice":      %s,
                  "currentMarginPct":    %.1f,
                  "salesCount":          %d,
                  "revenue":             %s,
                  "rating":              %s,
                  "reviewCount":         %d,
                  "stockStatus":        "%s",
                  "stock":               %d
                }
                """.formatted(
                product.getId(),
                nvl(product.getName()),
                nvlDec(product.getPrice()),
                nvlDec(product.getCost()),
                nvlDec(product.getCompareAtPrice()),
                marginPct,
                nvlInt(product.getSalesCount()),
                nvlDec(product.getRevenue()),
                nvlDec(product.getRating()),
                nvlInt(product.getReviewCount()),
                nvl(product.getStockStatus()),
                nvlInt(product.getStock()));

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        PricingResult result = PromptJsonExtractor.parse(rawResponse, PricingResult.class, FEATURE.name());

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(productId)
                .entityType("PRODUCT")
                .score((int) result.expectedMarginPercent())
                .recommendation("REPRICE")
                .reasoning(result.rationale())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());

        return new PricingResult(
                result.recommendedPrice(),
                result.minAcceptablePrice(),
                result.maxAcceptablePrice(),
                result.expectedMarginPercent(),
                result.rationale(),
                result.signals(),
                productId
        );
    }

    /**
     * Compute margin percentage from price and cost.
     *
     * <p>Example: price=199.99, cost=80.00 → (199.99 - 80.00) / 199.99 × 100 = 60.0%
     * Returns 0.0 if price or cost is null or price is zero.
     */
    private double computeMarginPct(BigDecimal price, BigDecimal cost) {
        if (price == null || cost == null || price.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return price.subtract(cost)
                .divide(price, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private String nvl(Object o)       { return o != null ? o.toString() : ""; }
    private String nvlDec(BigDecimal d) { return d != null ? d.toPlainString() : "0.00"; }
    private String nvlDec(Double d)     { return d != null ? String.valueOf(d) : "0.0"; }
    private int    nvlInt(Integer i)    { return i != null ? i : 0; }

    /** PricingResult — the model's pricing recommendation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingResult(
        /** The single best price to set (e.g. 189.99). */
        BigDecimal recommendedPrice,
        /** Lowest price that still makes business sense. */
        BigDecimal minAcceptablePrice,
        /** Highest price the market will likely accept. */
        BigDecimal maxAcceptablePrice,
        /** Margin % at the recommended price (e.g. 57.9). */
        double expectedMarginPercent,
        /** Plain-English explanation for the merchandising team. */
        String rationale,
        /** Signals that drove the recommendation, e.g. ["strong_reviews", "high_stock"]. */
        List<String> signals,
        /** Product ID for traceability. */
        String productId
    ) {}
}
