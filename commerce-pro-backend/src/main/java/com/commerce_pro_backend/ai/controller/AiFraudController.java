package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.FraudDetectionService;
import com.commerce_pro_backend.ai.service.FraudDetectionService.FraudResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AiFraudController — REST interface for on-demand AI fraud scoring.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/fraud}
 *
 * <h3>Typical flow</h3>
 * <ol>
 *   <li>Payment service creates a new {@code PaymentTransaction} record.
 *   <li>Payment service calls {@code POST /api/v1/ai/fraud/{transactionId}} immediately.
 *   <li>This controller delegates to {@link FraudDetectionService#analyse(String)}.
 *   <li>The response includes score, level, recommendation, signals, and reasoning.
 *   <li>Frontend shows a risk badge on the transaction detail page.
 * </ol>
 *
 * <h3>Security</h3>
 * Restricted to the ADMIN role.  Internal service-to-service calls authenticate via
 * the same JWT mechanism as human admin users.
 */
@RestController
@RequestMapping("/api/v1/ai/fraud")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiFraudController {

    private final FraudDetectionService fraudService;

    /**
     * POST /api/v1/ai/fraud/{transactionId}
     *
     * <p>Triggers an AI fraud analysis for the given transaction.
     * The transaction's {@code riskScore} and {@code isFlagged} fields are updated
     * as a side effect, so the caller does not need to make a separate PATCH request.
     *
     * <h3>Example request</h3>
     * <pre>
     *   POST /api/v1/ai/fraud/txn-abc-123-uuid
     *   Authorization: Bearer &lt;admin-jwt&gt;
     *   (no body required)
     * </pre>
     *
     * <h3>Example success response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "riskScore": 82,
     *     "riskLevel": "HIGH",
     *     "recommendation": "REVIEW",
     *     "signals": ["large_amount_first_order", "ip_mismatch"],
     *     "reasoning": "This is the customer's first ever transaction and the amount
     *                   ($999) is significantly above the average first-order value.
     *                   The billing country does not match the IP geolocation."
     *   }
     * }
     * </pre>
     *
     * <h3>Possible error responses</h3>
     * <ul>
     *   <li>503 — AI_DISABLED (feature toggled off in AiConfig)
     *   <li>429 — AI_RATE_LIMITED or AI_BUDGET_EXCEEDED
     *   <li>404 — transaction not found
     *   <li>500 — AI_PARSE_ERROR (model returned unparseable content)
     * </ul>
     *
     * @param transactionId  UUID of the PaymentTransaction to evaluate
     */
    @PostMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<FraudResult>> analyseTransaction(
            @PathVariable String transactionId) {

        FraudResult result = fraudService.analyse(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Fraud analysis complete", result));
    }
}
