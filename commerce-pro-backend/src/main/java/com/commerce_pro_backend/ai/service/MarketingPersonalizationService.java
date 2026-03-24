package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.repository.CustomerRepository;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MarketingPersonalizationService — uses AI to craft a personalised marketing
 * offer and message tailored to an individual customer's behaviour.
 *
 * <h3>What "personalised" means here</h3>
 * Rather than sending the same 10%-off coupon to everyone, the AI analyses
 * each customer's purchase history, tier, and engagement signals to recommend
 * the specific type of offer and the message angle most likely to convert.
 *
 * <h3>Example output scenarios</h3>
 * <ul>
 *   <li>High-value GOLD customer who hasn't bought in 30 days →
 *       offer: LOYALTY_REWARD (500 bonus points), angle: "exclusive member perk"</li>
 *   <li>SILVER customer who bought 3 accessories in one week →
 *       offer: CROSS_SELL (15% off compatible products), angle: "complete your setup"</li>
 *   <li>New customer (1 order, 7 days ago) →
 *       offer: WELCOME_DISCOUNT (10% off next order), angle: "welcome back"</li>
 * </ul>
 *
 * <h3>Example model response → {@link PersonalizationResult}</h3>
 * <pre>
 * {
 *   "offerType":      "CROSS_SELL",
 *   "discountPct":     15,
 *   "message":        "Hi Sarah — love that you're building out your home office setup!
 *                      Enjoy 15% off compatible accessories for the next 48 hours.",
 *   "subject":        "Complete your home office, Sarah — 15% off accessories inside",
 *   "segmentReason":  "Customer purchased a desk and monitor stand in the past 7 days.
 *                      High cross-sell affinity for accessories in this category.",
 *   "urgencyHours":   48
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketingPersonalizationService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final CustomerRepository  customerRepo;
    private final OrderRepository     orderRepo;

    private static final AiFeatureType FEATURE = AiFeatureType.MARKETING_PERSONALIZATION;

    /**
     * System prompt for personalised offer generation.
     *
     * <p>Key instructions:
     * - First-name personalisation in the message increases open rates.
     * - urgencyHours creates scarcity without feeling manipulative.
     * - offerType from the enumerated list keeps recommendations actionable
     *   (marketing tool can map these to existing campaign templates).
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert e-commerce marketing strategist specialising in
            personalised customer engagement.
            Craft a targeted marketing offer for the customer based on their behaviour.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "offerType":     <"DISCOUNT" | "LOYALTY_REWARD" | "CROSS_SELL" | "UPSELL" | "WIN_BACK" | "WELCOME_BACK">,
              "discountPct":   <integer 0-30 — discount percentage; 0 if offer is loyalty points>,
              "message":       <personalised 1-2 sentence email body using customer's first name>,
              "subject":       <personalised email subject line, max 60 chars>,
              "segmentReason": <why this offer type was chosen, 1-2 sentences>,
              "urgencyHours":  <integer — offer validity in hours, e.g. 48>
            }
            """;

    /**
     * Generate a personalised marketing offer for a customer.
     *
     * @param customerId  UUID of the customer
     * @return populated {@link PersonalizationResult}
     *
     * <p>Example:
     * <pre>
     *   PersonalizationResult r = marketingService.personalise("cust-uuid");
     *   // r.offerType()    → "CROSS_SELL"
     *   // r.discountPct()  → 15
     *   // r.subject()      → "Complete your setup, Sarah — 15% off accessories"
     * </pre>
     */
    @Transactional
    public PersonalizationResult personalise(String customerId) {
        log.info("Marketing personalisation requested for customer: {}", customerId);

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        // Load last 5 orders to understand recent buying behaviour
        List<Order> recentOrders = orderRepo.findByCustomerId(customerId)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

        // Summarise the product categories purchased recently
        // Example: "Wireless Headphones × 1, Phone Case × 2, USB-C Cable × 3"
        String recentProductsSummary = recentOrders.stream()
                .flatMap(o -> o.getItems() != null ? o.getItems().stream() : java.util.stream.Stream.empty())
                .map(item -> nvl(item.getProductName()) + " ×" + nvlInt(item.getQuantity()))
                .distinct()
                .limit(10)
                .collect(Collectors.joining(", "));

        String userMessage = """
                {
                  "customerId":          "%s",
                  "firstName":           "%s",
                  "tier":                "%s",
                  "totalOrders":          %d,
                  "lifetimeSpend":        %s,
                  "averageOrderValue":    %s,
                  "daysSinceLastOrder":   %d,
                  "loyaltyPoints":        %d,
                  "marketingOptIn":       %b,
                  "recentProducts":      "%s",
                  "isBlacklisted":        %b
                }
                """.formatted(
                customer.getId(),
                nvl(customer.getFirstName()),
                nvl(customer.getTier()),
                nvlInt(customer.getTotalOrders()),
                nvlDec(customer.getLifetimeSpend()),
                nvlDec(customer.getAverageOrderValue()),
                computeDaysSince(customer),
                nvlInt(customer.getLoyaltyPoints()),
                Boolean.TRUE.equals(customer.getMarketingOptIn()),
                recentProductsSummary,
                Boolean.TRUE.equals(customer.getIsBlacklisted()));

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        PersonalizationResult result = PromptJsonExtractor.parse(
                rawResponse, PersonalizationResult.class, FEATURE.name());

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(customerId)
                .entityType("CUSTOMER")
                .recommendation(result.offerType())
                .reasoning(result.segmentReason())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());

        return result;
    }

    private long computeDaysSince(Customer c) {
        if (c.getLastOrderAt() == null) return 999;
        return java.time.temporal.ChronoUnit.DAYS.between(c.getLastOrderAt(), java.time.LocalDateTime.now());
    }

    private String nvl(Object o)                     { return o != null ? o.toString() : ""; }
    private int    nvlInt(Integer i)                  { return i != null ? i : 0; }
    private String nvlDec(java.math.BigDecimal d)     { return d != null ? d.toPlainString() : "0.00"; }

    /** PersonalizationResult — the model's personalised marketing offer. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonalizationResult(
        /** DISCOUNT | LOYALTY_REWARD | CROSS_SELL | UPSELL | WIN_BACK | WELCOME_BACK */
        String offerType,
        /** Discount percentage (0 if offer is loyalty points). */
        int discountPct,
        /** Personalised email body (1–2 sentences using customer's first name). */
        String message,
        /** Personalised email subject line (≤60 chars). */
        String subject,
        /** Why this offer type was chosen (for the marketing team). */
        String segmentReason,
        /** How many hours the offer is valid for (e.g. 48). */
        int urgencyHours
    ) {}
}
