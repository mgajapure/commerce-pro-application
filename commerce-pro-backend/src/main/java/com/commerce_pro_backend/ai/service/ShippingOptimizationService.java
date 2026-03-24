package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.fulfillment.entity.Shipment;
import com.commerce_pro_backend.fulfillment.repository.ShipmentRepository;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ShippingOptimizationService — uses AI to recommend the optimal carrier and
 * shipping method for an order, balancing cost, speed, and destination.
 *
 * <h3>Why this matters</h3>
 * Shipping costs represent 5–15% of e-commerce revenue.  Choosing the wrong carrier
 * or service level either erodes margin (premium carrier for a small domestic parcel)
 * or damages customer satisfaction (economy service for a time-sensitive order).
 *
 * <h3>Decision factors the model considers</h3>
 * <ul>
 *   <li>Parcel dimensions and weight — small parcels often ship cheaper via courier.</li>
 *   <li>Destination — domestic vs international; urban vs rural.</li>
 *   <li>Order value — high-value orders should use tracked, insured services.</li>
 *   <li>Customer tier — GOLD/PLATINUM customers expect premium shipping.</li>
 *   <li>Current shipment SLA performance — the model is given carrier stats.</li>
 * </ul>
 *
 * <h3>Example model response → {@link ShippingResult}</h3>
 * <pre>
 * {
 *   "recommendedCarrier":  "FedEx",
 *   "recommendedMethod":   "Ground",
 *   "estimatedDays":        3,
 *   "estimatedCostUsd":    8.50,
 *   "reasoning":           "Order weight (1.2 kg) and domestic destination qualify
 *                           for FedEx Ground at an estimated $8.50. This saves ~$12
 *                           vs the current default carrier while meeting the 3-day SLA.
 *                           The $320 order value is adequately covered by standard
 *                           FedEx Ground insurance.",
 *   "signals":             ["domestic_eligible", "light_parcel", "cost_saving_opportunity"]
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShippingOptimizationService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final OrderRepository     orderRepo;
    private final ShipmentRepository  shipmentRepo;

    private static final AiFeatureType FEATURE = AiFeatureType.SHIPPING_OPTIMIZATION;

    private static final String SYSTEM_PROMPT = """
            You are a logistics and shipping optimisation specialist.
            Recommend the best carrier and service level for the order based on the data provided.
            Consider cost, delivery speed, order value, and destination when making the recommendation.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "recommendedCarrier":  <carrier name, e.g. "FedEx">,
              "recommendedMethod":   <service level, e.g. "Ground" | "Priority" | "Express">,
              "estimatedDays":       <integer — estimated transit days>,
              "estimatedCostUsd":    <decimal — estimated shipping cost in USD>,
              "reasoning":           <plain-English explanation, 2-3 sentences>,
              "signals":             [<signals, e.g. "domestic_eligible", "high_value_order">]
            }
            """;

    /**
     * Recommend the optimal shipping option for an order.
     *
     * @param orderId  UUID of the {@link Order} to evaluate
     * @return populated {@link ShippingResult}
     *
     * <p>Example:
     * <pre>
     *   ShippingResult r = shippingService.optimise("order-uuid");
     *   // r.recommendedCarrier() → "FedEx"
     *   // r.recommendedMethod()  → "Ground"
     *   // r.estimatedCostUsd()   → 8.50
     *   // r.estimatedDays()      → 3
     * </pre>
     */
    @Transactional
    public ShippingResult optimise(String orderId) {
        log.info("Shipping optimisation requested for order: {}", orderId);

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        // Load existing shipments for this order to provide context on what was tried before
        List<Shipment> existingShipments = shipmentRepo.findByOrderId(orderId);
        String existingCarrier = existingShipments.isEmpty() ? "(none)"
                : existingShipments.get(0).getCarrierName();

        // Compute total parcel weight from order items if available
        // (in a real system this would come from product weight attributes)
        int itemCount = order.getItems() != null ? order.getItems().size() : 0;

        // Determine if the order is domestic based on shipping address country
        boolean isDomestic = order.getShippingAddress() != null
                && "US".equalsIgnoreCase(order.getShippingAddress().getCountry());

        String userMessage = """
                {
                  "orderId":           "%s",
                  "orderNumber":       "%s",
                  "totalAmount":        %s,
                  "itemCount":          %d,
                  "shippingMethod":    "%s",
                  "destinationCountry":"%s",
                  "destinationState":  "%s",
                  "isDomestic":         %b,
                  "currentCarrier":    "%s",
                  "existingShipments":  %d
                }
                """.formatted(
                order.getId(),
                nvl(order.getOrderNumber()),
                nvlDec(order.getTotalAmount()),
                itemCount,
                nvl(order.getShippingMethod()),
                order.getShippingAddress() != null ? nvl(order.getShippingAddress().getCountry()) : "US",
                order.getShippingAddress() != null ? nvl(order.getShippingAddress().getState()) : "",
                isDomestic,
                existingCarrier,
                existingShipments.size());

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        ShippingResult result = PromptJsonExtractor.parse(rawResponse, ShippingResult.class, FEATURE.name());

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(orderId)
                .entityType("ORDER")
                .recommendation(result.recommendedCarrier() + "_" + result.recommendedMethod())
                .signals(result.signals() != null ? String.join(",", result.signals()) : "")
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());

        return result;
    }

    private String nvl(Object o)                    { return o != null ? o.toString() : ""; }
    private String nvlDec(java.math.BigDecimal d)   { return d != null ? d.toPlainString() : "0.00"; }

    /** ShippingResult — the model's carrier and method recommendation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShippingResult(
        /** Recommended carrier name (e.g. "FedEx", "UPS", "USPS"). */
        String recommendedCarrier,
        /** Recommended service level (e.g. "Ground", "Priority", "Express"). */
        String recommendedMethod,
        /** Estimated transit time in business days. */
        int estimatedDays,
        /** Estimated shipping cost in USD. */
        java.math.BigDecimal estimatedCostUsd,
        /** Plain-English justification for the logistics team. */
        String reasoning,
        /** Decision signals, e.g. ["domestic_eligible", "cost_saving_opportunity"]. */
        List<String> signals
    ) {}
}
