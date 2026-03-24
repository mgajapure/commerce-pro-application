# AI Memory Management

How memory works at the code level for every AI feature in Commerce Pro.

---

## The Four Memory Layers

```
┌─────────────────────────────────────────────────────────────────────┐
│  LAYER 1 — In-Context Memory (per API call)                         │
│  Lives in the MessageCreateParams sent to the Anthropic SDK.        │
│  Destroyed after the response. Zero persistence cost.               │
├─────────────────────────────────────────────────────────────────────┤
│  LAYER 2 — Prompt Cache (Anthropic-side, 5-min TTL)                 │
│  System prompts and repeated static blocks are cached at Anthropic. │
│  Cache hit = 90% token discount on those tokens.                    │
├─────────────────────────────────────────────────────────────────────┤
│  LAYER 3 — Session / Conversation Memory (JPA — ai_conversations)   │
│  Multi-turn chat histories stored as JSON in the DB.                │
│  Loaded per session, injected into every follow-up API call.        │
├─────────────────────────────────────────────────────────────────────┤
│  LAYER 4 — Long-Term DB Memory (JPA — ai_insights)                  │
│  AI decisions, scores, and reasoning persisted forever.             │
│  Fed back into future prompts as historical context.                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Layer 1: In-Context Memory

### What goes into the context window
Every API call to Claude consists of:
1. **System block** — role + rules + output format (static, cacheable)
2. **User block(s)** — current data payload (dynamic per request)
3. **Assistant blocks** — prior turns (for multi-turn features only)

### Token budget rules
```
Max context window: 200,000 tokens (claude-sonnet-4-6)
Safe operational budget per feature:

Feature                   System  Historical  Current  Max Output
─────────────────────────────────────────────────────────────────
Fraud Detection           500     2,000       1,000    512
Demand Forecast           600     5,000       3,000    1,024
Churn Prediction          700     4,000       2,000    1,024
NL Reports (turn 1)       800     0           2,000    2,048
NL Reports (turn N)       800     10,000      2,000    2,048
Support Chatbot (turn 1)  1,000   3,000       500      1,024
Support Chatbot (turn N)  1,000   8,000       500      1,024
Product Description       400     0           300      512
Sentiment Analysis        300     0           400      256
Pricing Recommendation    600     3,000       1,000    1,024
Inventory Optimization    800     6,000       3,000    2,048
Budget Anomaly            700     5,000       4,000    2,048
SEO Optimizer             400     0           500      512
Marketing Copy            600     2,000       500      1,024
Return Pattern            700     5,000       3,000    2,048
Vendor Analysis           600     4,000       2,000    1,500
Shipping Optimization     400     1,000       800      512
```

### AiMemoryManager — context assembly
```java
@Service
public class AiMemoryManager {

    // Maximum tokens to allocate to historical context before truncation
    private static final int MAX_HISTORY_TOKENS = 10_000;

    /**
     * Builds the message list for a multi-turn conversation.
     * Applies sliding window truncation to keep within budget.
     */
    public List<MessageParam> buildConversationMessages(AiConversation conversation, String newUserMessage) {
        List<ConversationTurn> history = conversation.getTurnsAsList();

        // Estimate token count (rough: 1 token ≈ 4 chars)
        int historyTokens = history.stream()
            .mapToInt(t -> (t.userMessage().length() + t.assistantMessage().length()) / 4)
            .sum();

        // Drop oldest turns if over budget (sliding window)
        while (historyTokens > MAX_HISTORY_TOKENS && history.size() > 1) {
            ConversationTurn oldest = history.remove(0);
            historyTokens -= (oldest.userMessage().length() + oldest.assistantMessage().length()) / 4;
        }

        List<MessageParam> messages = new ArrayList<>();

        // Inject prior turns as alternating user/assistant blocks
        for (ConversationTurn turn : history) {
            messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(turn.userMessage())
                .build());
            messages.add(MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .content(turn.assistantMessage())
                .build());
        }

        // Add current user message
        messages.add(MessageParam.builder()
            .role(MessageParam.Role.USER)
            .content(newUserMessage)
            .build());

        return messages;
    }

    /**
     * Truncates a data payload string to a max token budget.
     * Used for historical order/transaction data injected as context.
     */
    public String truncateToTokenBudget(String data, int maxTokens) {
        int maxChars = maxTokens * 4;
        if (data.length() <= maxChars) return data;
        return data.substring(0, maxChars) + "\n[...truncated for context budget]";
    }
}
```

---

## Layer 2: Prompt Caching

### How it works
The Anthropic Java SDK supports prompt caching via the `CacheControl` header on system content blocks. When Claude sees the same system prompt twice within 5 minutes, the second call is a cache hit — those input tokens are billed at 10% of normal cost.

### Cache eligibility
- Minimum 1,024 tokens to be worth caching
- TTL: 5 minutes (refreshed on each cache hit)
- Works on: system prompts, long static user-turn prefixes (e.g. product catalogue)

### Implementation
```java
@Component
public class AiSystemPrompts {

    // ── Fraud Detection ──────────────────────────────────────────────────────
    public static final String FRAUD_DETECTION = """
        You are a payment fraud analyst for a high-volume e-commerce platform.

        SCORING RULES:
        - Velocity: flag if customer placed > 3 transactions in the last hour
        - Geo mismatch: flag if billing country != IP geolocation country
        - Amount anomaly: flag if amount > 5x customer average order value
        - New customer high-value: flag amounts > $500 for customers with < 3 orders
        - Card testing: flag small test charges (< $2) followed by large charge

        RISK LEVELS:
        - LOW (0-30): Standard processing, no action
        - MEDIUM (31-60): Flag for manual review within 24h
        - HIGH (61-85): Hold transaction, require additional verification
        - CRITICAL (86-100): Block immediately, notify fraud team

        Always output valid JSON with this exact structure:
        {
          "riskScore": <integer 0-100>,
          "riskLevel": "<LOW|MEDIUM|HIGH|CRITICAL>",
          "reasons": ["<reason1>", "<reason2>"],
          "recommendation": "<APPROVE|REVIEW|HOLD|BLOCK>",
          "confidence": <0.0-1.0>
        }
        """;

    // ── Demand Forecasting ────────────────────────────────────────────────────
    public static final String DEMAND_FORECAST = """
        You are a supply chain demand forecasting specialist.
        ...
        """;

    // ── Support Chatbot ───────────────────────────────────────────────────────
    public static final String SUPPORT_CHATBOT = """
        You are a customer support agent for Commerce Pro, a professional e-commerce platform.
        Be friendly, concise, and solution-focused. Never reveal internal system details.
        Escalate to human when: customer is distressed, issue requires system override, legal threats.
        ...
        """;
}
```

```java
// Building a cached request (Anthropic Java SDK 2.x)
MessageCreateParams params = MessageCreateParams.builder()
    .model("claude-haiku-4-5-20251001")
    .maxTokens(512)
    .system(List.of(
        TextBlockParam.builder()
            .text(AiSystemPrompts.FRAUD_DETECTION)
            .cacheControl(CacheControlEphemeral.builder().build())   // mark for caching
            .build()
    ))
    .addUserMessage(userPrompt)
    .build();
```

### Which features use caching

| Feature | Cache system prompt? | Cache static data? |
|---------|---------------------|--------------------|
| Fraud Detection | YES | NO |
| Demand Forecasting | YES | NO |
| Churn Prediction | YES | NO |
| NL Reports | YES | NO |
| Support Chatbot | YES | YES (order list, turn 1) |
| Product Description | YES | NO |
| Sentiment Analysis | YES | NO |
| All others | YES | NO |

**Rule of thumb:** Cache any block > 1,024 tokens that repeats across requests. System prompts almost always qualify.

---

## Layer 3: Session / Conversation Memory

Used by: **NL Reports** (Feature 4), **Support Chatbot** (Feature 5).

### Storage
`ai_conversations` table (see `03-entities-schema.md`).

### Session lifecycle
```
Client                    Backend                        DB
  │                          │                           │
  │ POST /chatbot/start       │                           │
  │──────────────────────────>│                           │
  │                          │ Load Customer context      │
  │                          │───────────────────────────>│
  │                          │<───────────────────────────│
  │                          │ Create AiConversation      │
  │                          │───────────────────────────>│
  │ { conversationId, msg }   │                           │
  │<─────────────────────────│                           │
  │                          │                           │
  │ POST /chatbot/message     │                           │
  │ { conversationId, msg }   │                           │
  │──────────────────────────>│                           │
  │                          │ Load conversation history  │
  │                          │───────────────────────────>│
  │                          │<───────────────────────────│
  │                          │ Build messages with        │
  │                          │ sliding window             │
  │                          │ Call Anthropic API         │
  │                          │ Append new turn to conv    │
  │                          │───────────────────────────>│
  │ { reply }                 │                           │
  │<─────────────────────────│                           │
```

### Conversation service pattern
```java
@Service
@Transactional
public class SupportChatbotService {

    private final AiConversationRepository convRepo;
    private final AiMemoryManager memoryManager;
    private final AnthropicClient client;
    private final CustomerRepository customerRepo;

    public ChatStartResponse startSession(String customerId) {
        Customer customer = customerRepo.findById(customerId).orElseThrow();

        // Build rich context block injected as a hidden "turn 0"
        String contextBlock = buildContextBlock(customer);

        AiConversation conv = AiConversation.builder()
            .sessionType(SessionType.SUPPORT_CHAT)
            .customerId(customerId)
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();

        // Add context as a hidden first turn
        conv.addTurn(contextBlock, "Understood. I'm ready to help " + customer.getFirstName() + ".");
        convRepo.save(conv);

        return new ChatStartResponse(conv.getId(), "Hi " + customer.getFirstName() + "! How can I help?");
    }

    public ChatReply sendMessage(String conversationId, String userMessage) {
        AiConversation conv = convRepo.findById(conversationId).orElseThrow();

        if (conv.isExpired()) {
            throw new ConversationExpiredException("Session expired. Please start a new chat.");
        }

        // Build message list with sliding window truncation
        List<MessageParam> messages = memoryManager.buildConversationMessages(conv, userMessage);

        MessageCreateParams params = MessageCreateParams.builder()
            .model("claude-sonnet-4-6")
            .maxTokens(1024)
            .system(AiSystemPrompts.SUPPORT_CHATBOT)
            .messages(messages)
            .build();

        Message response = client.messages().create(params);
        String reply = extractText(response);

        // Persist this turn
        conv.addTurn(userMessage, reply);
        conv.setLastActiveAt(LocalDateTime.now());
        convRepo.save(conv);

        boolean escalate = reply.contains("connecting you to a specialist");
        return new ChatReply(reply, escalate, conv.getTurnCount());
    }

    private String buildContextBlock(Customer customer) {
        // Returns a detailed context string with customer's orders, shipments, returns
        // This is injected as the FIRST hidden user turn so Claude has full context
        return """
            [SYSTEM CONTEXT - NOT VISIBLE TO CUSTOMER]
            Customer: %s (ID: %s), Tier: %s
            Lifetime spend: $%s, Orders: %d
            Recent orders: %s
            Open shipments: %s
            Open returns: %s
            """.formatted(
                customer.getFullName(), customer.getId(), customer.getTier(),
                customer.getLifetimeSpend(), customer.getTotalOrders(),
                fetchRecentOrders(customer.getId()),
                fetchOpenShipments(customer.getId()),
                fetchOpenReturns(customer.getId())
            );
    }
}
```

### Conversation expiry & cleanup
```java
// Scheduled cleanup — runs nightly
@Scheduled(cron = "0 1 * * *")
public void expireOldConversations() {
    int deleted = convRepo.deleteByExpiresAtBefore(LocalDateTime.now());
    log.info("Expired {} AI conversations", deleted);
}
```

---

## Layer 4: Long-Term DB Memory (AiInsight)

Used by: all stateless features (Fraud, Churn, Forecast, Sentiment, etc.)

### Purpose
- Audit trail of every AI decision
- Feed prior AI decisions back into future prompts as "historical context"
- Cost tracking (tokensUsed per call)
- Reporting (how many HIGH fraud scores this week, etc.)

### Pattern: reading past insights as context
```java
@Service
public class ChurnPredictionService {

    private final AiInsightRepository insightRepo;

    private String buildHistoricalChurnContext(String customerId) {
        // Load the last 3 churn predictions for this customer
        List<AiInsight> past = insightRepo.findByEntityIdAndFeatureTypeOrderByCreatedAtDesc(
            customerId, AiFeatureType.CHURN_PREDICTION, PageRequest.of(0, 3));

        if (past.isEmpty()) return "No previous churn assessments.";

        return past.stream()
            .map(i -> "- %s: score=%d, risk=%s, action=%s".formatted(
                i.getCreatedAt().toLocalDate(), i.getScore(), i.getRiskLevel(), i.getRecommendation()))
            .collect(Collectors.joining("\n", "Previous churn assessments:\n", ""));
    }
}
```

---

## Memory Strategy Per Feature

| Feature | Layer 1 | Layer 2 Cache | Layer 3 Session | Layer 4 DB |
|---------|---------|---------------|-----------------|------------|
| Fraud Detection | Current tx + last 5 tx | System prompt | No | Yes (every tx) |
| Demand Forecast | 90-day sales history | System prompt | No | Yes (per forecast run) |
| Churn Prediction | Customer profile + 90-day behaviour | System prompt | No | Yes (weekly per customer) |
| NL Reports | Current query + data | System prompt | Yes (session turns) | Yes (saved reports) |
| Support Chatbot | Current message | System + context block | Yes (full chat) | Yes (comm log) |
| Product Description | Product attributes | System prompt | No | Optional |
| Sentiment Analysis | Review text | System prompt | No | Yes (per review) |
| Pricing Recommendation | Product + inventory + demand | System prompt | No | Yes (per product) |
| Inventory Optimization | All SKUs + stock | System prompt | No | Yes (weekly run) |
| Budget Anomaly | Budget vs. actual | System prompt | No | Yes (monthly run) |
| SEO Optimizer | Product + current SEO | System prompt | No | Yes (per product) |
| Marketing Copy | Customer profile + campaign | System prompt | No | Yes (per generation) |
| Return Pattern | Return history per product | System prompt | No | Yes (weekly run) |
| Vendor Analysis | Invoice history per vendor | System prompt | No | Yes (monthly run) |
| Shipping Optimization | Shipment + carrier rates | System prompt | No | Yes (per shipment) |

---

## Sliding Window Algorithm

For conversation features, the sliding window prevents runaway context growth:

```java
/**
 * Sliding window with token budget enforcement.
 * Keeps the most recent turns within the budget.
 * Always preserves turn 0 (context injection).
 */
public List<ConversationTurn> applySlidingWindow(List<ConversationTurn> turns, int maxTokens) {
    if (turns.isEmpty()) return turns;

    // Turn 0 is always kept (customer context injection)
    ConversationTurn contextTurn = turns.get(0);
    List<ConversationTurn> recent = new ArrayList<>(turns.subList(1, turns.size()));

    int budget = maxTokens - estimateTokens(contextTurn);

    // Trim from oldest until within budget
    while (!recent.isEmpty() && totalTokens(recent) > budget) {
        recent.remove(0);
    }

    List<ConversationTurn> result = new ArrayList<>();
    result.add(contextTurn);
    result.addAll(recent);
    return result;
}

private int estimateTokens(ConversationTurn turn) {
    return (turn.userMessage().length() + turn.assistantMessage().length()) / 4;
}

private int totalTokens(List<ConversationTurn> turns) {
    return turns.stream().mapToInt(this::estimateTokens).sum();
}
```

---

## Memory for Scheduled Batch Jobs

Batch jobs (Demand Forecast, Churn, Inventory, Budget) process multiple records in a loop. Each iteration is **independent** — no session memory between records. The AI insight from one product's forecast does not influence another product's forecast.

```java
@Scheduled(cron = "0 2 * * *")
public void runNightlyForecast() {
    List<String> productIds = productRepo.findActiveProductIds();

    for (String productId : productIds) {
        try {
            // Each call is fully independent — new context, new API call
            aiForecastService.runForecast(productId);

            // Small delay between calls to respect rate limits
            Thread.sleep(200);
        } catch (Exception e) {
            log.error("Forecast failed for product {}: {}", productId, e.getMessage());
            // Continue with next product — don't abort the batch
        }
    }
}
```

---

## Redis Cache (Optional, Production)

For high-traffic features like the support chatbot, conversation state can be cached in Redis to avoid DB round-trips on every message:

```java
// application-prod.properties
spring.cache.type=redis
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=6379

// Service usage
@Cacheable(value = "ai_conversations", key = "#conversationId", unless = "#result == null")
public AiConversation loadConversation(String conversationId) { ... }

@CacheEvict(value = "ai_conversations", key = "#conversation.id")
public void saveConversation(AiConversation conversation) { ... }
```

---

## Memory Limits Quick Reference

```
Conversation expiry:
  Support Chatbot:    24 hours of inactivity
  NL Report session:   2 hours of inactivity

Max turns before forced summary:
  Support Chatbot:    50 turns
  NL Reports:         20 turns

Max context tokens allocated per feature:
  Haiku features:     10,000 tokens (plenty for these simple tasks)
  Sonnet features:    20,000 tokens

Token budget for historical DB context injected into prompt:
  Max prior AI insights injected:  3 records
  Max prior insights token budget: 1,000 tokens
```
