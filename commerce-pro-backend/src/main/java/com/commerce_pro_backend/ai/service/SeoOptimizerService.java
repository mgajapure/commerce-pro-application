package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.catalog.product.entity.Product;
import com.commerce_pro_backend.catalog.product.repository.ProductRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SeoOptimizerService — uses AI to generate optimised SEO metadata (title tag,
 * meta description, and keyword suggestions) for a product page.
 *
 * <h3>SEO metadata anatomy</h3>
 * <ul>
 *   <li><b>seoTitle</b>    — appears in browser tab + Google search headline.
 *       Target: 50–60 characters, lead with primary keyword.</li>
 *   <li><b>seoDescription</b> — appears below the title in Google results.
 *       Target: 150–160 characters, action-oriented, includes a call-to-action.</li>
 *   <li><b>keywords</b>    — 5–10 short-tail and long-tail keywords for meta tags
 *       and internal linking strategy.</li>
 * </ul>
 *
 * <h3>Example model response → {@link SeoResult}</h3>
 * <pre>
 * {
 *   "seoTitle":       "Wireless Noise-Cancelling Headphones | SoundWave Pro – Free Shipping",
 *   "seoDescription": "Shop the SoundWave Pro wireless headphones with 30-hour battery
 *                      and advanced ANC. Bluetooth 5.3, premium comfort. Order today
 *                      and enjoy free shipping.",
 *   "keywords":       ["wireless headphones", "noise cancelling headphones", "bluetooth headphones",
 *                      "over ear headphones", "SoundWave Pro", "30 hour battery headphones"],
 *   "reasoning":      "Led the title with the primary keyword 'wireless noise-cancelling
 *                      headphones' and added the brand for recognition. Description hits
 *                      key specs (battery, BT version) and ends with a CTA."
 * }
 * </pre>
 *
 * <p>After generating the result, the service writes the new seoTitle and seoDescription
 * directly back to the {@link Product} record so no separate PATCH request is needed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeoOptimizerService {

    private final AiOrchestrator    orchestrator;
    private final ProductRepository productRepo;

    private static final AiFeatureType FEATURE = AiFeatureType.SEO_OPTIMIZER;

    /**
     * System prompt for SEO metadata generation.
     *
     * <p>Character-length constraints are explicit so the model doesn't produce
     * metadata that gets truncated in search results.
     * "no markdown" prevents the model from wrapping the title in backticks.
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert SEO copywriter for e-commerce product pages.
            Generate optimised SEO metadata for the product provided.

            Rules:
            - seoTitle:       50–60 characters; lead with the primary keyword; include brand if space allows.
            - seoDescription: 150–160 characters; action-oriented; include 1–2 key specs and a CTA.
            - keywords:       5–8 keywords; mix short-tail (1-2 words) and long-tail (3-5 words).
            - No markdown, no formatting characters in the output strings.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "seoTitle":       <string — optimised title tag>,
              "seoDescription": <string — meta description>,
              "keywords":       [<keyword strings>],
              "reasoning":      <1-2 sentences explaining the keyword strategy>
            }
            """;

    /**
     * Generate and save SEO metadata for a product.
     *
     * <p>Both {@code product.seoTitle} and {@code product.seoDescription} are updated
     * in the same transaction.  The caller gets back the {@link SeoResult} for immediate
     * display in the admin UI before the page cache refreshes.
     *
     * @param productId  UUID of the product
     * @return populated {@link SeoResult}
     *
     * <p>Example:
     * <pre>
     *   SeoResult r = seoService.optimiseAndSave("prod-uuid");
     *   // r.seoTitle()       → "Wireless Noise-Cancelling Headphones | SoundWave Pro"
     *   // r.seoDescription() → "Shop the SoundWave Pro wireless headphones..."
     *   // r.keywords()       → ["wireless headphones", "noise cancelling", ...]
     * </pre>
     */
    @Transactional
    public SeoResult optimiseAndSave(String productId) {
        log.info("SEO optimisation requested for product: {}", productId);

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        String tagsLine = product.getTags() != null ? String.join(", ", product.getTags()) : "(none)";

        String userMessage = """
                Product name:       %s
                Brand:              %s
                Category:           %s
                Short description:  %s
                Key tags:           %s
                Current seoTitle:   %s
                Current seoDesc:    %s
                """.formatted(
                nvl(product.getName()),
                nvl(product.getBrand()),
                nvl(product.getCategory()),
                nvl(product.getShortDescription()),
                tagsLine,
                nvl(product.getSeoTitle()),
                nvl(product.getSeoDescription()));

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        SeoResult result = PromptJsonExtractor.parse(rawResponse, SeoResult.class, FEATURE.name());

        product.setSeoTitle(result.seoTitle());
        product.setSeoDescription(result.seoDescription());
        productRepo.save(product);

        return new SeoResult(result.seoTitle(), result.seoDescription(), result.keywords(), result.reasoning(), productId, true);
    }

    /**
     * Generate a SEO preview WITHOUT saving to the database.
     * Useful when the admin wants to see AI suggestions before approving them.
     *
     * @param productId  UUID of the product
     * @return generated {@link SeoResult} (not persisted)
     */
    public SeoResult preview(String productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        String tagsLine = product.getTags() != null ? String.join(", ", product.getTags()) : "(none)";
        String userMessage = "Product name: %s\nBrand: %s\nCategory: %s\nTags: %s".formatted(
                nvl(product.getName()), nvl(product.getBrand()),
                nvl(product.getCategory()), tagsLine);

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        SeoResult result = PromptJsonExtractor.parse(rawResponse, SeoResult.class, FEATURE.name());
        return new SeoResult(result.seoTitle(), result.seoDescription(), result.keywords(), result.reasoning(), productId, false);
    }

    private String nvl(Object o) { return o != null ? o.toString() : ""; }

    /** SeoResult — the model's SEO metadata output. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeoResult(
        /** Optimised title tag (50–60 chars). Written to product.seoTitle. */
        String seoTitle,
        /** Meta description (150–160 chars). Written to product.seoDescription. */
        String seoDescription,
        /** 5–8 target keywords. */
        List<String> keywords,
        /** Brief explanation of the keyword strategy. */
        String reasoning,
        /** Product ID for traceability. */
        String productId,
        /** Whether the result was saved to the database. */
        boolean saved
    ) {}
}
