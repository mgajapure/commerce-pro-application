# Commerce Pro — AI Module Documentation

> **Tech Stack:** Spring Boot 3.5.11 · Java 17 · Anthropic Java SDK 2.16.1 · JPA/Hibernate · H2 (dev) / MySQL/PostgreSQL (prod)

---

## Overview

Commerce Pro integrates Claude (Anthropic) as its AI engine across all business domains — payments, inventory, customer management, analytics, catalog, and logistics. This directory is the single source of truth for every AI feature, its memory strategy, the database schema it writes to, and the infrastructure that governs cost and safety.

---

## Document Index

| File | What it covers |
|------|----------------|
| [`01-features-catalog.md`](./01-features-catalog.md) | All 15 AI features — purpose, inputs, outputs, API shape, and which entities they read/write |
| [`02-memory-management.md`](./02-memory-management.md) | In-context, session, long-term, and cached memory — code-level patterns for every feature |
| [`03-entities-schema.md`](./03-entities-schema.md) | New JPA entities: `AiInsight`, `AiConversation`, `AiUsageLog`, `AiConfig` — full field definitions |
| [`04-infrastructure.md`](./04-infrastructure.md) | `AiOrchestrator`, `AiMemoryManager`, `AiCostGuard`, `AiRateLimiter`, prompt-cache config, `application.properties` |
| [`05-implementation-guide.md`](./05-implementation-guide.md) | Phased rollout plan, dependency order, testing strategy, go-live checklist |

---

## Feature Summary (15 Features)

| # | Feature | Domain | Memory Type | Model |
|---|---------|--------|-------------|-------|
| 1 | **Fraud Detection** | Payment | In-context + DB | claude-haiku-4-5 |
| 2 | **Demand Forecasting** | Inventory | In-context + DB | claude-sonnet-4-6 |
| 3 | **Customer Churn Prediction** | Customer | In-context + DB | claude-sonnet-4-6 |
| 4 | **Natural Language Reports** | Analytics | Conversation (session) | claude-sonnet-4-6 |
| 5 | **Customer Support Chatbot** | Customer | Conversation (per-customer) | claude-sonnet-4-6 |
| 6 | **Product Description Generator** | Catalog | In-context, stateless | claude-haiku-4-5 |
| 7 | **Review Sentiment Analysis** | Catalog | In-context + DB | claude-haiku-4-5 |
| 8 | **Smart Pricing Recommendations** | Catalog/Inventory | In-context + DB | claude-sonnet-4-6 |
| 9 | **Inventory Optimization** | Inventory | In-context + DB | claude-sonnet-4-6 |
| 10 | **Budget Anomaly Detection** | Finance | In-context + DB | claude-sonnet-4-6 |
| 11 | **SEO Content Optimizer** | Catalog/SEO | In-context, stateless | claude-haiku-4-5 |
| 12 | **Marketing Personalization** | Customer | In-context + DB | claude-sonnet-4-6 |
| 13 | **Return & Refund Pattern Analysis** | Payment/Order | In-context + DB | claude-sonnet-4-6 |
| 14 | **Vendor Performance Analysis** | Finance | In-context + DB | claude-sonnet-4-6 |
| 15 | **Shipping Route Optimization** | Fulfillment | In-context + DB | claude-haiku-4-5 |

---

## Module Package Structure

```
com.commerce_pro_backend.ai/
├── config/
│   └── AiModuleConfig.java              # API key, model defaults, feature flags
├── entity/
│   ├── AiInsight.java                   # Long-term AI decision store
│   ├── AiConversation.java              # Session/chat memory store
│   └── AiUsageLog.java                  # Token usage + cost tracking
├── enums/
│   ├── AiFeatureType.java               # FRAUD_DETECTION, DEMAND_FORECAST, ...
│   ├── AiModelTier.java                 # HAIKU, SONNET, OPUS
│   └── InsightStatus.java               # PENDING, PROCESSED, EXPIRED
├── repository/
│   ├── AiInsightRepository.java
│   ├── AiConversationRepository.java
│   └── AiUsageLogRepository.java
├── service/
│   ├── AiOrchestrator.java              # Builds requests, calls SDK, routes responses
│   ├── AiMemoryManager.java             # Manages context window budget
│   ├── AiCostGuard.java                 # Budget enforcement, daily spend limits
│   └── AiRateLimiter.java               # Per-feature request throttling
└── features/
    ├── fraud/FraudDetectionService.java
    ├── forecast/AiForecastService.java
    ├── churn/ChurnPredictionService.java
    ├── reports/NlReportService.java
    ├── chatbot/SupportChatbotService.java
    ├── catalog/ProductDescriptionService.java
    ├── catalog/SentimentAnalysisService.java
    ├── pricing/PricingRecommendationService.java
    ├── inventory/InventoryOptimizationService.java
    ├── finance/BudgetAnomalyService.java
    ├── seo/SeoOptimizerService.java
    ├── marketing/MarketingPersonalizationService.java
    ├── returns/ReturnPatternService.java
    ├── vendor/VendorAnalysisService.java
    └── shipping/ShippingOptimizationService.java
```

---

## Quick Start

### 1. Set API Key
```properties
# application.properties
anthropic.api-key=${ANTHROPIC_API_KEY}
```

### 2. Spring Bean
```java
// AiModuleConfig.java
@Bean
public AnthropicClient anthropicClient() {
    return AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build();
}
```

### 3. Call a Feature
```java
// Example: Fraud Detection
FraudResult result = fraudDetectionService.analyze(transactionId);
// Result is automatically persisted to ai_insights table
```

---

## Anthropic SDK Reference

```gradle
// build.gradle
implementation 'com.anthropic:anthropic-java:2.16.1'
```

```java
// Core SDK classes used
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
```

---

## Cost Model

| Model | Input (per 1M tokens) | Output (per 1M tokens) | Use case |
|-------|-----------------------|------------------------|----------|
| claude-haiku-4-5 | $0.80 | $4.00 | High-volume, low-latency |
| claude-sonnet-4-6 | $3.00 | $15.00 | Complex reasoning |
| claude-opus-4-6 | $15.00 | $75.00 | Reserved for critical decisions |
| Prompt Cache Write | +25% on input | — | One-time system prompt cache |
| Prompt Cache Hit | -90% on input | — | Subsequent requests reuse cache |
