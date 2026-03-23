package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.ai.entity.AiConfig;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds one AiConfig row per AiFeatureType on startup (dev profile only).
 *
 * Model aliases come from application.properties:
 *   ai.models.fast     → lightweight model for high-volume, low-complexity features
 *   ai.models.balanced → general-purpose model for most features
 *   ai.models.powerful → strongest model for complex reasoning tasks
 *
 * Runs at @Order(18) — after all other data seeders.
 * Skips seeding if rows already exist (idempotent).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiConfigDataSeeder {

    private final AiConfigRepository aiConfigRepository;

    @Value("${ai.models.fast}")
    private String fastModel;

    @Value("${ai.models.balanced}")
    private String balancedModel;

    @Value("${ai.models.powerful}")
    private String powerfulModel;

    @Bean
    @Profile("dev")
    @Order(18)
    CommandLineRunner seedAiConfigs() {
        return args -> {
            if (aiConfigRepository.count() > 0) {
                log.info("[AiConfigDataSeeder] AiConfig rows already present — skipping seed.");
                return;
            }

            List<AiConfig> configs = List.of(
                build(AiFeatureType.FRAUD_DETECTION,           fastModel,     512,  5.00, 1500.00, 120, 2000),
                build(AiFeatureType.DEMAND_FORECAST,           balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.CHURN_PREDICTION,          balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.NL_REPORT,                 powerfulModel, 2048, 8.00, 2400.00,  20,  300),
                build(AiFeatureType.SUPPORT_CHATBOT,           fastModel,     1024, 5.00, 1500.00,  60, 1000),
                build(AiFeatureType.PRODUCT_DESCRIPTION,       balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.SENTIMENT_ANALYSIS,        fastModel,      512, 5.00, 1500.00,  60, 1000),
                build(AiFeatureType.PRICING_RECOMMENDATION,    balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.INVENTORY_OPTIMIZATION,    balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.BUDGET_ANOMALY,            balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.SEO_OPTIMIZER,             balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.MARKETING_PERSONALIZATION, balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.RETURN_PATTERN_ANALYSIS,   balancedModel, 1024, 3.00,  900.00,  30,  500),
                build(AiFeatureType.VENDOR_ANALYSIS,           powerfulModel, 2048, 5.00, 1500.00,  20,  300),
                build(AiFeatureType.SHIPPING_OPTIMIZATION,     balancedModel, 1024, 3.00,  900.00,  30,  500)
            );

            aiConfigRepository.saveAll(configs);
            log.info("[AiConfigDataSeeder] Seeded {} AiConfig rows.", configs.size());
        };
    }

    private AiConfig build(AiFeatureType feature,
                           String model,
                           int maxTokens,
                           double dailyBudgetUsd,
                           double monthlyBudgetUsd,
                           int rateLimitPerMinute,
                           int rateLimitPerHour) {
        return AiConfig.builder()
                .featureType(feature)
                .model(model)
                .maxTokens(maxTokens)
                .enabled(true)
                .dailyBudgetUsd(BigDecimal.valueOf(dailyBudgetUsd))
                .monthlyBudgetUsd(BigDecimal.valueOf(monthlyBudgetUsd))
                .rateLimitPerMinute(rateLimitPerMinute)
                .rateLimitPerHour(rateLimitPerHour)
                .promptCacheEnabled(true)
                .fallbackStrategy("SKIP")
                .defaultScore(50)
                .build();
    }
}
