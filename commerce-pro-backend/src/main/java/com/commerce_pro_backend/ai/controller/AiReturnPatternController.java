package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.ReturnPatternAnalysisService;
import com.commerce_pro_backend.ai.service.ReturnPatternAnalysisService.ReturnAnalysisResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiReturnPatternController — AI root-cause analysis for product return rates.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/returns}
 *
 * <h3>Typical integration</h3>
 * The operations dashboard flags products with &gt;10% return rates.
 * Clicking the "Analyse" button calls this endpoint to get the AI's diagnosis.
 */
@RestController
@RequestMapping("/v1/ai/returns")
@RequiredArgsConstructor
public class AiReturnPatternController {

    private final ReturnPatternAnalysisService returnService;

    /**
     * POST /api/v1/ai/returns/products/{productId}?days={lookbackDays}
     *
     * <p>Analyses the return pattern for a product over the specified window.
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "returnScore": 72, "riskLevel": "HIGH",
     *     "suspectedCauses": ["description_mismatch", "sizing_issue"],
     *     "recommendation": "UPDATE_LISTING",
     *     "reasoning": "28% return rate far exceeds the 10% healthy threshold..."
     *   }
     * }
     * </pre>
     *
     * @param productId    UUID of the product
     * @param lookbackDays days of history to include (default 90)
     */
    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('ai:returns:analyse')")
    public ResponseEntity<ApiResponse<ReturnAnalysisResult>> analyse(
            @PathVariable String productId,
            @RequestParam(defaultValue = "90") int lookbackDays) {

        ReturnAnalysisResult result = returnService.analyse(productId, lookbackDays);
        return ResponseEntity.ok(ApiResponse.success("Return pattern analysis complete", result));
    }
}
