package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.dto.request.AiConfigUpdateRequest;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.catalog.product.entity.Product;
import com.commerce_pro_backend.catalog.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ProductDescriptionService — uses AI to generate compelling e-commerce product descriptions.
 *
 * <h3>Use cases</h3>
 * <ul>
 *   <li><b>New product onboarding:</b> generate an initial description when a product
 *       is created with no description yet.</li>
 *   <li><b>SEO refresh:</b> rewrite an existing description using more keyword-rich,
 *       engaging language without changing the facts.</li>
 *   <li><b>Tone variants:</b> generate multiple variants for A/B testing
 *       (professional vs. casual, short vs. long).</li>
 * </ul>
 *
 * <h3>What makes a good AI-generated product description?</h3>
 * The system prompt instructs the model to:
 * <ul>
 *   <li>Lead with the biggest customer benefit, not a list of specs.</li>
 *   <li>Use sensory language ("buttery-smooth", "whisper-quiet") where appropriate.</li>
 *   <li>Naturally embed the category and key attributes as keywords (SEO).</li>
 *   <li>Keep length to 2–3 paragraphs for readability.</li>
 *   <li>End with a soft call-to-action.</li>
 * </ul>
 *
 * <h3>Example prompt payload</h3>
 * <pre>
 * Product name:  Wireless Noise-Cancelling Headphones Pro
 * Brand:         SoundWave
 * Category:      Electronics / Audio
 * Price:         $199.99
 * Tags:          wireless, noise-cancelling, bluetooth, over-ear
 * Key attributes:
 *   - Battery life: 30 hours
 *   - Driver size: 40mm
 *   - Connectivity: Bluetooth 5.3
 * Current description: (empty — new product)
 * </pre>
 *
 * <h3>Example model output (plain text, not JSON)</h3>
 * <pre>
 * Experience pure, uninterrupted audio with the SoundWave Wireless Noise-Cancelling
 * Headphones Pro.  Powered by advanced 40mm drivers and active noise cancellation,
 * every note lands exactly as the artist intended — whether you're on a crowded commute
 * or deep in a work session.
 *
 * Bluetooth 5.3 pairs instantly with all your devices, and an industry-leading 30-hour
 * battery means the music plays as long as you do.  Lightweight over-ear cushions
 * provide all-day comfort without the fatigue of cheaper alternatives.
 *
 * Ready to hear the difference?  Add them to your cart and enjoy free shipping today.
 * </pre>
 *
 * <p>Note: This service uses {@code orchestrator.complete()} (not {@code completeAndPersistInsight()})
 * because product description generation is creative output, not a scored decision.
 * No {@code AiInsight} row is saved for this feature.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductDescriptionService {

    private final AiOrchestrator     orchestrator;
    private final ProductRepository  productRepo;

    /** Feature constant — controls model, budget, and rate limit for description generation. */
    private static final AiFeatureType FEATURE = AiFeatureType.PRODUCT_DESCRIPTION;

    /**
     * System prompt for product description generation.
     *
     * <p>Design notes:
     * - "Return ONLY the description text" avoids markdown headers or preamble.
     * - Tone guidelines match a mid-range e-commerce brand (not luxury, not budget).
     * - Keyword embedding instruction helps SEO without keyword stuffing.
     * - The model is told the output will be displayed directly on a product page,
     *   which encourages appropriate length and format.
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert e-commerce copywriter.
            Write a compelling product description that will appear on a product page.

            Guidelines:
            - Lead with the primary customer benefit, not a list of specifications.
            - Use vivid, sensory language where it fits naturally.
            - Embed product name, category, and key attributes naturally for SEO.
            - Write 2–3 focused paragraphs (no headers, no bullet points).
            - End with a soft call-to-action (e.g. "Add to cart today.").
            - Tone: friendly, confident, mid-market appeal.

            Return ONLY the description text — no introduction, no markdown, no explanation.
            """;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generate a product description and immediately save it back to the product record.
     *
     * <p>After the AI generates the text:
     * <ul>
     *   <li>{@code product.description} is updated with the new long description.
     *   <li>{@code product.shortDescription} is updated with the first sentence only
     *       (derived client-side here, not by the AI).
     * </ul>
     *
     * @param productId  UUID of the {@link Product} to generate a description for
     * @return the generated description text
     * @throws EntityNotFoundException if the product does not exist
     *
     * <p>Example:
     * <pre>
     *   String description = productDescService.generateAndSave("prod-uuid");
     *   // description → "Experience pure, uninterrupted audio with the SoundWave..."
     *   // Product.description is now updated in the database
     * </pre>
     */
    @Transactional
    public String generateAndSave(String productId) {
        log.info("Generating AI description for product: {}", productId);

        // 1. Load the product
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        // 2. Build the prompt payload from the product's metadata
        String userMessage = buildDescriptionPayload(product);

        // 3. Call the AI model (single-shot — no conversation, no insight row)
        String description = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);

        // 4. Clean up any leading/trailing whitespace from the model output
        description = description.strip();

        // 5. Save the generated description back to the product
        product.setDescription(description);

        // 6. Also set shortDescription to the first sentence (truncated to 200 chars)
        //    The short description is used on category listing pages and search results.
        product.setShortDescription(extractFirstSentence(description, 200));

        productRepo.save(product);

        log.info("Description generated and saved for product: {}", productId);
        return description;
    }

    /**
     * Generate a description WITHOUT saving it to the database.
     *
     * <p>Useful when the admin wants to preview the AI-generated copy before
     * deciding whether to publish it.
     *
     * @param productId  UUID of the product
     * @return generated description text (not persisted)
     *
     * <p>Example:
     * <pre>
     *   // Admin previews 3 variants before choosing one
     *   String preview1 = productDescService.generatePreview("prod-uuid");
     *   String preview2 = productDescService.generatePreview("prod-uuid"); // different each call
     * </pre>
     */
    public String generatePreview(String productId) {
        log.info("Generating AI description preview for product: {}", productId);

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        return orchestrator.complete(FEATURE, SYSTEM_PROMPT, buildDescriptionPayload(product)).strip();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build the user message containing all product metadata the model needs.
     *
     * <p>We send structured text (not JSON) here because the output is creative prose,
     * not a parsed JSON object.  A plain-text format reads more naturally as a
     * "creative brief" for the copywriter model.
     *
     * <p>Example output:
     * <pre>
     * Product name:  Wireless Noise-Cancelling Headphones Pro
     * Brand:         SoundWave
     * Category:      Electronics > Audio
     * Price:         $199.99
     * Tags:          wireless, noise-cancelling, bluetooth, over-ear
     * Stock status:  IN_STOCK
     * Key attributes:
     *   - Battery life: 30 hours
     *   - Connectivity: Bluetooth 5.3
     * Current description: (empty — please generate from scratch)
     * </pre>
     */
    private String buildDescriptionPayload(Product product) {
        // Format the tags list as a comma-separated string for readability
        // Example: ["wireless", "bluetooth"] → "wireless, bluetooth"
        String tagsLine = product.getTags() != null && !product.getTags().isEmpty()
                ? String.join(", ", product.getTags())
                : "(none)";

        // Format each attribute as "  - Name: Value" for clarity
        // Example: attribute "Color: Red" → "  - Color: Red"
        String attributesBlock = buildAttributesBlock(product);

        // Include the current description so the model can improve it if it exists,
        // or generate from scratch if it is blank.
        String currentDesc = isBlank(product.getDescription())
                ? "(empty — please generate from scratch)"
                : product.getDescription();

        return """
                Product name:  %s
                Brand:         %s
                Category:      %s
                Price:         $%s
                Tags:          %s
                Stock status:  %s
                %s
                Current description: %s
                """.formatted(
                nvl(product.getName()),
                nvl(product.getBrand()),
                nvl(product.getCategory()),
                nvl(product.getPrice()),
                tagsLine,
                nvl(product.getStockStatus()),
                attributesBlock,
                currentDesc
        );
    }

    /**
     * Format the product's attribute list as a bullet block for the prompt.
     *
     * <p>Example product attributes:
     * <pre>
     *   attributes: [ProductAttribute(name="Battery life", value="30 hours"),
     *                ProductAttribute(name="Connectivity", value="Bluetooth 5.3")]
     *
     *   output:
     *   Key attributes:
     *     - Battery life: 30 hours
     *     - Connectivity: Bluetooth 5.3
     * </pre>
     */
    private String buildAttributesBlock(Product product) {
        if (product.getAttributes() == null || product.getAttributes().isEmpty()) {
            return "Key attributes: (none)";
        }
        var sb = new StringBuilder("Key attributes:\n");
        product.getAttributes().forEach(attr ->
                sb.append("  - ").append(attr.getName()).append(": ").append(attr.getValue()).append("\n"));
        return sb.toString().stripTrailing();
    }

    /**
     * Extract the first sentence (or first {@code maxLen} characters) from a paragraph.
     *
     * <p>Used to auto-populate {@code product.shortDescription}.
     * Example: "Experience pure audio. Powered by 40mm drivers." → "Experience pure audio."
     */
    private String extractFirstSentence(String text, int maxLen) {
        if (text == null || text.isBlank()) return "";
        int dot = text.indexOf('.');
        String first = dot > 0 ? text.substring(0, dot + 1) : text;
        return first.length() > maxLen ? first.substring(0, maxLen) : first;
    }

    private String nvl(Object o)    { return o != null ? o.toString() : ""; }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
