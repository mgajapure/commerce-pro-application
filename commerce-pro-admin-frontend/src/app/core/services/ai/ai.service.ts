import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  AiConfigResponse, AiConfigUpdateRequest, AiUsageSummaryItem, AiDashboardResponse,
  AiFraudResult, AiChurnResult, AiMarketingResult,
  AiSentimentResult, AiBatchSentimentResult,
  AiDescriptionResult, AiDemandForecastResult,
  AiPricingResult, AiInventoryResult, AiBudgetAnomalyResult,
  AiSeoResult, AiReturnPatternResult, AiVendorResult, AiShippingResult,
  AiChatRequest, AiChatResponse, AiChatSession,
  AiNlReportRequest, AiNlReportResponse, AiNlSessionSummary, AiNlSessionDetail,
  AiFeatureType, AiInsightsPage
} from '../../models/ai/ai.model';

const BASE = 'http://localhost:8080/api/v1';

/** Unwraps the standard ApiResponse<T> envelope `{ success, data, timestamp }`. */
type Api<T> = { data: T };

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);

  // ─── Admin ────────────────────────────────────────────────────────────────

  getDashboard(): Observable<AiDashboardResponse> {
    return this.http.get<Api<AiDashboardResponse>>(`${BASE}/admin/ai/dashboard`).pipe(map(r => r.data));
  }

  getUsage(from?: string, to?: string): Observable<AiUsageSummaryItem[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<Api<AiUsageSummaryItem[]>>(`${BASE}/admin/ai/usage`, { params }).pipe(map(r => r.data));
  }

  getConfigs(): Observable<AiConfigResponse[]> {
    return this.http.get<Api<AiConfigResponse[]>>(`${BASE}/admin/ai/configs`).pipe(map(r => r.data));
  }

  getConfig(feature: string): Observable<AiConfigResponse> {
    return this.http.get<Api<AiConfigResponse>>(`${BASE}/admin/ai/configs/${feature}`).pipe(map(r => r.data));
  }

  updateConfig(feature: string, req: AiConfigUpdateRequest): Observable<AiConfigResponse> {
    return this.http.patch<Api<AiConfigResponse>>(`${BASE}/admin/ai/configs/${feature}`, req).pipe(map(r => r.data));
  }

  // ─── Fraud Detection ──────────────────────────────────────────────────────

  analyseTransaction(transactionId: string): Observable<AiFraudResult> {
    return this.http.post<Api<AiFraudResult>>(`${BASE}/ai/fraud/${transactionId}`, {}).pipe(map(r => r.data));
  }

  // ─── Churn Prediction ─────────────────────────────────────────────────────

  predictChurn(customerId: string): Observable<AiChurnResult> {
    return this.http.post<Api<AiChurnResult>>(`${BASE}/ai/churn/${customerId}`, {}).pipe(map(r => r.data));
  }

  // ─── Marketing Personalisation ────────────────────────────────────────────

  personaliseMarketing(customerId: string): Observable<AiMarketingResult> {
    return this.http.post<Api<AiMarketingResult>>(`${BASE}/ai/marketing/customers/${customerId}/personalise`, {}).pipe(map(r => r.data));
  }

  // ─── Sentiment Analysis ───────────────────────────────────────────────────

  analyseReviewSentiment(reviewId: string): Observable<AiSentimentResult> {
    return this.http.post<Api<AiSentimentResult>>(`${BASE}/ai/sentiment/reviews/${reviewId}`, {}).pipe(map(r => r.data));
  }

  batchAnalyseSentiment(reviewIds: string[]): Observable<AiBatchSentimentResult> {
    return this.http.post<Api<AiBatchSentimentResult>>(`${BASE}/ai/sentiment/reviews/batch`, { reviewIds }).pipe(map(r => r.data));
  }

  // ─── Product Description ──────────────────────────────────────────────────

  generateDescription(productId: string): Observable<AiDescriptionResult> {
    return this.http.post<Api<AiDescriptionResult>>(`${BASE}/ai/products/${productId}/description/generate`, {}).pipe(map(r => r.data));
  }

  previewDescription(productId: string): Observable<AiDescriptionResult> {
    return this.http.post<Api<AiDescriptionResult>>(`${BASE}/ai/products/${productId}/description/preview`, {}).pipe(map(r => r.data));
  }

  // ─── Demand Forecast ──────────────────────────────────────────────────────

  forecastDemand(productId: string, days = 30): Observable<AiDemandForecastResult> {
    const params = new HttpParams().set('days', days);
    return this.http.post<Api<AiDemandForecastResult>>(`${BASE}/ai/forecast/products/${productId}`, {}, { params }).pipe(map(r => r.data));
  }

  // ─── Pricing Recommendation ───────────────────────────────────────────────

  recommendPricing(productId: string): Observable<AiPricingResult> {
    return this.http.post<Api<AiPricingResult>>(`${BASE}/ai/pricing/products/${productId}`, {}).pipe(map(r => r.data));
  }

  // ─── Inventory Optimisation ───────────────────────────────────────────────

  optimiseInventory(productId: string, warehouseId?: string): Observable<AiInventoryResult> {
    let params = new HttpParams();
    if (warehouseId) params = params.set('warehouseId', warehouseId);
    return this.http.post<Api<AiInventoryResult>>(`${BASE}/ai/inventory/products/${productId}`, {}, { params }).pipe(map(r => r.data));
  }

  // ─── Budget Anomaly ───────────────────────────────────────────────────────

  analyseBudgetAnomaly(budgetId: string): Observable<AiBudgetAnomalyResult> {
    return this.http.post<Api<AiBudgetAnomalyResult>>(`${BASE}/ai/budget-anomaly/budgets/${budgetId}`, {}).pipe(map(r => r.data));
  }

  // ─── SEO ──────────────────────────────────────────────────────────────────

  optimiseSeo(productId: string): Observable<AiSeoResult> {
    return this.http.post<Api<AiSeoResult>>(`${BASE}/ai/seo/products/${productId}/optimise`, {}).pipe(map(r => r.data));
  }

  previewSeo(productId: string): Observable<AiSeoResult> {
    return this.http.post<Api<AiSeoResult>>(`${BASE}/ai/seo/products/${productId}/preview`, {}).pipe(map(r => r.data));
  }

  // ─── Return Pattern ───────────────────────────────────────────────────────

  analyseReturnPattern(productId: string, days = 90): Observable<AiReturnPatternResult> {
    const params = new HttpParams().set('days', days);
    return this.http.post<Api<AiReturnPatternResult>>(`${BASE}/ai/returns/products/${productId}`, {}, { params }).pipe(map(r => r.data));
  }

  // ─── Vendor Analysis ──────────────────────────────────────────────────────

  analyseVendor(vendorId: string): Observable<AiVendorResult> {
    return this.http.post<Api<AiVendorResult>>(`${BASE}/ai/vendors/${vendorId}/analyse`, {}).pipe(map(r => r.data));
  }

  // ─── Shipping Optimisation ────────────────────────────────────────────────

  optimiseShipping(orderId: string): Observable<AiShippingResult> {
    return this.http.post<Api<AiShippingResult>>(`${BASE}/ai/shipping/orders/${orderId}/optimise`, {}).pipe(map(r => r.data));
  }

  // ─── Support Chatbot ──────────────────────────────────────────────────────

  chatbot(req: AiChatRequest): Observable<AiChatResponse> {
    return this.http.post<Api<AiChatResponse>>(`${BASE}/ai/chatbot/chat`, req).pipe(map(r => r.data));
  }

  getChatbotSession(sessionId: string): Observable<AiChatSession> {
    return this.http.get<Api<AiChatSession>>(`${BASE}/ai/chatbot/sessions/${sessionId}`).pipe(map(r => r.data));
  }

  // ─── NL Report ────────────────────────────────────────────────────────────

  nlReportChat(req: AiNlReportRequest): Observable<AiNlReportResponse> {
    return this.http.post<Api<AiNlReportResponse>>(`${BASE}/ai/nl-report/chat`, req).pipe(map(r => r.data));
  }

  getNlReportSessions(): Observable<AiNlSessionSummary[]> {
    return this.http.get<Api<AiNlSessionSummary[]>>(`${BASE}/ai/nl-report/sessions`).pipe(map(r => r.data));
  }

  getNlReportSession(sessionId: string): Observable<AiNlSessionDetail> {
    return this.http.get<Api<AiNlSessionDetail>>(`${BASE}/ai/nl-report/sessions/${sessionId}`).pipe(map(r => r.data));
  }

  // ─── AI Insights ──────────────────────────────────────────────────────────

  getInsights(opts: {
    feature?: AiFeatureType;
    riskLevel?: string;
    entityType?: string;
    since?: string;
    page?: number;
    size?: number;
  } = {}): Observable<AiInsightsPage> {
    let params = new HttpParams();
    if (opts.feature)      params = params.set('feature',    opts.feature);
    if (opts.riskLevel)    params = params.set('riskLevel',  opts.riskLevel);
    if (opts.entityType)   params = params.set('entityType', opts.entityType);
    if (opts.since)        params = params.set('since',      opts.since);
    if (opts.page != null) params = params.set('page',       opts.page);
    if (opts.size != null) params = params.set('size',       opts.size);
    return this.http.get<Api<AiInsightsPage>>(`${BASE}/admin/ai/insights`, { params }).pipe(map(r => r.data));
  }
}
