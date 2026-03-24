package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.BudgetAnomalyService;
import com.commerce_pro_backend.ai.service.BudgetAnomalyService.AnomalyResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiBudgetAnomalyController — AI-powered budget variance anomaly detection.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/budget-anomaly}
 *
 * <h3>Typical integration</h3>
 * The finance module calls this when a budget is submitted for approval, or when
 * the CFO wants a quick AI-generated variance commentary for the board report.
 */
@RestController
@RequestMapping("/v1/ai/budget-anomaly")
@RequiredArgsConstructor
public class AiBudgetAnomalyController {

    private final BudgetAnomalyService budgetAnomalyService;

    /**
     * POST /api/v1/ai/budget-anomaly/budgets/{budgetId}
     *
     * <p>Analyses the budget's line variances and flags anomalies.
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "anomalyScore": 78, "severity": "HIGH",
     *     "anomalousLines": ["Marketing", "R&D"],
     *     "anomalyTypes": ["overspend", "underspend"],
     *     "recommendation": "INVESTIGATE",
     *     "reasoning": "Marketing is 57.5% over budget..."
     *   }
     * }
     * </pre>
     *
     * @param budgetId  UUID of the Budget to analyse
     */
    @PostMapping("/budgets/{budgetId}")
    @PreAuthorize("hasAuthority('ai:budget:analyse')")
    public ResponseEntity<ApiResponse<AnomalyResult>> detect(
            @PathVariable String budgetId) {

        AnomalyResult result = budgetAnomalyService.detect(budgetId);
        return ResponseEntity.ok(ApiResponse.success("Budget anomaly detection complete", result));
    }
}
