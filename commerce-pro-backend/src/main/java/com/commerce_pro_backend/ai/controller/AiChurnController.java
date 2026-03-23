package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.ChurnPredictionService;
import com.commerce_pro_backend.ai.service.ChurnPredictionService.ChurnResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AiChurnController — on-demand AI churn scoring for individual customers.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/churn}
 *
 * <h3>Typical integration pattern</h3>
 * The CRM dashboard calls this endpoint when a customer support agent opens a
 * customer profile.  The response is shown as a "Retention Risk" widget that
 * shows the score, recommended action, and the key signals that drove the score.
 *
 * <h3>Batch use</h3>
 * For bulk churn scoring (e.g., weekly all-customer scan), a scheduled job calls
 * {@link ChurnPredictionService#predict(String)} directly without going through
 * this controller.
 */
@RestController
@RequestMapping("/api/v1/ai/churn")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiChurnController {

    private final ChurnPredictionService churnService;

    /**
     * POST /api/v1/ai/churn/{customerId}
     *
     * <p>Runs churn prediction for the specified customer and returns the assessment.
     * An {@link com.commerce_pro_backend.ai.entity.AiInsight} is persisted as a side effect
     * so the result is visible in the AI insights history.
     *
     * <h3>Example request</h3>
     * <pre>
     *   POST /api/v1/ai/churn/cust-abc-123-uuid
     *   Authorization: Bearer &lt;admin-jwt&gt;
     *   (no body required)
     * </pre>
     *
     * <h3>Example success response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "churnScore": 72,
     *     "riskLevel": "HIGH",
     *     "recommendation": "WIN_BACK_EMAIL",
     *     "signals": ["no_order_75_days", "increasing_order_gap", "high_return_rate"],
     *     "reasoning": "Customer has not ordered in 75 days despite averaging orders
     *                   every 21 days historically. A win-back email campaign with a
     *                   15% discount is the recommended first step."
     *   }
     * }
     * </pre>
     *
     * @param customerId  UUID of the Customer to evaluate
     */
    @PostMapping("/{customerId}")
    public ResponseEntity<ApiResponse<ChurnResult>> predictChurn(
            @PathVariable String customerId) {

        ChurnResult result = churnService.predict(customerId);
        return ResponseEntity.ok(ApiResponse.success("Churn prediction complete", result));
    }
}
