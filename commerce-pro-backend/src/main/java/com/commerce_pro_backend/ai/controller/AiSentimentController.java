package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.SentimentAnalysisService;
import com.commerce_pro_backend.ai.service.SentimentAnalysisService.SentimentResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AiSentimentController — REST interface for AI-powered review sentiment analysis.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/sentiment}
 *
 * <h3>Typical integration patterns</h3>
 * <ul>
 *   <li><b>On review submission:</b> the review service calls the single-review
 *       endpoint immediately so the admin dashboard can display a sentiment badge
 *       before a human moderator reviews the content.</li>
 *   <li><b>Bulk backfill:</b> the admin calls the batch endpoint with a list of
 *       review IDs to analyse all historical reviews for a product at once.</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/ai/sentiment")
@RequiredArgsConstructor
public class AiSentimentController {

    private final SentimentAnalysisService sentimentService;

    /**
     * POST /api/v1/ai/sentiment/reviews/{reviewId}
     *
     * <p>Analyses the sentiment of a single product review.
     *
     * <h3>Example request</h3>
     * <pre>
     *   POST /api/v1/ai/sentiment/reviews/rev-abc-123-uuid
     *   Authorization: Bearer &lt;admin-jwt&gt;
     * </pre>
     *
     * <h3>Example success response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "sentimentScore": 75,
     *     "label": "POSITIVE",
     *     "themes": ["product_quality", "value_for_money"],
     *     "reasoning": "The customer is satisfied with the product's build quality
     *                   and feels it offers good value. The language is enthusiastic
     *                   and contains no negative qualifiers."
     *   }
     * }
     * </pre>
     *
     * @param reviewId  UUID of the Review to analyse
     */
    @PostMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAuthority('ai:sentiment:analyse')")
    public ResponseEntity<ApiResponse<SentimentResult>> analyseReview(
            @PathVariable String reviewId) {

        SentimentResult result = sentimentService.analyse(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Sentiment analysis complete", result));
    }

    /**
     * POST /api/v1/ai/sentiment/reviews/batch
     *
     * <p>Analyses sentiment for a list of reviews in sequence.
     * Individual failures are skipped and logged; the response contains only
     * the reviews that were successfully analysed.
     *
     * <h3>Example request</h3>
     * <pre>
     * POST /api/v1/ai/sentiment/reviews/batch
     * Content-Type: application/json
     *
     * ["rev-uuid-1", "rev-uuid-2", "rev-uuid-3"]
     * </pre>
     *
     * <h3>Example success response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": [
     *     {"sentimentScore": 80, "label": "POSITIVE",      "themes": ["quality"], ...},
     *     {"sentimentScore": -40, "label": "NEGATIVE",     "themes": ["price"],   ...}
     *   ]
     * }
     * </pre>
     *
     * @param reviewIds  list of Review UUIDs (must be non-empty)
     */
    @PostMapping("/reviews/batch")
    @PreAuthorize("hasAuthority('ai:sentiment:analyse')")
    public ResponseEntity<ApiResponse<List<SentimentResult>>> analyseBatch(
            @RequestBody @NotEmpty List<String> reviewIds) {

        List<SentimentResult> results = sentimentService.analyseBatch(reviewIds);
        return ResponseEntity.ok(ApiResponse.success(
                "Batch sentiment analysis complete (" + results.size() + "/" + reviewIds.size() + " succeeded)",
                results));
    }
}
