package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.VendorAnalysisService;
import com.commerce_pro_backend.ai.service.VendorAnalysisService.VendorAnalysisResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiVendorController — AI-powered vendor performance scorecard.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/vendors}
 *
 * <h3>Typical integration</h3>
 * The procurement team opens a vendor profile; the vendor detail page calls this
 * endpoint to show a live AI performance scorecard alongside payment history.
 */
@RestController
@RequestMapping("/api/v1/ai/vendors")
@RequiredArgsConstructor
public class AiVendorController {

    private final VendorAnalysisService vendorService;

    /**
     * POST /api/v1/ai/vendors/{vendorId}/analyse
     *
     * <p>Evaluates the vendor's financial relationship and returns a scorecard.
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "performanceScore": 72, "riskLevel": "MEDIUM",
     *     "strengths": ["long_payment_history"],
     *     "weaknesses": ["high_outstanding_payable"],
     *     "recommendation": "RENEGOTIATE_TERMS",
     *     "reasoning": "Strong 3-year history but $42k outstanding on 60-day terms..."
     *   }
     * }
     * </pre>
     *
     * @param vendorId  UUID of the vendor
     */
    @PostMapping("/{vendorId}/analyse")
    @PreAuthorize("hasAuthority('ai:vendor:analyse')")
    public ResponseEntity<ApiResponse<VendorAnalysisResult>> analyse(
            @PathVariable String vendorId) {

        VendorAnalysisResult result = vendorService.analyse(vendorId);
        return ResponseEntity.ok(ApiResponse.success("Vendor analysis complete", result));
    }
}
