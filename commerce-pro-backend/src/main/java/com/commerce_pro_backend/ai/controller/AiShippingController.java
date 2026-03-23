package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.ShippingOptimizationService;
import com.commerce_pro_backend.ai.service.ShippingOptimizationService.ShippingResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiShippingController — AI-powered carrier and service level optimisation.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/shipping}
 *
 * <h3>Typical integration</h3>
 * The fulfilment workflow calls this after an order is confirmed to get the
 * recommended carrier before printing the shipping label.  The fulfillment team
 * can accept the recommendation or override it manually.
 */
@RestController
@RequestMapping("/api/v1/ai/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiShippingController {

    private final ShippingOptimizationService shippingService;

    /**
     * POST /api/v1/ai/shipping/orders/{orderId}/optimise
     *
     * <p>Returns the optimal carrier and service level for an order.
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "recommendedCarrier": "FedEx",
     *     "recommendedMethod":  "Ground",
     *     "estimatedDays":       3,
     *     "estimatedCostUsd":   8.50,
     *     "reasoning": "1.2 kg domestic parcel qualifies for FedEx Ground at $8.50...",
     *     "signals": ["domestic_eligible", "light_parcel", "cost_saving_opportunity"]
     *   }
     * }
     * </pre>
     *
     * @param orderId  UUID of the order to optimise shipping for
     */
    @PostMapping("/orders/{orderId}/optimise")
    public ResponseEntity<ApiResponse<ShippingResult>> optimise(
            @PathVariable String orderId) {

        ShippingResult result = shippingService.optimise(orderId);
        return ResponseEntity.ok(ApiResponse.success("Shipping optimisation complete", result));
    }
}
