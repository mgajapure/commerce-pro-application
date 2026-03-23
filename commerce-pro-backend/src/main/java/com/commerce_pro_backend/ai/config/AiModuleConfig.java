package com.commerce_pro_backend.ai.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * AiModuleConfig — binds all anthropic.* properties and exposes the AnthropicClient bean.
 *
 * Properties are loaded from application.properties (prefix = "anthropic").
 * All values can be overridden per environment via environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "anthropic")
@Data
public class AiModuleConfig {

    /** Anthropic API key. Set via ANTHROPIC_API_KEY environment variable. */
    private String apiKey;

    /** Model name shortcuts: haiku, sonnet, opus. */
    private Map<String, String> models;

    /** Hard cap on max tokens for any single API call (safety guard). */
    private int maxTokensCap = 4096;

    /** Whether to enable prompt caching globally (overridden per-feature by AiConfig). */
    private boolean promptCacheEnabled = true;

    private BudgetConfig budget = new BudgetConfig();
    private TimeoutConfig timeout = new TimeoutConfig();
    private RetryConfig retry = new RetryConfig();
    private SessionConfig session = new SessionConfig();
    private JobsConfig jobs = new JobsConfig();

    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .connectTimeout(Duration.ofMillis(timeout.getConnectMs()))
            .readTimeout(Duration.ofMillis(timeout.getReadMs()))
            .build();
    }

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
