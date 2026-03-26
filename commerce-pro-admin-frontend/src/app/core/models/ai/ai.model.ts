// ─── Admin / Config ─────────────────────────────────────────────────────────

export type AiFeatureType =
  | 'FRAUD_DETECTION'
  | 'CHURN_PREDICTION'
  | 'SENTIMENT_ANALYSIS'
  | 'PRODUCT_DESCRIPTION'
  | 'SUPPORT_CHATBOT'
  | 'DEMAND_FORECAST'
  | 'NL_REPORT'
  | 'PRICING_RECOMMENDATION'
  | 'INVENTORY_OPTIMIZATION'
  | 'BUDGET_ANOMALY'
  | 'SEO_OPTIMIZER'
  | 'MARKETING_PERSONALIZATION'
  | 'RETURN_PATTERN'
  | 'VENDOR_ANALYSIS'
  | 'SHIPPING_OPTIMIZATION';

export interface AiConfigResponse {
  feature: AiFeatureType;
  enabled: boolean;
  model: string;
  maxTokens: number;
  dailyBudgetUsd: number;
  rateLimitPerMinute: number;
  rateLimitPerHour: number;
  fallbackStrategy: string;
  defaultScore: number;
}

export interface AiConfigUpdateRequest {
  enabled?: boolean;
  model?: string;
  maxTokens?: number;
  dailyBudgetUsd?: number;
  rateLimitPerMinute?: number;
  rateLimitPerHour?: number;
  fallbackStrategy?: string;
  defaultScore?: number;
}

export interface AiUsageSummaryItem {
  feature: AiFeatureType;
  totalCalls: number;
  successCalls: number;
  failedCalls: number;
  totalTokens: number;
  totalCostUsd: number;
}

export interface AiDashboardResponse {
  todaySpendUsd: number;
  dailyBudgetUsd: number;
  totalCallsToday: number;
  successCallsToday: number;
  failedCallsToday: number;
  slowCallsToday: number;
  featureSpend: Record<string, number>;
}

// ─── Fraud Detection ─────────────────────────────────────────────────────────

export interface AiFraudResult {
  transactionId: string;
  riskScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  recommendation: 'APPROVE' | 'REVIEW' | 'DECLINE';
  signals: string[];
  reasoning: string;
}

// ─── Churn Prediction ────────────────────────────────────────────────────────

export interface AiChurnResult {
  customerId: string;
  churnScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  signals: string[];
  recommendation: string;
  reasoning: string;
}

// ─── Marketing Personalisation ───────────────────────────────────────────────

export interface AiMarketingResult {
  customerId: string;
  offerType: string;
  discountPercent: number;
  messageBody: string;
  subjectLine: string;
  urgencyHours: number;
}

// ─── Sentiment Analysis ──────────────────────────────────────────────────────

export interface AiSentimentResult {
  reviewId: string;
  sentimentScore: number;
  sentimentLabel: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
  themes: string[];
  reasoning: string;
}

export interface AiBatchSentimentResult {
  results: AiSentimentResult[];
  failedIds: string[];
}

// ─── Product Description ─────────────────────────────────────────────────────

export interface AiDescriptionResult {
  productId: string;
  description: string;
  shortDescription: string;
  saved: boolean;
}

// ─── Demand Forecast ─────────────────────────────────────────────────────────

export interface AiDemandForecastResult {
  productId: string;
  forecastDays: number;
  predictedDemand: number;
  reorderPoint: number;
  safetyStock: number;
  confidenceLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  reasoning: string;
  signals?: string[];
}

// ─── Pricing Recommendation ──────────────────────────────────────────────────

export interface AiPricingResult {
  productId: string;
  recommendedPrice: number;
  minAcceptablePrice: number;
  maxAcceptablePrice: number;
  expectedMarginPercent: number;
  rationale: string;
  signals?: string[];
}

// ─── Inventory Optimisation ──────────────────────────────────────────────────

export interface AiInventoryResult {
  productId: string;
  warehouseId?: string;
  recommendation: 'RESTOCK' | 'REDUCE' | 'HOLD';
  urgencyLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  recommendedQuantity: number;
  reasoning: string;
}

// ─── Budget Anomaly ──────────────────────────────────────────────────────────

export interface AiBudgetAnomalyResult {
  budgetId: string;
  anomalyScore: number;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  recommendation: 'MONITOR' | 'INVESTIGATE' | 'ESCALATE';
  anomalousLines: string[];
  reasoning: string;
}

// ─── SEO Optimiser ───────────────────────────────────────────────────────────

export interface AiSeoResult {
  productId: string;
  seoTitle: string;
  seoDescription: string;
  keywords: string[];
  reasoning: string;
  saved: boolean;
}

// ─── Return Pattern ──────────────────────────────────────────────────────────

export interface AiReturnPatternResult {
  productId: string;
  lookbackDays: number;
  returnRate: number;
  suspectedCauses: string[];
  recommendations: string[];
  reasoning: string;
  returnScore?: number;
  riskLevel?: string;
}

// ─── Vendor Analysis ─────────────────────────────────────────────────────────

export interface AiVendorResult {
  vendorId: string;
  score: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  strengths: string[];
  weaknesses: string[];
  recommendation: string;
  reasoning: string;
}

// ─── Shipping Optimisation ───────────────────────────────────────────────────

export interface AiShippingResult {
  orderId: string;
  carrier: string;
  serviceLevel: string;
  estimatedDays: number;
  estimatedCostUsd: number;
  signals: string[];
  reasoning: string;
}

// ─── Chatbot ─────────────────────────────────────────────────────────────────

export interface AiChatRequest {
  message: string;
  sessionId?: string;
}

export interface AiChatResponse {
  sessionId: string;
  message: string;   // matches Java AiChatResponse record field name
  turnCount: number;
}

export interface AiConversationMessage {
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
}

export interface AiChatSession {
  sessionId: string;
  messages: AiConversationMessage[];
  createdAt: string;
}

// ─── AI Insights ─────────────────────────────────────────────────────────────

export interface AiInsight {
  id: string;
  featureType: AiFeatureType;
  entityId: string;
  entityType: string;
  score: number | null;
  riskLevel: string | null;
  recommendation: string | null;
  reasoning: string | null;
  signals: string | null;
  status: string;
  reviewStatus: string;
  modelUsed: string | null;
  tokensInput: number | null;
  tokensOutput: number | null;
  estimatedCostUsd: number | null;
  latencyMs: number | null;
  createdAt: string;
}

export interface AiInsightsPage {
  content: AiInsight[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

// ─── NL Report ───────────────────────────────────────────────────────────────

export interface AiNlReportRequest {
  message: string;
  sessionId?: string;
  rawData?: string;
}

export interface AiNlReportResponse {
  sessionId: string;
  message: string;   // matches Java AiChatResponse record field name
  turnCount: number;
}

export interface AiNlSessionSummary {
  id: string;
  preview: string;
  turnCount: number;
  lastActiveAt: string;
  createdAt: string;
}

export interface AiNlSessionTurn {
  userMessage: string;
  assistantMessage: string;
}

export interface AiNlSessionDetail {
  id: string;
  turnCount: number;
  turns: AiNlSessionTurn[];
  createdAt: string;
}
