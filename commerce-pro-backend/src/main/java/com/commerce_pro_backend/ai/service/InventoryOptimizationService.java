package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.inventory.entity.Inventory;
import com.commerce_pro_backend.inventory.repository.InventoryRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * InventoryOptimizationService — uses AI to recommend the right inventory action
 * (restock, reduce, or hold) for a specific product-warehouse combination.
 *
 * <h3>Problem context</h3>
 * Too much stock ties up working capital; too little causes stockouts and lost sales.
 * This service gives the warehouse manager an AI recommendation on what action to
 * take, with urgency level and a plain-English explanation.
 *
 * <h3>Example prompt payload</h3>
 * <pre>
 * {
 *   "productId":      "prod_abc",
 *   "warehouseId":    "wh_main",
 *   "quantity":        45,
 *   "reserved":        12,
 *   "available":       33,
 *   "incoming":         0,
 *   "reorderPoint":    20,
 *   "reorderQuantity": 100,
 *   "safetyStock":     10,
 *   "maxStockLevel":  500,
 *   "status":         "IN_STOCK",
 *   "unitCost":       80.00,
 *   "totalValue":    3600.00
 * }
 * </pre>
 *
 * <h3>Example model response → {@link OptimizationResult}</h3>
 * <pre>
 * {
 *   "action":             "RESTOCK",
 *   "urgency":            "MEDIUM",
 *   "recommendedQuantity": 100,
 *   "reasoning":          "Available stock (33 units) is approaching the reorder point
 *                          of 20. With no incoming stock and a safety stock of 10,
 *                          a restock of 100 units is recommended before available
 *                          inventory drops below the safety threshold."
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryOptimizationService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final InventoryRepository inventoryRepo;

    private static final AiFeatureType FEATURE = AiFeatureType.INVENTORY_OPTIMIZATION;

    private static final String SYSTEM_PROMPT = """
            You are an expert inventory management specialist.
            Analyse the product's current stock levels against its configuration
            and recommend the optimal inventory action.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "action":              <"RESTOCK" | "REDUCE" | "HOLD">,
              "urgency":             <"CRITICAL" | "HIGH" | "MEDIUM" | "LOW">,
              "recommendedQuantity": <integer — units to order (RESTOCK) or liquidate (REDUCE), 0 for HOLD>,
              "reasoning":           <plain-English explanation, 2-3 sentences>
            }

            Action guidelines:
              RESTOCK  — available stock is approaching or below reorder point
              REDUCE   — stock exceeds max level, or velocity is too low for current holdings
              HOLD     — stock is healthy; no action needed
            """;

    /**
     * Generate an inventory optimization recommendation for a product-warehouse pair.
     *
     * @param productId    UUID of the product
     * @param warehouseId  UUID of the warehouse (null = use primary / default warehouse)
     * @return populated {@link OptimizationResult}
     *
     * <p>Example:
     * <pre>
     *   OptimizationResult r = inventoryService.optimize("prod-uuid", "wh-uuid");
     *   // r.action()              → "RESTOCK"
     *   // r.urgency()             → "MEDIUM"
     *   // r.recommendedQuantity() → 100
     * </pre>
     */
    @Transactional
    public OptimizationResult optimize(String productId, String warehouseId) {
        log.info("Inventory optimization requested for product={}, warehouse={}", productId, warehouseId);

        // Load either the specific warehouse record or all records for the product
        List<Inventory> inventories = warehouseId != null
                ? inventoryRepo.findByProductId(productId).stream()
                        .filter(i -> warehouseId.equals(
                                i.getWarehouse() != null ? i.getWarehouse().getId() : null))
                        .collect(Collectors.toList())
                : inventoryRepo.findByProductId(productId);

        if (inventories.isEmpty()) {
            throw new EntityNotFoundException("No inventory found for product: " + productId);
        }

        // Use the first matching record (or primary warehouse)
        Inventory inv = inventories.get(0);

        String userMessage = """
                {
                  "productId":          "%s",
                  "warehouseId":        "%s",
                  "quantity":            %d,
                  "reserved":            %d,
                  "available":           %d,
                  "incoming":            %d,
                  "reorderPoint":        %d,
                  "reorderQuantity":     %d,
                  "safetyStock":         %d,
                  "maxStockLevel":       %d,
                  "status":             "%s",
                  "unitCost":            %s,
                  "totalValue":          %s
                }
                """.formatted(
                productId,
                inv.getWarehouse() != null ? inv.getWarehouse().getId() : "unknown",
                nvlInt(inv.getQuantity()),
                nvlInt(inv.getReserved()),
                nvlInt(inv.getAvailable()),
                nvlInt(inv.getIncoming()),
                nvlInt(inv.getReorderPoint()),
                nvlInt(inv.getReorderQuantity()),
                nvlInt(inv.getSafetyStock()),
                nvlInt(inv.getMaxStockLevel()),
                nvl(inv.getStatus()),
                nvlDec(inv.getUnitCost()),
                nvlDec(inv.getTotalValue()));

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        OptimizationResult result = PromptJsonExtractor.parse(rawResponse, OptimizationResult.class, FEATURE.name());

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(productId)
                .entityType("PRODUCT")
                .riskLevel(result.urgency())
                .recommendation(result.action())
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());

        return result;
    }

    private int    nvlInt(Integer i)           { return i != null ? i : 0; }
    private String nvl(Object o)               { return o != null ? o.toString() : "UNKNOWN"; }
    private String nvlDec(java.math.BigDecimal d) { return d != null ? d.toPlainString() : "0.00"; }

    /** OptimizationResult — the model's inventory action recommendation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OptimizationResult(
        /** RESTOCK | REDUCE | HOLD */
        String action,
        /** CRITICAL | HIGH | MEDIUM | LOW — how urgently to act. */
        String urgency,
        /** Units to order (RESTOCK) or liquidate (REDUCE). Zero for HOLD. */
        int recommendedQuantity,
        /** Plain-English explanation for the warehouse manager. */
        String reasoning
    ) {}
}
