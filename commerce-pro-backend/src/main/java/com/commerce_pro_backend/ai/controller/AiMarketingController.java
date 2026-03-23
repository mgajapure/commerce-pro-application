package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.MarketingPersonalizationService;
import com.commerce_pro_backend.ai.service.MarketingPersonalizationService.PersonalizationResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiMarketingController — AI-powered personalised marketing offer generation.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/marketing}
 *
 * <h3>Typical integration</h3>
 * The CRM bulk-action panel calls this endpoint for each selected customer
 * to generate a targeted offer before launching a campaign.
 */
@RestController
@RequestMapping("/api/v1/ai/marketing")
@RequiredArgsConstructor
public class AiMarketingController {

    private final MarketingPersonalizationService marketingService;

    /**
     * POST /api/v1/ai/marketing/customers/{customerId}/personalise
     *
     * <p>Generates a personalised offer and email copy for the customer.
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "offerType": "CROSS_SELL", "discountPct": 15,
     *     "message": "Hi Sarah — complete your home office setup with 15% off accessories!",
     *     "subject": "Complete your setup, Sarah — 15% off accessories",
     *     "segmentReason": "Customer bought a desk and monitor stand last week.",
     *     "urgencyHours": 48
     *   }
     * }
     * </pre>
     *
     * @param customerId  UUID of the customer
     */
    @PostMapping("/customers/{customerId}/personalise")
    @PreAuthorize("hasAuthority('ai:marketing:personalise')")
    public ResponseEntity<ApiResponse<PersonalizationResult>> personalise(
            @PathVariable String customerId) {

        PersonalizationResult result = marketingService.personalise(customerId);
        return ResponseEntity.ok(ApiResponse.success("Personalisation complete", result));
    }
}
