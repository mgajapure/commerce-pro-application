package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.AiDemandForecastService;
import com.commerce_pro_backend.ai.service.AiDemandForecastService.ForecastResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiDemandForecastController — on-demand AI demand forecasting for products.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/forecast}
 *
 * <h3>Typical integration</h3>
 * The procurement team calls this when preparing a purchase order.
 * The response tells them exactly how many units to order and at what urgency.
 */
@RestController
@RequestMapping("/v1/ai/forecast")
@RequiredArgsConstructor
public class AiDemandForecastController {

    private final AiDemandForecastService forecastService;

    /**
     * POST /api/v1/ai/forecast/products/{productId}?days={forecastDays}
     *
     * <p>Generates a demand forecast for the product over the specified horizon.
     *
     * <h3>Example request</h3>
     * <pre>
     *   POST /api/v1/ai/forecast/products/prod-uuid?days=30
     *   Authorization: Bearer &lt;admin-jwt&gt;
     * </pre>
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "predictedDemand": 75, "reorderPoint": 25, "safetyStock": 15,
     *     "recommendedOrderQty": 120, "confidence": "HIGH",
     *     "signals": ["upward_trend"], "reasoning": "Monthly volume up 33%..."
     *   }
     * }
     * </pre>
     *
     * @param productId    UUID of the product
     * @param forecastDays number of days to forecast (default 30)
     */
    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('ai:forecast:run')")
    public ResponseEntity<ApiResponse<ForecastResult>> forecast(
            @PathVariable String productId,
            @RequestParam(defaultValue = "30") int forecastDays) {

        ForecastResult result = forecastService.forecast(productId, forecastDays);
        return ResponseEntity.ok(ApiResponse.success("Demand forecast complete", result));
    }
}
