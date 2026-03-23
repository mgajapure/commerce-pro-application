package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.InventoryOptimizationService;
import com.commerce_pro_backend.ai.service.InventoryOptimizationService.OptimizationResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiInventoryController — AI-powered inventory optimisation recommendations.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/inventory}
 *
 * <h3>Typical integration</h3>
 * The warehouse management page shows an "AI Advice" badge for each product row.
 * Clicking it calls this endpoint; the response tells the warehouse manager whether
 * to restock, reduce, or hold — with urgency and recommended quantity.
 */
@RestController
@RequestMapping("/api/v1/ai/inventory")
@RequiredArgsConstructor
public class AiInventoryController {

    private final InventoryOptimizationService inventoryService;

    /**
     * POST /api/v1/ai/inventory/products/{productId}?warehouseId={warehouseId}
     *
     * <p>Returns an inventory optimisation recommendation.
     * {@code warehouseId} is optional — omit to analyse the primary warehouse.
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "action": "RESTOCK", "urgency": "MEDIUM",
     *     "recommendedQuantity": 100,
     *     "reasoning": "Available stock approaching the reorder point of 20..."
     *   }
     * }
     * </pre>
     *
     * @param productId   UUID of the product
     * @param warehouseId optional UUID of a specific warehouse
     */
    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('ai:inventory:optimise')")
    public ResponseEntity<ApiResponse<OptimizationResult>> optimise(
            @PathVariable String productId,
            @RequestParam(required = false) String warehouseId) {

        OptimizationResult result = inventoryService.optimize(productId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success("Inventory optimisation complete", result));
    }
}
