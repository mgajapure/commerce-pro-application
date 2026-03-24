package com.commerce_pro_backend.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * CostCalculator — computes the exact USD cost of a Groq API call from token counts.
 *
 * Pricing is per 1M tokens in USD. Groq does not support prompt caching,
 * so only input and output token costs are tracked.
 *
 * Update {@link #PRICING} whenever Groq changes their published rates:
 * https://console.groq.com/docs/pricing
 */
public class CostCalculator {

    /**
     * Pricing per 1M tokens in USD.
     * Key = Groq model ID string.
     */
    private static final Map<String, ModelPricing> PRICING = Map.of(
        "llama-3.1-8b-instant",          new ModelPricing(0.05,  0.08),
        "llama-3.3-70b-versatile",        new ModelPricing(0.59,  0.79),
        "mixtral-8x7b-32768",             new ModelPricing(0.24,  0.24),
        "gemma2-9b-it",                   new ModelPricing(0.20,  0.20),
        "deepseek-r1-distill-llama-70b",  new ModelPricing(0.75,  0.99)
    );

    /** Fallback when the model ID is not in the pricing table. */
    private static final ModelPricing DEFAULT_PRICING = new ModelPricing(0.59, 0.79);

    /**
     * Calculate total cost for a single API call.
     *
     * @param model        Groq model ID (e.g. "llama-3.3-70b-versatile")
     * @param inputTokens  number of input tokens consumed
     * @param outputTokens number of output tokens generated
     * @return total cost in USD, rounded to 8 decimal places
     */
    public static BigDecimal calculate(String model, int inputTokens, int outputTokens) {
        ModelPricing pricing = PRICING.getOrDefault(model, DEFAULT_PRICING);

        BigDecimal inputCost  = costForTokens(inputTokens,  pricing.inputPricePer1M());
        BigDecimal outputCost = costForTokens(outputTokens, pricing.outputPricePer1M());

        return inputCost.add(outputCost).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Split cost into input and output components (used for detailed logging).
     */
    public static CostBreakdown breakdown(String model, int inputTokens, int outputTokens) {
        ModelPricing pricing = PRICING.getOrDefault(model, DEFAULT_PRICING);

        BigDecimal inputCost  = costForTokens(inputTokens,  pricing.inputPricePer1M());
        BigDecimal outputCost = costForTokens(outputTokens, pricing.outputPricePer1M());

        return new CostBreakdown(inputCost, outputCost, inputCost.add(outputCost));
    }

    private static BigDecimal costForTokens(int tokens, double pricePer1M) {
        return BigDecimal.valueOf(tokens)
                .multiply(BigDecimal.valueOf(pricePer1M))
                .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
    }

    record ModelPricing(double inputPricePer1M, double outputPricePer1M) {}

    public record CostBreakdown(BigDecimal inputCostUsd, BigDecimal outputCostUsd, BigDecimal totalCostUsd) {}
}
