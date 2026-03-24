package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.catalog.review.entity.Review;
import com.commerce_pro_backend.catalog.review.repository.ReviewRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SentimentAnalysisService — uses AI to analyse the sentiment of a product review.
 *
 * <h3>Why AI and not a keyword-based approach?</h3>
 * Keyword matching (e.g. "great" = positive, "bad" = negative) fails on nuanced
 * feedback such as "not bad at all", sarcasm, or mixed reviews ("love the design,
 * hate the price").  The AI model understands context and extracts themes, which
 * enables the product team to act on specific improvement areas.
 *
 * <h3>Processing pipeline</h3>
 * <ol>
 *   <li>Load the {@link Review} by ID.
 *   <li>Build a prompt containing the review title, body, star rating, and verified status.
 *   <li>Ask the model to return a sentiment score, label, extracted themes,
 *       and brief reasoning — all as JSON.
 *   <li>Parse the response and persist an {@link AiInsight}.
 * </ol>
 *
 * <h3>Sentiment score semantics</h3>
 * <pre>
 *   -100  = extremely negative   (e.g. "terrible product, broke on day one")
 *    -50  = moderately negative
 *      0  = neutral / mixed
 *    +50  = moderately positive
 *   +100  = extremely positive   (e.g. "best purchase I've ever made!")
 * </pre>
 *
 * <h3>Example prompt payload</h3>
 * <pre>
 * {
 *   "reviewId":          "rev_abc",
 *   "productId":         "prod_xyz",
 *   "starRating":        3,
 *   "title":             "Decent but overpriced",
 *   "content":           "The quality is acceptable but I expected more for the price.
 *                         Delivery was fast though.",
 *   "isVerifiedPurchase": true
 * }
 * </pre>
 *
 * <h3>Example model response</h3>
 * <pre>
 * {
 *   "sentimentScore": 10,
 *   "label":          "NEUTRAL",
 *   "themes":         ["value_for_money", "delivery_speed", "product_quality"],
 *   "reasoning":      "The reviewer has mixed feelings — they appreciate fast delivery
 *                      but feel the product does not justify its price point. The 3-star
 *                      rating is consistent with a balanced but slightly disappointed view."
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SentimentAnalysisService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final ReviewRepository    reviewRepo;

    /** Feature constant — controls rate limits, model, and budget for sentiment analysis. */
    private static final AiFeatureType FEATURE = AiFeatureType.SENTIMENT_ANALYSIS;

    /**
     * System prompt for sentiment analysis.
     *
     * <p>Design notes:
     * - "Do not let the star rating override your sentiment analysis" is critical.
     *   A 1-star review can still contain constructive praise ("customer service was great"),
     *   and we want to capture that nuance in the themes.
     * - themes is an array of 1-4 items so the dashboard can show concise labels
     *   without overwhelming the product manager.
     * - sentimentScore -100 to +100 is stored in AiInsight.score.
     *   The DB column allows negative values via a signed integer.
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert sentiment analyst for e-commerce product reviews.
            Analyse the review text and infer the customer's true sentiment.
            Do not let the star rating override your analysis — focus on the language.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "sentimentScore": <integer -100 to +100, negative=negative, 0=neutral, positive=positive>,
              "label":          <"VERY_POSITIVE" | "POSITIVE" | "NEUTRAL" | "NEGATIVE" | "VERY_NEGATIVE">,
              "themes":         [<1 to 4 snake_case theme strings, e.g. "product_quality">],
              "reasoning":      <plain-English explanation, 2-3 sentences>
            }

            Label mapping:
              +61 to +100 = VERY_POSITIVE
              +21 to +60  = POSITIVE
              -20 to +20  = NEUTRAL
              -60 to -21  = NEGATIVE
              -100 to -61 = VERY_NEGATIVE
            """;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Analyse the sentiment of a single product review.
     *
     * @param reviewId  UUID of the {@link Review} to analyse
     * @return populated {@link SentimentResult}
     * @throws EntityNotFoundException if the review does not exist
     *
     * <p>Example:
     * <pre>
     *   SentimentResult result = sentimentService.analyse("rev-uuid");
     *   // result.sentimentScore() → 10   (slightly positive overall)
     *   // result.label()          → "NEUTRAL"
     *   // result.themes()         → ["value_for_money", "delivery_speed"]
     * </pre>
     */
    @Transactional
    public SentimentResult analyse(String reviewId) {
        log.info("Starting sentiment analysis for review: {}", reviewId);

        // 1. Load the review record
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));

        // 2. Build the prompt (title + body + star rating + verified status)
        String userMessage = buildSentimentPayload(review);

        // 3. Call the AI model
        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);

        // 4. Parse JSON response into SentimentResult
        SentimentResult result = PromptJsonExtractor.parse(rawResponse, SentimentResult.class, FEATURE.name());

        // 5. Persist insight — entity is the product (not the review) so we can
        //    aggregate sentiment across all reviews for a product later.
        persistInsight(review, result);

        log.info("Sentiment analysis complete for review {}: score={}, label={}",
                reviewId, result.sentimentScore(), result.label());

        return result;
    }

    /**
     * Analyse a batch of reviews for a product and return one result per review.
     *
     * <p>Useful for the product detail admin page which shows a sentiment breakdown
     * for all reviews at once.
     *
     * <p>Example:
     * <pre>
     *   // Analyse all 5 pending reviews for product "prod-123"
     *   List&lt;String&gt; reviewIds = List.of("rev1", "rev2", "rev3", "rev4", "rev5");
     *   List&lt;SentimentResult&gt; results = sentimentService.analyseBatch(reviewIds);
     * </pre>
     *
     * @param reviewIds  list of review UUIDs to analyse
     * @return list of {@link SentimentResult} in the same order as {@code reviewIds};
     *         any review that fails is skipped (logged as a warning)
     */
    public List<SentimentResult> analyseBatch(List<String> reviewIds) {
        return reviewIds.stream()
                .map(id -> {
                    try {
                        return analyse(id);
                    } catch (Exception ex) {
                        // Log and skip failures so one bad review doesn't abort the batch
                        log.warn("Sentiment analysis failed for review {}: {}", id, ex.getMessage());
                        return null;
                    }
                })
                .filter(r -> r != null)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build the review payload as a JSON string for the model.
     *
     * <p>Including {@code isVerifiedPurchase} helps the model weigh the credibility
     * of the review — unverified reviews sometimes contain competitor spam or fake
     * positive/negative content.
     *
     * <p>Example output:
     * <pre>
     * {
     *   "reviewId":          "rev_abc",
     *   "productName":       "Wireless Bluetooth Headphones Pro",
     *   "starRating":        3,
     *   "title":             "Good sound, poor build quality",
     *   "content":           "Bass is impressive but the ear pads fell off after 2 weeks.",
     *   "isVerifiedPurchase": true
     * }
     * </pre>
     */
    private String buildSentimentPayload(Review review) {
        return """
                {
                  "reviewId":           "%s",
                  "productName":        "%s",
                  "starRating":          %d,
                  "title":             "%s",
                  "content":           "%s",
                  "isVerifiedPurchase":  %b
                }
                """.formatted(
                review.getId(),
                nvl(review.getProductName()),
                nvlInt(review.getRating()),
                nvl(review.getTitle()),
                escapeJson(review.getContent()),
                Boolean.TRUE.equals(review.getIsVerifiedPurchase())
        );
    }

    /**
     * Persist an {@link AiInsight} for this sentiment result.
     *
     * <p>The entityId is set to the {@code productId} (not the reviewId) so that
     * {@code AiInsightRepository.findByEntityIdAndFeatureType()} can retrieve all
     * sentiment insights for a product in one query.
     */
    private void persistInsight(Review review, SentimentResult result) {
        String themesCsv = result.themes() != null ? String.join(",", result.themes()) : "";

        // Map label to a canonical risk level understood by AiInsight dashboards.
        // Negative reviews are "high risk" from a product reputation standpoint.
        String riskLevel = mapLabelToRiskLevel(result.label());

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(review.getProductId())   // aggregate by product, not by review
                .entityType("PRODUCT")
                .score(result.sentimentScore())
                .riskLevel(riskLevel)
                .signals(themesCsv)
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());
    }

    /**
     * Map the sentiment label to a risk level compatible with the AiInsight.riskLevel column.
     *
     * <p>Example:
     * <pre>
     *   "VERY_NEGATIVE" → "HIGH"      (product has a reputation problem)
     *   "NEGATIVE"      → "MEDIUM"
     *   "NEUTRAL"       → "LOW"
     *   "POSITIVE"      → "POSITIVE"
     *   "VERY_POSITIVE" → "POSITIVE"
     * </pre>
     */
    private String mapLabelToRiskLevel(String label) {
        if (label == null) return "LOW";
        return switch (label) {
            case "VERY_NEGATIVE" -> "HIGH";
            case "NEGATIVE"      -> "MEDIUM";
            case "NEUTRAL"       -> "LOW";
            default              -> "POSITIVE"; // POSITIVE or VERY_POSITIVE
        };
    }

    /** Escape double-quotes inside the review content to keep the JSON payload valid. */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private String nvl(Object o)  { return o != null ? o.toString() : ""; }
    private int nvlInt(Integer i)  { return i != null ? i : 0; }

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * SentimentResult — immutable record holding the model's sentiment assessment.
     *
     * <p>Example JSON → record mapping:
     * <pre>
     * JSON:   {"sentimentScore":75,"label":"POSITIVE","themes":["fast_shipping","good_quality"],
     *          "reasoning":"Customer is very happy with delivery speed and build quality."}
     * Record: SentimentResult(sentimentScore=75, label="POSITIVE",
     *                         themes=["fast_shipping","good_quality"], reasoning="...")
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SentimentResult(
        /** -100 (very negative) to +100 (very positive). Stored in AiInsight.score. */
        int sentimentScore,

        /** VERY_POSITIVE | POSITIVE | NEUTRAL | NEGATIVE | VERY_NEGATIVE */
        String label,

        /** Key themes extracted from the review text, e.g. ["product_quality", "delivery_speed"]. */
        List<String> themes,

        /** Plain-English explanation for the product / marketing team. */
        String reasoning
    ) {}
}
