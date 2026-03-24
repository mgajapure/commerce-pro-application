package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.PricingRecommendationService;
import com.commerce_pro_backend.ai.service.PricingRecommendationService.PricingResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiPricingController — AI-powered product pricing recommendations.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/pricing}
 *
 * <h3>Typical integration</h3>
 * The merchandising team opens a product's pricing panel; the frontend calls
 * this endpoint to show an AI-recommended price range alongside the current price.
 * The admin can accept or override the recommendation.
 */
@RestController
@RequestMapping("/v1/ai/pricing")
@RequiredArgsConstructor
public class AiPricingController {

    private final PricingRecommendationService pricingService;

    /**
     * POST /api/v1/ai/pricing/products/{productId}
     *
     * <p>Returns a pricing recommendation for the product.
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "recommendedPrice": 189.99,
     *     "minAcceptablePrice": 160.00,
     *     "maxRecommendedPrice": 219.99,
     *     "expectedMarginPct": 57.9,
     *     "rationale": "Reducing by ~5% improves value perception while maintaining 58% margin.",
     *     "signals": ["healthy_margin", "strong_reviews", "mid_velocity"]
     *   }
     * }
     * </pre>
     *
     * @param productId  UUID of the product to price
     */
    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('ai:pricing:run')")
    public ResponseEntity<ApiResponse<PricingResult>> recommend(
            @PathVariable String productId) {

        PricingResult result = pricingService.recommend(productId);
        return ResponseEntity.ok(ApiResponse.success("Pricing recommendation ready", result));
    }
}
