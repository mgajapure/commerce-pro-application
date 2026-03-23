package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.SeoOptimizerService;
import com.commerce_pro_backend.ai.service.SeoOptimizerService.SeoResult;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiSeoController — AI-powered SEO metadata generation for product pages.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/seo}
 *
 * <h3>Two modes</h3>
 * <ul>
 *   <li>{@code /optimise} — generates and saves seoTitle + seoDescription immediately.</li>
 *   <li>{@code /preview}  — generates without saving; for copy review before publish.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ai/seo")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiSeoController {

    private final SeoOptimizerService seoService;

    /**
     * POST /api/v1/ai/seo/products/{productId}/optimise
     *
     * <p>Generates and immediately persists SEO metadata.
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "seoTitle": "Wireless Noise-Cancelling Headphones | SoundWave Pro – Free Shipping",
     *     "seoDescription": "Shop SoundWave Pro wireless headphones with 30-hour battery...",
     *     "keywords": ["wireless headphones", "noise cancelling headphones", ...],
     *     "reasoning": "Led with primary keyword; description includes key specs and CTA."
     *   }
     * }
     * </pre>
     */
    @PostMapping("/products/{productId}/optimise")
    public ResponseEntity<ApiResponse<SeoResult>> optimiseAndSave(
            @PathVariable String productId) {

        SeoResult result = seoService.optimiseAndSave(productId);
        return ResponseEntity.ok(ApiResponse.success("SEO metadata generated and saved", result));
    }

    /**
     * POST /api/v1/ai/seo/products/{productId}/preview
     *
     * <p>Generates a SEO preview without saving — for admin review before publishing.
     */
    @PostMapping("/products/{productId}/preview")
    public ResponseEntity<ApiResponse<SeoResult>> preview(
            @PathVariable String productId) {

        SeoResult result = seoService.preview(productId);
        return ResponseEntity.ok(ApiResponse.success("SEO preview generated (not saved)", result));
    }
}
