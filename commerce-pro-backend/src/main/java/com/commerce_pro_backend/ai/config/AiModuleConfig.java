package com.commerce_pro_backend.ai.config;

import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

/**
 * AiModuleConfig — binds all ai.* custom properties.
 *
 * Spring AI auto-configures the ChatClient bean from spring.ai.openai.* properties.
 * This class holds our application-level settings: budgets, rate limits, session TTLs,
 * model aliases, retry policy, and scheduled job toggles.
 *
 * All values are overridable per environment via environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiModuleConfig {

    /**
     * Model name aliases.
     * Keys: fast, balanced, powerful — mapped to Groq model IDs.
     * Used when seeding default AiConfig records.
     */
    private Map<String, String> models;

    /** Hard cap on max tokens for any single API call (safety guard). */
    private int maxTokensCap = 4096;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    private BudgetConfig budget = new BudgetConfig();
    private TimeoutConfig timeout = new TimeoutConfig();
    private RetryConfig retry = new RetryConfig();
    private SessionConfig session = new SessionConfig();
    private JobsConfig jobs = new JobsConfig();

    @Data
    public static class BudgetConfig {
        /** Global daily spend cap in USD across all features. */
        private BigDecimal dailyTotalUsd = new BigDecimal("50.00");
    }

    @Data
    public static class TimeoutConfig {
        private long connectMs = 5000;
        private long readMs = 30000;
    }

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long backoffMs = 1000;
    }

    @Data
    public static class SessionConfig {
        /** How long a support chatbot session stays alive (hours). */
        private int chatbotTtlHours = 24;
        /** How long a NL report session stays alive (hours). */
        private int reportTtlHours = 2;
    }

    @Data
    public static class JobsConfig {
        private boolean nightlyForecastEnabled = true;
        private boolean weeklyChurnEnabled = true;
        private boolean weeklyInventoryEnabled = true;
        private boolean monthlyBudgetEnabled = true;
        private boolean monthlyVendorEnabled = true;
    }
}
