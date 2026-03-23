# AI Module — Implementation Guide

Phased rollout plan, dependency order, testing strategy, and go-live checklist.

---

## Phase Overview

```
Phase 1 — Foundation (Week 1-2)
  Core infrastructure: AiOrchestrator, entities, config, cost guard, rate limiter
  No feature logic yet — just the scaffolding all features share

Phase 2 — High-Value Quick Wins (Week 3-4)
  Features: Fraud Detection, Product Description Generator, Sentiment Analysis
  Reason: Fraud is business-critical; description/sentiment are stateless and easy to test

Phase 3 — Batch Intelligence (Week 5-6)
  Features: Demand Forecasting, Churn Prediction, Inventory Optimization
  Reason: Scheduled jobs, no real-time pressure, can be tested without prod traffic

Phase 4 — Conversational AI (Week 7-8)
  Features: Support Chatbot, Natural Language Reports
  Reason: Require conversation memory infra (Phase 1), more complex to test

Phase 5 — Financial & Operational AI (Week 9-10)
  Features: Budget Anomaly, Vendor Analysis, Return Pattern Analysis
  Reason: Require financial data accumulation, run monthly

Phase 6 — Optimisation & Advisory (Week 11-12)
  Features: Pricing Recommendations, SEO Optimizer, Marketing Personalization, Shipping Optimization
  Reason: Advisory (read-only AI output), admin reviews before applying
```

---

## Phase 1: Foundation

### Step 1.1 — New package structure
Create the package `com.commerce_pro_backend.ai` with sub-packages:
```
ai/
  config/
  entity/
  enums/
  repository/
  service/
  features/
  exception/
  dto/
```

### Step 1.2 — Enums
Create:
- `AiFeatureType` (15 values — see `03-entities-schema.md`)
- `SessionType` (SUPPORT_CHAT, NL_REPORT)
- `InsightStatus` (PENDING, PROCESSED, EXPIRED, ERROR)

### Step 1.3 — Entities
Create all four entities from `03-entities-schema.md`:
1. `AiInsight`
2. `AiConversation`
3. `AiUsageLog`
4. `AiConfig`

### Step 1.4 — Repositories
Create all four repositories (from `03-entities-schema.md`).

### Step 1.5 — application.properties
Add all `anthropic.*` properties from `04-infrastructure.md`.
Set `ANTHROPIC_API_KEY` in `.env` or IDE run config (never in source control).

### Step 1.6 — AiModuleConfig bean
Creates the `AnthropicClient` Spring bean. Verify with a unit test:
```java
@SpringBootTest
class AiModuleConfigTest {
    @Autowired AnthropicClient client;

    @Test
    void clientBeanIsCreated() {
        assertNotNull(client);
    }
}
```

### Step 1.7 — Core services
Implement in this order (each depends on the previous):
1. `CostCalculator` (pure utility, no Spring)
2. `AiCostGuard`
3. `AiRateLimiter`
4. `AiMemoryManager`
5. `AiOrchestrator`

### Step 1.8 — Seed AiConfig data
Add a `DataInitializer` bean that inserts one `AiConfig` row per `AiFeatureType` if none exist:
```java
@Component
@RequiredArgsConstructor
public class AiConfigInitializer implements ApplicationRunner {
    private final AiConfigRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        for (AiFeatureType type : AiFeatureType.values()) {
            if (repo.findByFeatureType(type).isEmpty()) {
                repo.save(defaultConfig(type));
            }
        }
    }
}
```

### Step 1.9 — Admin controller
Implement `AiAdminController` (from `04-infrastructure.md`).

### Step 1.10 — Foundation smoke test
```java
// Verify the full stack: config -> orchestrator -> Anthropic API
@Test
void orchestratorCanCallApi() {
    MessageCreateParams params = MessageCreateParams.builder()
        .model("claude-haiku-4-5-20251001")
        .maxTokens(50)
        .addUserMessage("Reply with the single word: OK")
        .build();

    Message response = orchestrator.execute(AiFeatureType.FRAUD_DETECTION, params);
    assertThat(extractText(response)).contains("OK");
}
```

---

## Phase 2: Quick-Win Features

### Feature 2.1 — Fraud Detection
**Dependencies:** PaymentTransaction, Customer repositories (already exist)

Implementation steps:
1. Create `FraudResult` record (riskScore, riskLevel, reasons, recommendation, confidence)
2. Create `AiSystemPrompts.FRAUD_DETECTION` constant (see `01-features-catalog.md`)
3. Implement `FraudDetectionService.analyze(String transactionId)`
4. Create `PaymentTransactionCreatedEvent` and publish from `PaymentService`
5. Add `@EventListener` in `FraudDetectionService`
6. Create `FraudController` (`POST /api/v1/ai/fraud/score/{transactionId}`)
7. Write unit test with mock API response
8. Write integration test with H2 database

**Test case (unit):**
```java
@Test
void highRiskTransactionSetsIsFlaggedTrue() {
    // Given: mock Claude returns { "riskScore": 90, "riskLevel": "CRITICAL", ... }
    // When: fraudService.analyze(transactionId)
    // Then: transaction.isFlagged == true, transaction.riskScore == 90
}
```

### Feature 2.2 — Product Description Generator
**Dependencies:** Product, ProductAttribute, SeoMetadata repositories (already exist)

Implementation steps:
1. Create `ProductDescriptionRequest` DTO (productId, tone enum)
2. Implement `ProductDescriptionService.generate(String productId, Tone tone)`
3. Create `CatalogAiController` (`POST /api/v1/ai/catalog/describe/{productId}`)
4. Test with a real product from seed data

### Feature 2.3 — Sentiment Analysis
**Dependencies:** Review repository

Implementation steps:
1. Create `SentimentResult` record
2. Implement `SentimentAnalysisService.analyze(String reviewId)`
3. Publish `ReviewCreatedEvent` from `ReviewService`
4. Add `@EventListener` in `SentimentAnalysisService`
5. Add endpoint to `CatalogAiController`

---

## Phase 3: Batch Intelligence

### Feature 3.1 — Demand Forecasting
**Dependencies:** DemandForecast entity already exists and maps to a real table.

Implementation steps:
1. Implement `AiForecastService.runForecast(String productId)`
2. Create `NightlyForecastJob` scheduled job
3. Endpoint: `POST /api/v1/ai/forecast/run/{productId}`
4. Test with product seed data: verify `DemandForecast` record is created/updated

### Feature 3.2 — Churn Prediction
**Dependencies:** Customer, CustomerCommunicationLog, Order repositories

Key consideration: `Order` is in a different module. Use plain String FK:
```java
@Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
List<Order> findRecentByCustomerId(String customerId, Pageable pageable);
```

### Feature 3.3 — Inventory Optimization
**Dependencies:** Inventory, StockMovement, Warehouse, DemandForecast repositories

---

## Phase 4: Conversational AI

### Feature 4.1 — Support Chatbot
This is the most complex feature. Extra care needed.

Implementation steps:
1. Implement `SupportChatbotService` (full code in `02-memory-management.md`)
2. Create `ChatbotController` with 4 endpoints (start, message, history, escalate)
3. Implement context injection (customer orders, shipments, returns)
4. Test multi-turn conversation: 5+ turns, sliding window at turn 12+
5. Test session expiry: create session, advance clock 25 hours, verify rejection

**Critical edge cases to test:**
- Customer ID doesn't exist → 404
- Conversation ID doesn't exist → 404
- Expired session → 410 Gone
- Turn count = 50 (max) → force summary or reject new turns
- Claude returns escalation signal → `escalate: true` in response

### Feature 4.2 — Natural Language Reports
Implementation steps:
1. Implement `NlReportService` with two-step flow:
   - Step 1: Claude identifies report type and filters (query spec)
   - Step 2: Backend executes query against existing analytics data
   - Step 3: Claude formats and narrates the result
2. Create `AiReportController`
3. Wire conversation memory (same `AiConversation` entity as chatbot)

---

## Phase 5: Financial & Operational AI

These features follow the same pattern as Phase 3 (batch jobs + on-demand endpoints).
Each reads financial data → calls Claude → writes AiInsight.

| Feature | Key Entity | Scheduled |
|---------|-----------|-----------|
| Budget Anomaly | Budget, BudgetLine, Expense | Monthly |
| Vendor Analysis | Vendor, VendorInvoice | Monthly |
| Return Pattern | RefundRequest, ChargebackDispute | Weekly |

---

## Phase 6: Advisory Features

These features return recommendations but never automatically modify production data.
The admin must explicitly accept the AI suggestion.

Pattern:
```java
// 1. AI generates recommendation → stored in AiInsight (status = PENDING_REVIEW)
// 2. Admin reviews via UI or API
// 3. Admin calls PATCH /apply/{insightId}
// 4. Service reads insightId's recommendation and applies to the entity
```

| Feature | Advisory Output | Apply Endpoint |
|---------|----------------|----------------|
| Pricing | New price suggestion | PATCH /api/v1/ai/pricing/apply/{insightId} |
| SEO | New meta title/description | PATCH /api/v1/ai/seo/apply/{productId} |
| Marketing | Generated copy | POST /api/v1/ai/marketing/send/{insightId} |
| Shipping | Carrier recommendation | PATCH /api/v1/ai/shipping/apply/{shipmentId} |

---

## Testing Strategy

### Unit Tests (no Anthropic API calls)
Use `@MockBean AnthropicClient` or Mockito to mock the SDK.
Test:
- Correct prompt is built from entity data
- Correct fields are written back to entity after AI response
- Budget exception is thrown when over limit
- Rate limit exception is thrown when over limit
- Conversation sliding window works correctly

### Integration Tests (with H2 database)
Use `@SpringBootTest` with `spring.profiles.active=test`.
Test:
- Entity is saved to H2 after AI call
- Scheduled job can be triggered manually via `job.run()`
- REST endpoints return correct HTTP codes

### Contract Tests (with real Anthropic API)
Mark with `@Tag("contract")` — only run in CI with real `ANTHROPIC_API_KEY`.
Test:
- Claude returns valid JSON for fraud prompt
- Claude returns valid JSON for all other structured-output features
- Response respects maxTokens limit

### Load Tests
Use JMeter or k6 for:
- Fraud detection: 100 concurrent transactions/second
- Support chatbot: 50 concurrent multi-turn sessions

---

## Go-Live Checklist

### Environment
- [ ] `ANTHROPIC_API_KEY` set in production secrets manager (not `.env` file)
- [ ] `spring.profiles.active=prod` set
- [ ] Redis configured (for production conversation caching)
- [ ] MySQL/PostgreSQL configured (not H2)
- [ ] All `ai_*` tables created (via JPA DDL auto or Flyway migration)

### Budget Controls
- [ ] Global daily budget set (`anthropic.budget.daily-total-usd`)
- [ ] Per-feature budgets set in `ai_configs` table
- [ ] Alert thresholds configured in monitoring tool
- [ ] Slack webhook configured for budget alerts

### Feature Toggles
- [ ] All features start as `enabled = false` in production
- [ ] Enable features one by one after smoke testing
- [ ] Confirm fallback behaviour works when feature is disabled (non-AI path still works)

### Observability
- [ ] Actuator `/actuator/metrics` endpoint accessible to monitoring
- [ ] `ai.calls`, `ai.latency`, `ai.spend.daily` metrics visible in Grafana/Datadog
- [ ] Slow call alerts configured (> 5 seconds)
- [ ] Error rate alert configured (> 5%)

### Data Retention
- [ ] `ai_conversations` expiry confirmed (24h chatbot, 2h reports)
- [ ] `ai_insights` retention policy agreed (recommend: 2 years, then archive)
- [ ] `ai_usage_logs` retention policy agreed (recommend: 6 months)
- [ ] Cleanup jobs running (verify via logs)

### Security
- [ ] `AiAdminController` endpoints restricted to SUPER_ADMIN role
- [ ] AI insight endpoints restricted to appropriate roles per feature
- [ ] No customer PII logged in `ai_usage_logs.errorMessage`
- [ ] `ANTHROPIC_API_KEY` not visible in application logs

---

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Claude returns non-JSON text when asked for JSON | Add `"Respond ONLY with valid JSON, no markdown"` to system prompt |
| Context window overflow in chatbot | Sliding window already handled by `AiMemoryManager` |
| Nightly batch job fails on one product, kills entire batch | Wrap each iteration in try/catch, log and continue |
| Prompt cache not triggering | Ensure system prompt block is > 1,024 tokens; use same object reference |
| `OutOfMemoryError` loading full order history | Always use `Pageable` — never load all records at once |
| Rate limit from Anthropic (429) | `AiOrchestrator` handles with exponential backoff retry |
| AI changes prices automatically | Pricing/SEO/Marketing are ADVISORY ONLY — always require human approval |
| Cross-module entity access | Always use plain String FK + dedicated query service, never `@ManyToOne` across modules |

---

## Dependency Map

```
Phase 1 (Foundation)
  └── Phase 2 (Quick Wins)
        ├── Fraud Detection     → requires PaymentTransaction events
        ├── Product Description → requires Product entity
        └── Sentiment Analysis  → requires Review events
  └── Phase 3 (Batch)
        ├── Demand Forecast     → requires DemandForecast entity
        ├── Churn Prediction    → requires Customer + cross-module Order query
        └── Inventory Optim.    → requires DemandForecast (Phase 3.1 first)
  └── Phase 4 (Conversational)
        ├── Support Chatbot     → requires AiConversation, cross-module order/shipment
        └── NL Reports          → requires SavedReport, AiConversation
  └── Phase 5 (Financial)
        ├── Budget Anomaly      → requires Finance module entities
        ├── Vendor Analysis     → requires Finance module entities
        └── Return Pattern      → requires Payment module entities
  └── Phase 6 (Advisory)
        ├── Pricing             → requires DemandForecast (Phase 3.1)
        ├── SEO                 → requires SeoMetadata entity
        ├── Marketing           → requires Customer + Notification module
        └── Shipping            → requires Shipment, Carrier entities
```
