# AI Features Catalog

Complete specification for all 15 AI features in Commerce Pro.

---

## Feature 1: Fraud Detection

### Purpose
Real-time scoring of every payment transaction before or immediately after capture. Claude evaluates velocity signals, device fingerprints, geographical anomalies, and historical patterns to produce a risk score and a recommended action.

### Domain
`com.commerce_pro_backend.payment`

### Existing Entities Read
- `PaymentTransaction` — amount, currency, gateway, IP, card details, customer history
- `Customer` — `riskScore`, `isFraudFlagged`, `lifetimeSpend`, `totalOrders`, blacklist status

### AI Output Stored In
- `PaymentTransaction.riskScore` (Integer 0–100)
- `PaymentTransaction.isFlagged` (Boolean)
- `PaymentTransaction.flagReason` (String)
- `AiInsight` — full reasoning, recommendation, tokens used

### Trigger
- `@EventListener` on `PaymentTransactionCreatedEvent` (synchronous, inline)
- Fallback: scheduled sweep every 5 minutes for unscored transactions

### Model
`claude-haiku-4-5` — must respond in < 300 ms; haiku is sufficient for pattern scoring

### Prompt Pattern
```
System (cached):
  You are a payment fraud analyst for an e-commerce platform.
  Scoring rules: [VELOCITY] max 3 transactions/hour per customer.
  [GEO] flag country mismatches. [AMOUNT] flag > 5x customer AOV.
  Output JSON: { "riskScore": 0-100, "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                 "reasons": [...], "recommendation": "APPROVE|REVIEW|BLOCK" }

User (per-request):
  Transaction: amount=$299, currency=USD, gateway=STRIPE,
               card_last4=4242, ip=185.x.x.x (RU)
  Customer: lifetime_spend=$450, total_orders=3, avg_order=$150,
            last_order=2 days ago, fraud_flagged=false
  Last 5 transactions of this customer: [...]
  Score this transaction.
```

### API Endpoint
```
POST /api/v1/ai/fraud/score/{transactionId}
GET  /api/v1/ai/fraud/insights?customerId=&page=&size=
```

### Java Service Skeleton
```java
@Service
public class FraudDetectionService {

    private final AnthropicClient client;
    private final PaymentTransactionRepository txRepo;
    private final CustomerRepository customerRepo;
    private final AiInsightRepository insightRepo;

    public FraudResult analyze(String transactionId) {
        PaymentTransaction tx = txRepo.findById(transactionId).orElseThrow();
        Customer customer = customerRepo.findById(tx.getCustomerId()).orElse(null);

        String userPrompt = buildUserPrompt(tx, customer);

        MessageCreateParams params = MessageCreateParams.builder()
            .model("claude-haiku-4-5-20251001")
            .maxTokens(512)
            .system(SYSTEM_PROMPT)                         // cached
            .addUserMessage(userPrompt)
            .build();

        Message response = client.messages().create(params);
        FraudResult result = parseJson(response.content());

        // Persist back to transaction
        tx.setRiskScore(result.riskScore());
        tx.setIsFlagged(result.riskLevel() == RiskLevel.HIGH || result.riskLevel() == RiskLevel.CRITICAL);
        tx.setFlagReason(String.join("; ", result.reasons()));
        txRepo.save(tx);

        // Persist AI insight for audit trail
        insightRepo.save(AiInsight.builder()
            .featureType(AiFeatureType.FRAUD_DETECTION)
            .entityId(transactionId)
            .entityType("TRANSACTION")
            .score(result.riskScore())
            .riskLevel(result.riskLevel().name())
            .reasoning(result.reasoning())
            .recommendation(result.recommendation().name())
            .modelUsed("claude-haiku-4-5-20251001")
            .tokensUsed(response.usage().inputTokens() + response.usage().outputTokens())
            .build());

        return result;
    }
}
```

---

## Feature 2: AI-Enhanced Demand Forecasting

### Purpose
Augments the existing `DemandForecast` entity with an AI-generated narrative analysis, outlier detection, and actionable procurement recommendations based on sales velocity, seasonality, and external context (holidays, trends).

### Domain
`com.commerce_pro_backend.inventory.forecast`

### Existing Entities Read
- `DemandForecast` — existing statistical forecast fields
- `Inventory` — current stock level, reorder point
- `StockMovement` — last 90 days of movements
- `Product` — category, seasonal tags

### AI Output Stored In
- `DemandForecast.generatedBy` = `"AI_CLAUDE"`
- `DemandForecast.algorithm` = `"CLAUDE_HYBRID"`
- `AiInsight` — narrative explanation, top 3 recommendations

### Trigger
- Scheduled `@Scheduled(cron = "0 2 * * *")` — nightly batch for all active products
- On-demand: `POST /api/v1/ai/forecast/run/{productId}`

### Model
`claude-sonnet-4-6` — needs reasoning for multi-factor analysis

### Prompt Pattern
```
System (cached):
  You are a supply-chain analyst. Given sales history data, produce:
  1. totalPredictedDemand (integer) for the period
  2. averageDailyDemand (double)
  3. peakDemandQuantity (integer)
  4. safetyStockRecommendation (integer)
  5. reorderPointRecommendation (integer)
  6. narrative: 2-3 sentence plain-English explanation
  7. alerts: list of risk flags (stockout risk, overstock risk, etc.)
  Output as JSON matching the DemandForecast schema.

User (per-request):
  Product: SKU-1234, "Winter Jacket", Category: Apparel
  Current stock: 150 units at Warehouse-A
  Last 90 days movements: [daily sales array]
  Forecast period: next 30 days
  Notable context: Black Friday in 12 days
```

### API Endpoint
```
POST /api/v1/ai/forecast/run/{productId}
POST /api/v1/ai/forecast/batch          (triggers nightly job immediately)
GET  /api/v1/ai/forecast/{forecastId}/explanation
```

---

## Feature 3: Customer Churn Prediction

### Purpose
Predicts the probability that a customer will stop purchasing within the next 30/60/90 days. Updates `Customer.riskScore`. High-risk customers are flagged for retention campaigns.

### Domain
`com.commerce_pro_backend.customer`

### Existing Entities Read
- `Customer` — tier, lifetimeSpend, totalOrders, lastOrderAt, loyaltyPoints, status
- `CustomerCommunicationLog` — last 10 communication events
- `Order` (cross-module via plain String FK) — order frequency, cart abandonment
- `Wishlist` — engagement signal

### AI Output Stored In
- `Customer.riskScore` (Integer 0–100, repurposed for churn risk)
- `AiInsight` — churn probability, top churn drivers, recommended retention action

### Trigger
- `@Scheduled(cron = "0 3 * * 0")` — weekly Sunday batch for all ACTIVE customers
- On-demand: `POST /api/v1/ai/churn/predict/{customerId}`

### Model
`claude-sonnet-4-6`

### Prompt Pattern
```
System (cached):
  You are a customer retention specialist.
  Churn signals: no purchase in 45+ days, declining order frequency,
  support tickets with no resolution, cancelled orders, low loyalty engagement.
  Score 0 (zero churn risk) to 100 (certain churn).
  Output JSON: { "churnScore": 0-100, "churnRisk": "LOW|MEDIUM|HIGH|CRITICAL",
    "top_drivers": [...], "recommended_action": "DISCOUNT|LOYALTY_BONUS|PERSONAL_OUTREACH|WIN_BACK_EMAIL",
    "predicted_churn_window_days": 30|60|90 }

User:
  Customer: Gold tier, $2,400 lifetime spend, 18 orders, last order 52 days ago
  Order frequency trend: monthly -> bi-monthly -> quarterly (declining)
  Recent communication: 1 support ticket (unresolved, 8 days ago)
  Wishlist items: 3 (last added 30 days ago)
  Predict churn.
```

### API Endpoint
```
POST /api/v1/ai/churn/predict/{customerId}
GET  /api/v1/ai/churn/high-risk?threshold=70&page=&size=
POST /api/v1/ai/churn/batch
```

---

## Feature 4: Natural Language Report Generation

### Purpose
Allows admin users to ask analytics questions in plain English. Translates the question into a report query, executes it against existing analytics data, and returns the answer as formatted prose + a data table.

### Domain
`com.commerce_pro_backend.analytics`

### Existing Entities Read / Written
- `SavedReport` — saves the NL query and result for later reuse
- `ReportExecution` — records each execution
- `AiConversation` — multi-turn context (e.g. "filter by category" in turn 2 refers to turn 1's result)

### Trigger
User-initiated via REST; supports multi-turn follow-ups within a session.

### Model
`claude-sonnet-4-6`

### Prompt Pattern
```
System (cached):
  You are a business intelligence assistant for an e-commerce admin.
  Available report types: SALES, INVENTORY, CUSTOMER, ORDER, FINANCIAL, FULFILLMENT.
  When the user asks a question, first identify the report type and required filters.
  Then either: (a) provide the answer from context already given, or
               (b) output a structured query specification JSON so the backend can fetch data.
  Query spec format: { "reportType": "...", "filters": {...}, "groupBy": "...", "orderBy": "..." }

Turn 1 user: "Show me top 10 products by revenue last month"
Turn 2 user: "Now filter that by category Electronics"     ← needs conversation context
Turn 3 user: "What was the return rate for those products?"
```

### API Endpoint
```
POST /api/v1/ai/reports/query
     Body: { "question": "...", "conversationId": "uuid (optional for follow-up)" }
GET  /api/v1/ai/reports/conversations/{conversationId}
DELETE /api/v1/ai/reports/conversations/{conversationId}
```

---

## Feature 5: Customer Support Chatbot

### Purpose
AI-powered support agent that handles customer inquiries about orders, shipments, returns, and product questions. Has full order/shipment context injected on session start. Escalates to human agent when confidence is low.

### Domain
`com.commerce_pro_backend.customer`

### Existing Entities Read
- `Customer` — name, tier, order history summary
- `Order` / `OrderItem` — order status, items
- `Shipment` / `TrackingEvent` — real-time tracking
- `RefundRequest` — return/refund status

### Entities Written
- `AiConversation` — full chat history stored per customer session
- `CustomerCommunicationLog` — records AI interaction as a communication event

### Trigger
Customer-facing API; session starts when customer opens support widget.

### Model
`claude-sonnet-4-6`

### Prompt Pattern
```
System (cached):
  You are a helpful e-commerce customer support agent for Commerce Pro.
  Always be polite, concise, and solution-focused.
  You can help with: order status, shipment tracking, returns, product questions.
  If you cannot resolve an issue, say: "I'm connecting you to a specialist."
  Never reveal internal system details or pricing logic.

User turn 1 (context injection, not displayed):
  Customer: John Doe (Gold tier), customerId: uuid
  Recent orders: [last 3 orders with status]
  Open shipments: [tracking info]
  Open returns: [refund requests]

User turn 1 (displayed):
  "Where is my order ORD-2024-001?"
```

### API Endpoint
```
POST /api/v1/ai/chatbot/start          → { conversationId, welcomeMessage }
POST /api/v1/ai/chatbot/message        → { conversationId, message } → { reply, escalate: bool }
GET  /api/v1/ai/chatbot/history/{conversationId}
POST /api/v1/ai/chatbot/escalate/{conversationId}
```

---

## Feature 6: Product Description Generator

### Purpose
Auto-generates compelling, SEO-optimised product descriptions from a product's attributes, category, brand, and variant data. Supports tone customisation (professional, casual, luxury).

### Domain
`com.commerce_pro_backend.catalog.product`

### Existing Entities Read
- `Product` — name, brand, category, attributes, price range
- `ProductAttribute` — technical specs
- `ProductVariant` — size/colour options
- `SeoMetadata` — existing keywords to incorporate

### AI Output Stored In
- `Product.description` (TEXT column)

### Trigger
- On-demand: `POST /api/v1/ai/catalog/describe/{productId}`
- Batch: `POST /api/v1/ai/catalog/describe/batch` (list of productIds)

### Model
`claude-haiku-4-5` — purely creative/generative, no complex reasoning needed

### Prompt Pattern
```
System (cached):
  You are a professional e-commerce copywriter.
  Rules: 100-200 words, highlight key benefits, include 3-5 SEO keywords naturally,
  end with a soft call-to-action. Tone: {tone}.
  Output plain text only, no markdown.

User:
  Product: "Men's Premium Down Jacket"
  Brand: NorthPeak
  Category: Apparel > Outerwear > Jackets
  Key attributes: Fill power 700, Water resistant, Weight 650g, Sizes: S-3XL
  Existing SEO keywords: winter jacket, down jacket men, warm outdoor jacket
  Generate a product description.
```

---

## Feature 7: Review Sentiment Analysis

### Purpose
Analyses customer reviews to extract: overall sentiment, sentiment score, key positive/negative themes, and whether the review indicates a product defect or delivery issue. Aggregates insights at the product level.

### Domain
`com.commerce_pro_backend.catalog.review`

### Existing Entities Read
- `Review` — rating, title, body, verified purchase flag

### AI Output Stored In
- `AiInsight` — sentimentScore, themes, flags, productId summary
- Product-level aggregation (optional `ProductSentimentSummary` view entity)

### Trigger
- `@EventListener` on `ReviewCreatedEvent`
- Batch backfill: `POST /api/v1/ai/sentiment/batch/{productId}`

### Model
`claude-haiku-4-5` — classification task, very cheap at scale

### Prompt Pattern
```
System (cached):
  Analyse the review. Output JSON:
  { "sentiment": "POSITIVE|NEUTRAL|NEGATIVE",
    "score": -100 to 100,
    "themes": { "positive": [...], "negative": [...] },
    "flags": { "defect": bool, "delivery_issue": bool, "size_issue": bool },
    "summary": "one sentence" }

User:
  Rating: 3/5
  Title: "Good but runs small"
  Body: "The jacket quality is excellent, very warm, but the sizing is way off.
         I'm usually a Medium but had to exchange for XL. Shipping was fast."
```

---

## Feature 8: Smart Pricing Recommendations

### Purpose
Recommends optimal pricing for products based on current inventory levels, demand forecasts, competitor positioning, margin targets, and customer tier pricing rules.

### Domain
`com.commerce_pro_backend.catalog.product` + `inventory`

### Existing Entities Read
- `Product` — current price, cost price, category
- `Inventory` — stock level, days on hand
- `DemandForecast` — predicted demand
- `Order` — recent order volume and price sensitivity signals

### AI Output Stored In
- `AiInsight` — recommended price, price change %, reasoning, confidence
- Admin reviews and applies changes manually (AI is advisory only)

### Model
`claude-sonnet-4-6`

### API Endpoint
```
GET  /api/v1/ai/pricing/recommend/{productId}
POST /api/v1/ai/pricing/bulk-recommend      (list of productIds)
```

---

## Feature 9: Inventory Optimization

### Purpose
Given current stock levels, pending orders, forecasted demand, and warehouse capacity, Claude produces a rebalancing plan: which products to reorder, how many units, which warehouse to stock, and which products are at overstock risk.

### Domain
`com.commerce_pro_backend.inventory`

### Existing Entities Read
- `Inventory` — quantity, reorderPoint, warehouse
- `Warehouse` — capacity, location
- `StockMovement` — velocity
- `DemandForecast` — AI forecast output
- `Order` — open/pending orders consuming stock

### AI Output Stored In
- `AiInsight` — reorder recommendations, overstock alerts, transfer suggestions

### Trigger
- `@Scheduled(cron = "0 4 * * 1")` — weekly Monday morning
- On-demand: `POST /api/v1/ai/inventory/optimize`

### Model
`claude-sonnet-4-6`

---

## Feature 10: Budget Anomaly Detection & Financial Insights

### Purpose
Analyses actual vs. budgeted spend, identifies anomalous expense patterns, flags potential duplicate or fraudulent expenses, and provides a plain-English financial health summary.

### Domain
`com.commerce_pro_backend.finance`

### Existing Entities Read
- `Budget` / `BudgetLine` — planned vs. actual
- `Expense` / `ExpenseCategory` — all recorded expenses
- `FinancialPeriod` — period boundaries

### AI Output Stored In
- `AiInsight` — anomaly list, financial health score, top recommendations

### Trigger
- `@Scheduled(cron = "0 6 1 * *")` — monthly on the 1st
- On-demand: `POST /api/v1/ai/finance/analyze`

### Model
`claude-sonnet-4-6`

### Prompt Pattern
```
System (cached):
  You are a financial controller analysing budget vs. actual data.
  Flag: >20% overspend on any line, unusual one-time spikes,
  potential duplicate invoices (same vendor, amount, within 7 days),
  and month-over-month acceleration.
  Output JSON: { "healthScore": 0-100, "anomalies": [...], "summary": "...", "recommendations": [...] }
```

---

## Feature 11: SEO Content Optimizer

### Purpose
Reviews existing `SeoMetadata` for a product or category and suggests improvements to meta titles, descriptions, and keyword lists for better search ranking.

### Domain
`com.commerce_pro_backend.catalog.seo`

### Existing Entities Read
- `SeoMetadata` — metaTitle, metaDescription, keywords
- `Product` — name, description, category

### AI Output
- Suggested new values returned in response body; admin applies changes manually
- `AiInsight` — stores current vs. suggested, improvement score

### Model
`claude-haiku-4-5`

### API Endpoint
```
GET  /api/v1/ai/seo/suggest/{productId}
POST /api/v1/ai/seo/apply/{productId}       (admin explicitly applies AI suggestion)
```

---

## Feature 12: Marketing Personalization

### Purpose
Generates personalised marketing copy (email subject, email body, push notification, SMS) for individual customers or customer segments. Takes into account customer's purchase history, tier, preferences, and recent behaviour.

### Domain
`com.commerce_pro_backend.customer`

### Existing Entities Read
- `Customer` — tier, preferences, marketingOptIn, acquisitionSource
- `CustomerGroup` — segment rules
- `Order` — favourite categories, AOV

### AI Output
- Returns generated copy; stored in `AiInsight` for approval before sending
- Does not send directly — feeds into `Notification` module

### Model
`claude-sonnet-4-6`

### API Endpoint
```
POST /api/v1/ai/marketing/generate
     Body: { "targetType": "CUSTOMER|SEGMENT", "targetId": "...", "campaignGoal": "WIN_BACK|UPSELL|BIRTHDAY" }
```

---

## Feature 13: Return & Refund Pattern Analysis

### Purpose
Identifies products with abnormally high return rates, customer segments that abuse returns, and patterns that indicate product quality issues vs. expectation mismatches.

### Domain
`com.commerce_pro_backend.payment`

### Existing Entities Read
- `RefundRequest` — reason, amount, product info
- `ChargebackDispute` — chargeback reason, resolution
- `Order` / `OrderItem` — product details

### AI Output Stored In
- `AiInsight` — return risk score per product, pattern description, recommended action

### Trigger
- `@Scheduled(cron = "0 5 * * 1")` — weekly
- On-demand: `POST /api/v1/ai/returns/analyze/{productId}`

### Model
`claude-sonnet-4-6`

---

## Feature 14: Vendor Performance Analysis

### Purpose
Scores vendor reliability using invoice payment terms compliance, delivery timelines, invoice accuracy, and dispute history. Produces a vendor health scorecard.

### Domain
`com.commerce_pro_backend.finance`

### Existing Entities Read
- `Vendor` — payment terms, contact info
- `VendorInvoice` / `VendorInvoiceItem` — amounts, dates, status
- `ChargebackDispute` (indirect, via Orders linked to vendor products)

### AI Output Stored In
- `AiInsight` — vendorScore (0–100), reliability rating, risk flags, recommendations

### Trigger
- `@Scheduled(cron = "0 7 1 * *")` — monthly
- On-demand: `POST /api/v1/ai/vendors/score/{vendorId}`

### Model
`claude-sonnet-4-6`

---

## Feature 15: Shipping Route Optimization

### Purpose
Given a shipment's origin warehouse, destination address, package dimensions/weight, and available carriers with their current rates, Claude recommends the optimal carrier + service level balancing cost, speed, and customer SLA.

### Domain
`com.commerce_pro_backend.fulfillment`

### Existing Entities Read
- `Shipment` — destination, weight, dimensions
- `Carrier` — supported services, zones
- `ShippingRule` — business rules (e.g. always use express for Gold+ customers)
- `Customer` — tier (for SLA rules)

### AI Output
- Returned inline as recommendation; `Shipment.carrierId` updated only when admin/auto-accepts
- `AiInsight` — carrier recommendation, cost comparison, reasoning

### Trigger
- On shipment creation: `@EventListener(ShipmentCreatedEvent)`
- Re-evaluate: `POST /api/v1/ai/shipping/recommend/{shipmentId}`

### Model
`claude-haiku-4-5` — rule application + cost comparison, low complexity
