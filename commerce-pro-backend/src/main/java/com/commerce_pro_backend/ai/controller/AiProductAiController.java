package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.service.ProductDescriptionService;
import com.commerce_pro_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AiProductAiController — REST interface for AI-generated product descriptions.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/products}
 *
 * <h3>Two modes of operation</h3>
 * <ul>
 *   <li><b>Generate &amp; Save</b> — POST /{productId}/description/generate
 *       Generates a description and immediately persists it to the product record.
 *       Use this when the admin is happy to auto-publish the AI copy.</li>
 *   <li><b>Preview only</b> — POST /{productId}/description/preview
 *       Generates a description but does NOT save it.  Use this when the admin
 *       wants to review and edit the copy before publishing.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ai/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiProductAiController {

    private final ProductDescriptionService productDescService;

    /**
     * POST /api/v1/ai/products/{productId}/description/generate
     *
     * <p>Generates and persists an AI product description.
     * Both {@code product.description} and {@code product.shortDescription}
     * are updated in the same transaction.
     *
     * <h3>Example request</h3>
     * <pre>
     *   POST /api/v1/ai/products/prod-abc-uuid/description/generate
     *   Authorization: Bearer &lt;admin-jwt&gt;
     *   (no body required)
     * </pre>
     *
     * <h3>Example success response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Description generated and saved",
     *   "data": "Experience pure, uninterrupted audio with the SoundWave Pro...
     *            Bluetooth 5.3 pairs instantly with all your devices...
     *            Add to cart and enjoy free shipping today."
     * }
     * </pre>
     *
     * @param productId  UUID of the Product to generate a description for
     */
    @PostMapping("/{productId}/description/generate")
    public ResponseEntity<ApiResponse<String>> generateAndSave(
            @PathVariable String productId) {

        String description = productDescService.generateAndSave(productId);
        return ResponseEntity.ok(ApiResponse.success("Description generated and saved", description));
    }

    /**
     * POST /api/v1/ai/products/{productId}/description/preview
     *
     * <p>Generates a description preview WITHOUT saving it to the database.
     * The admin can call this multiple times to get different variants and
     * then pick the best one before calling the /generate endpoint.
     *
     * <h3>Example request</h3>
     * <pre>
     *   POST /api/v1/ai/products/prod-abc-uuid/description/preview
     *   Authorization: Bearer &lt;admin-jwt&gt;
     * </pre>
     *
     * <h3>Example success response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Description preview generated (not saved)",
     *   "data": "Discover the next generation of wireless audio..."
     * }
     * </pre>
     *
     * @param productId  UUID of the Product
     */
    @PostMapping("/{productId}/description/preview")
    public ResponseEntity<ApiResponse<String>> preview(
            @PathVariable String productId) {

        String preview = productDescService.generatePreview(productId);
        return ResponseEntity.ok(ApiResponse.success("Description preview generated (not saved)", preview));
    }
}
