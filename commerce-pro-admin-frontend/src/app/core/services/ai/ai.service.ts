import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  AiConfigResponse, AiConfigUpdateRequest, AiUsageSummaryItem, AiDashboardResponse,
  AiFraudResult, AiChurnResult, AiMarketingResult,
  AiSentimentResult, AiBatchSentimentResult,
  AiDescriptionResult, AiDemandForecastResult,
  AiPricingResult, AiInventoryResult, AiBudgetAnomalyResult,
  AiSeoResult, AiReturnPatternResult, AiVendorResult, AiShippingResult,
  AiChatRequest, AiChatResponse, AiChatSession,
  AiNlReportRequest, AiNlReportResponse,
  AiFeatureType, AiInsightsPage
} from '../../models/ai/ai.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);

  // ─── Admin ────────────────────────────────────────────────────────────────

  getDashboard(): Observable<AiDashboardResponse> {
    return this.http.get<AiDashboardResponse>(`${BASE}/admin/ai/dashboard`);
  }

  getUsage(from?: string, to?: string): Observable<AiUsageSummaryItem[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<AiUsageSummaryItem[]>(`${BASE}/admin/ai/usage`, { params });
  }

  getConfigs(): Observable<AiConfigResponse[]> {
    return this.http.get<AiConfigResponse[]>(`${BASE}/admin/ai/configs`);
  }

  getConfig(feature: string): Observable<AiConfigResponse> {
    return this.http.get<AiConfigResponse>(`${BASE}/admin/ai/configs/${feature}`);
  }

  updateConfig(feature: string, req: AiConfigUpdateRequest): Observable<AiConfigResponse> {
    return this.http.patch<AiConfigResponse>(`${BASE}/admin/ai/configs/${feature}`, req);
  }

  // ─── Fraud Detection ──────────────────────────────────────────────────────

  analyseTransaction(transactionId: string): Observable<AiFraudResult> {
    return this.http.post<AiFraudResult>(`${BASE}/ai/fraud/${transactionId}`, {});
  }

  // ─── Churn Prediction ─────────────────────────────────────────────────────

  predictChurn(customerId: string): Observable<AiChurnResult> {
    return this.http.post<AiChurnResult>(`${BASE}/ai/churn/${customerId}`, {});
  }

  // ─── Marketing Personalisation ────────────────────────────────────────────

  personaliseMarketing(customerId: string): Observable<AiMarketingResult> {
    return this.http.post<AiMarketingResult>(`${BASE}/ai/marketing/customers/${customerId}/personalise`, {});
  }

  // ─── Sentiment Analysis ───────────────────────────────────────────────────

  analyseReviewSentiment(reviewId: string): Observable<AiSentimentResult> {
    return this.http.post<AiSentimentResult>(`${BASE}/ai/sentiment/reviews/${reviewId}`, {});
  }

  batchAnalyseSentiment(reviewIds: string[]): Observable<AiBatchSentimentResult> {
    return this.http.post<AiBatchSentimentResult>(`${BASE}/ai/sentiment/reviews/batch`, { reviewIds });
  }

  // ─── Product Description ──────────────────────────────────────────────────

  generateDescription(productId: string): Observable<AiDescriptionResult> {
    return this.http.post<AiDescriptionResult>(`${BASE}/ai/products/${productId}/description/generate`, {});
  }

  previewDescription(productId: string): Observable<AiDescriptionResult> {
    return this.http.post<AiDescriptionResult>(`${BASE}/ai/products/${productId}/description/preview`, {});
  }

  // ─── Demand Forecast ──────────────────────────────────────────────────────

  forecastDemand(productId: string, days = 30): Observable<AiDemandForecastResult> {
    const params = new HttpParams().set('days', days);
    return this.http.post<AiDemandForecastResult>(`${BASE}/ai/forecast/products/${productId}`, {}, { params });
  }

  // ─── Pricing Recommendation ───────────────────────────────────────────────

  recommendPricing(productId: string): Observable<AiPricingResult> {
    return this.http.post<AiPricingResult>(`${BASE}/ai/pricing/products/${productId}`, {});
  }

  // ─── Inventory Optimisation ───────────────────────────────────────────────

  optimiseInventory(productId: string, warehouseId?: string): Observable<AiInventoryResult> {
    let params = new HttpParams();
    if (warehouseId) params = params.set('warehouseId', warehouseId);
    return this.http.post<AiInventoryResult>(`${BASE}/ai/inventory/products/${productId}`, {}, { params });
  }

  // ─── Budget Anomaly ───────────────────────────────────────────────────────

  analyseBudgetAnomaly(budgetId: string): Observable<AiBudgetAnomalyResult> {
    return this.http.post<AiBudgetAnomalyResult>(`${BASE}/ai/budget-anomaly/budgets/${budgetId}`, {});
  }

  // ─── SEO ──────────────────────────────────────────────────────────────────

  optimiseSeo(productId: string): Observable<AiSeoResult> {
    return this.http.post<AiSeoResult>(`${BASE}/ai/seo/products/${productId}/optimise`, {});
  }

  previewSeo(productId: string): Observable<AiSeoResult> {
    return this.http.post<AiSeoResult>(`${BASE}/ai/seo/products/${productId}/preview`, {});
  }

  // ─── Return Pattern ───────────────────────────────────────────────────────

  analyseReturnPattern(productId: string, days = 90): Observable<AiReturnPatternResult> {
    const params = new HttpParams().set('days', days);
    return this.http.post<AiReturnPatternResult>(`${BASE}/ai/returns/products/${productId}`, {}, { params });
  }

  // ─── Vendor Analysis ──────────────────────────────────────────────────────

  analyseVendor(vendorId: string): Observable<AiVendorResult> {
    return this.http.post<AiVendorResult>(`${BASE}/ai/vendors/${vendorId}/analyse`, {});
  }

  // ─── Shipping Optimisation ────────────────────────────────────────────────

  optimiseShipping(orderId: string): Observable<AiShippingResult> {
    return this.http.post<AiShippingResult>(`${BASE}/ai/shipping/orders/${orderId}/optimise`, {});
  }

  // ─── Support Chatbot ──────────────────────────────────────────────────────

  chatbot(req: AiChatRequest): Observable<AiChatResponse> {
    return this.http.post<AiChatResponse>(`${BASE}/ai/chatbot/chat`, req);
  }

  getChatbotSession(sessionId: string): Observable<AiChatSession> {
    return this.http.get<AiChatSession>(`${BASE}/ai/chatbot/sessions/${sessionId}`);
  }

  // ─── NL Report ────────────────────────────────────────────────────────────

  nlReportChat(req: AiNlReportRequest): Observable<AiNlReportResponse> {
    return this.http.post<AiNlReportResponse>(`${BASE}/ai/nl-report/chat`, req);
  }

  // ─── AI Insights ──────────────────────────────────────────────────────────

  getInsights(opts: {
    feature?: AiFeatureType;
    riskLevel?: string;
    entityType?: string;
    since?: string;
    page?: number;
    size?: number;
  } = {}): Observable<{ data: AiInsightsPage }> {
    let params = new HttpParams();
    if (opts.feature)    params = params.set('feature',    opts.feature);
    if (opts.riskLevel)  params = params.set('riskLevel',  opts.riskLevel);
    if (opts.entityType) params = params.set('entityType', opts.entityType);
    if (opts.since)      params = params.set('since',      opts.since);
    if (opts.page != null) params = params.set('page',     opts.page);
    if (opts.size != null) params = params.set('size',     opts.size);
    return this.http.get<{ data: AiInsightsPage }>(`${BASE}/admin/ai/insights`, { params });
  }
}
