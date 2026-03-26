package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.inventory.entity.Inventory;
import com.commerce_pro_backend.inventory.repository.InventoryRepository;
import com.commerce_pro_backend.inventory.repository.StockMovementRepository;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DemandForecastService — uses AI to predict future demand for a product
 * and recommend reorder quantities and safety stock levels.
 *
 * <h3>Why AI over traditional forecasting?</h3>
 * Classic methods (moving average, exponential smoothing) treat every product the
 * same.  AI can detect seasonal spikes, promotion-driven surges, and correlated
 * product demand patterns that time-series formulas miss.
 *
 * <h3>Processing pipeline</h3>
 * <ol>
 *   <li>Load the product's {@link Inventory} record(s) across all warehouses.</li>
 *   <li>Pull the last 90 days of stock movements (IN/OUT) for velocity data.</li>
 *   <li>Pull monthly order counts for the last 6 months for trend context.</li>
 *   <li>Build a prompt containing current stock, movement velocity, and order trends.</li>
 *   <li>Parse the model's response into a {@link ForecastResult}.</li>
 * </ol>
 *
 * <h3>Example prompt payload (sent as user message)</h3>
 * <pre>
 * {
 *   "productId":              "prod_abc",
 *   "productName":            "Wireless Headphones Pro",
 *   "currentStock":            45,
 *   "reorderPoint":            20,
 *   "reorderQuantity":         100,
 *   "safetyStock":             10,
 *   "avgDailyOutflow":         2.3,
 *   "stockMovements90Days":    [{"type":"OUT","qty":7,"date":"2025-01-15"}, ...],
 *   "monthlyOrderTotals":      [120, 135, 98, 142, 110, 160],
 *   "forecastHorizonDays":     30
 * }
 * </pre>
 *
 * <h3>Example model response → {@link ForecastResult}</h3>
 * <pre>
 * {
 *   "predictedDemand":        75,
 *   "reorderPoint":            25,
 *   "safetyStock":             15,
 *   "recommendedOrderQty":     120,
 *   "confidence":             "HIGH",
 *   "signals":                ["upward_trend", "seasonal_spike_detected"],
 *   "reasoning":              "Monthly order volume has grown 33% over 6 months.
 *                              At the current outflow rate of 2.3 units/day, the
 *                              30-day horizon demand is ~70 units. Seasonal data
 *                              suggests a 10% buffer is advisable for this period."
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiDemandForecastService {

    private final AiOrchestrator          orchestrator;
    private final AiInsightRepository     insightRepo;
    private final InventoryRepository     inventoryRepo;
    private final StockMovementRepository movementRepo;
    private final OrderRepository         orderRepo;

    /** Feature constant controlling model, budget, and rate limits for forecasting. */
    private static final AiFeatureType FEATURE = AiFeatureType.DEMAND_FORECAST;

    /**
     * System prompt for demand forecasting.
     *
     * <p>Key design choices:
     * - "Do not hallucinate external data" — prevents the model from inventing
     *   market trends it cannot know about.
     * - We ask for confidence (HIGH/MEDIUM/LOW) so the caller can decide whether
     *   to auto-apply the recommendation or require human approval first.
     * - recommendedOrderQty accounts for both predicted demand AND safety stock.
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert inventory and supply-chain analyst.
            Forecast demand for the product based ONLY on the historical data provided.
            Do not assume external market trends not reflected in the data.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "predictedDemand":     <integer — total units expected in the forecast horizon>,
              "reorderPoint":        <integer — units on hand that should trigger a reorder>,
              "safetyStock":         <integer — minimum buffer to hold at all times>,
              "recommendedOrderQty": <integer — how many units to order now>,
              "confidence":          <"HIGH" | "MEDIUM" | "LOW">,
              "signals":             [<trend signals found in the data, e.g. "upward_trend">],
              "reasoning":           <plain-English explanation, 2-4 sentences>
            }
            """;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generate a demand forecast for a specific product.
     *
     * @param productId         UUID of the product to forecast
     * @param forecastDays      how many days ahead to forecast (e.g. 30, 60, 90)
     * @return populated {@link ForecastResult}
     * @throws EntityNotFoundException if no inventory record exists for the product
     *
     * <p>Example:
     * <pre>
     *   ForecastResult r = forecastService.forecast("prod-uuid", 30);
     *   // r.predictedDemand()     → 75
     *   // r.recommendedOrderQty() → 120
     *   // r.confidence()          → "HIGH"
     * </pre>
     */
    @Transactional
    public ForecastResult forecast(String productId, int forecastDays) {
        log.info("Demand forecast requested for product {} over {} days", productId, forecastDays);

        // 1. Load inventory records (one per warehouse — aggregate totals)
        List<Inventory> inventories = inventoryRepo.findByProductId(productId);
        if (inventories.isEmpty()) {
            throw new EntityNotFoundException("No inventory records found for product: " + productId);
        }

        // 2. Aggregate total current stock across all warehouses
        int totalStock = inventories.stream()
                .mapToInt(inv -> inv.getQuantity() != null ? inv.getQuantity() : 0)
                .sum();

        // Use the first warehouse record's reorder settings as the baseline
        Inventory primary = inventories.get(0);

        // 3. Pull recent stock movements (last 90 days) for velocity calculation
        //    Movements include IN (restocks), OUT (sales fulfillment), and RETURNS
        LocalDateTime since90 = LocalDateTime.now().minusDays(90);
        var movements = movementRepo.findByProductIdOrderByCreatedAtDesc(productId);
        var recent90 = movements.stream()
                .filter(m -> m.getCreatedAt().isAfter(since90))
                .limit(50) // cap at 50 entries to keep prompt size reasonable
                .collect(Collectors.toList());

        // 4. Compute average daily outflow from the OUT movements in the last 90 days
        //    outflow = units sold/fulfilled per day (used as base demand rate)
        double avgDailyOutflow = recent90.stream()
                .filter(m -> m.getType() != null && m.getType().name().equals("OUT"))
                .mapToInt(m -> m.getQuantity() != null ? m.getQuantity() : 0)
                .sum() / 90.0;

        // 5. Build movement summary for the prompt
        //    We only send type + qty + date to keep the payload compact
        String movementSummary = recent90.stream()
                .map(m -> String.format("{\"type\":\"%s\",\"qty\":%d,\"date\":\"%s\"}",
                        m.getType(), nvlInt(m.getQuantity()), m.getCreatedAt().toLocalDate()))
                .collect(Collectors.joining(",\n    ", "[\n    ", "\n  ]"));

        // 6. Build and call the prompt
        String userMessage = """
                {
                  "productId":          "%s",
                  "currentStock":        %d,
                  "reorderPoint":        %d,
                  "reorderQuantity":     %d,
                  "safetyStock":         %d,
                  "avgDailyOutflow":     %.2f,
                  "stockMovements90Days": %s,
                  "forecastHorizonDays": %d
                }
                """.formatted(
                productId,
                totalStock,
                nvlInt(primary.getReorderPoint()),
                nvlInt(primary.getReorderQuantity()),
                nvlInt(primary.getSafetyStock()),
                avgDailyOutflow,
                movementSummary,
                forecastDays);

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        ForecastResult result = PromptJsonExtractor.parse(rawResponse, ForecastResult.class, FEATURE.name());

        // 7. Persist the insight so trend history can be tracked over time
        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(productId)
                .entityType("PRODUCT")
                .score(result.predictedDemand())  // score = predicted demand units
                .riskLevel(result.confidence())   // reuse riskLevel column for confidence tier
                .recommendation(result.recommendedOrderQty() > 0 ? "REORDER" : "HOLD")
                .signals(result.signals() != null ? String.join(",", result.signals()) : "")
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());

        log.info("Forecast complete for product {}: predictedDemand={}, orderQty={}",
                productId, result.predictedDemand(), result.recommendedOrderQty());
        return new ForecastResult(
                result.predictedDemand(),
                result.reorderPoint(),
                result.safetyStock(),
                result.recommendedOrderQty(),
                result.confidence(),
                result.signals(),
                result.reasoning(),
                productId,
                forecastDays
        );
    }

    private int nvlInt(Integer i) { return i != null ? i : 0; }

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * ForecastResult — immutable record holding the model's demand forecast.
     *
     * <p>Example:
     * <pre>
     *   ForecastResult(predictedDemand=75, reorderPoint=25, safetyStock=15,
     *                  recommendedOrderQty=120, confidence="HIGH",
     *                  signals=["upward_trend"], reasoning="...")
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForecastResult(
        /** Total units predicted to be demanded in the forecast window. */
        int predictedDemand,
        /** Units-on-hand level that should trigger a new purchase order. */
        int reorderPoint,
        /** Minimum buffer stock to protect against stockouts. */
        int safetyStock,
        /** How many units to order now. */
        int recommendedOrderQty,
        /** HIGH | MEDIUM | LOW — how certain the model is about the forecast. */
        String confidence,
        /** Trend signals, e.g. ["upward_trend", "seasonal_spike_detected"]. */
        List<String> signals,
        /** Plain-English reasoning for the procurement team. */
        String reasoning,
        /** Product ID for traceability. */
        String productId,
        /** Number of days forecasted. */
        int forecastDays
    ) {}
}
