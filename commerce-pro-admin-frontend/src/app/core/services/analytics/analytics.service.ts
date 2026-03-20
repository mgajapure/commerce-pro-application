// src/app/core/services/analytics/analytics.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  AnalyticsDashboard,
  SalesOverview,
  SalesReport,
  InventoryReport,
  OrderReport,
  CustomerReport,
  FinancialReport,
  ShippingReport,
  ReturnReport,
  SavedReportResponse,
  SavedReportRequest,
  ScheduledReportResponse,
  ScheduledReportRequest,
  ReportExecution,
  ReportFilter,
  ExportFormat
} from '../../models/analytics/analytics.model';

import { ApiResponse, PageResponse } from '../../models/common/api-response.model';

const BASE = 'http://localhost:8080/api/v1/analytics';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private http = inject(HttpClient);

  // ── Dashboard ─────────────────────────────────────────────────────────────

  getDashboard(): Observable<AnalyticsDashboard> {
    return this.http.get<ApiResponse<AnalyticsDashboard>>(`${BASE}/dashboard`)
      .pipe(map(r => r.data));
  }

  // ── Sales ─────────────────────────────────────────────────────────────────

  getSalesOverview(filter: ReportFilter): Observable<SalesOverview> {
    return this.http.post<ApiResponse<SalesOverview>>(`${BASE}/sales/overview`, filter)
      .pipe(map(r => r.data));
  }

  getSalesByDate(filter: ReportFilter): Observable<SalesReport> {
    return this.http.post<ApiResponse<SalesReport>>(`${BASE}/sales/by-date`, filter)
      .pipe(map(r => r.data));
  }

  getSalesByProduct(filter: ReportFilter): Observable<SalesReport> {
    return this.http.post<ApiResponse<SalesReport>>(`${BASE}/sales/by-product`, filter)
      .pipe(map(r => r.data));
  }

  getSalesByCategory(filter: ReportFilter): Observable<SalesReport> {
    return this.http.post<ApiResponse<SalesReport>>(`${BASE}/sales/by-category`, filter)
      .pipe(map(r => r.data));
  }

  getSalesByChannel(filter: ReportFilter): Observable<SalesReport> {
    return this.http.post<ApiResponse<SalesReport>>(`${BASE}/sales/by-channel`, filter)
      .pipe(map(r => r.data));
  }

  // ── Inventory ─────────────────────────────────────────────────────────────

  getInventoryStockValue(filter: ReportFilter): Observable<InventoryReport> {
    return this.http.post<ApiResponse<InventoryReport>>(`${BASE}/inventory/stock-value`, filter)
      .pipe(map(r => r.data));
  }

  getInventoryMovement(filter: ReportFilter): Observable<InventoryReport> {
    return this.http.post<ApiResponse<InventoryReport>>(`${BASE}/inventory/movement`, filter)
      .pipe(map(r => r.data));
  }

  getInventoryAgeing(filter: ReportFilter): Observable<InventoryReport> {
    return this.http.post<ApiResponse<InventoryReport>>(`${BASE}/inventory/ageing`, filter)
      .pipe(map(r => r.data));
  }

  // ── Orders ────────────────────────────────────────────────────────────────

  getOrderReport(filter: ReportFilter): Observable<OrderReport> {
    return this.http.post<ApiResponse<OrderReport>>(`${BASE}/orders/summary`, filter)
      .pipe(map(r => r.data));
  }

  // ── Customers ─────────────────────────────────────────────────────────────

  getCustomerLtv(filter: ReportFilter): Observable<CustomerReport> {
    return this.http.post<ApiResponse<CustomerReport>>(`${BASE}/customers/ltv`, filter)
      .pipe(map(r => r.data));
  }

  getCustomerCohort(filter: ReportFilter): Observable<CustomerReport> {
    return this.http.post<ApiResponse<CustomerReport>>(`${BASE}/customers/cohort`, filter)
      .pipe(map(r => r.data));
  }

  getCustomerChurn(filter: ReportFilter): Observable<CustomerReport> {
    return this.http.post<ApiResponse<CustomerReport>>(`${BASE}/customers/churn`, filter)
      .pipe(map(r => r.data));
  }

  // ── Financial ─────────────────────────────────────────────────────────────

  getFinancialReport(filter: ReportFilter): Observable<FinancialReport> {
    return this.http.post<ApiResponse<FinancialReport>>(`${BASE}/financial/summary`, filter)
      .pipe(map(r => r.data));
  }

  // ── Shipping ──────────────────────────────────────────────────────────────

  getShippingReport(filter: ReportFilter): Observable<ShippingReport> {
    return this.http.post<ApiResponse<ShippingReport>>(`${BASE}/shipping/summary`, filter)
      .pipe(map(r => r.data));
  }

  // ── Returns ───────────────────────────────────────────────────────────────

  getReturnReport(filter: ReportFilter): Observable<ReturnReport> {
    return this.http.post<ApiResponse<ReturnReport>>(`${BASE}/returns/summary`, filter)
      .pipe(map(r => r.data));
  }

  // ── Saved Reports ─────────────────────────────────────────────────────────

  listSavedReports(page = 0, size = 20): Observable<PageResponse<SavedReportResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<SavedReportResponse>>>(`${BASE}/reports/saved`, { params })
      .pipe(map(r => r.data));
  }

  getSavedReport(id: string): Observable<SavedReportResponse> {
    return this.http.get<ApiResponse<SavedReportResponse>>(`${BASE}/reports/saved/${id}`)
      .pipe(map(r => r.data));
  }

  createSavedReport(req: SavedReportRequest): Observable<SavedReportResponse> {
    return this.http.post<ApiResponse<SavedReportResponse>>(`${BASE}/reports/saved`, req)
      .pipe(map(r => r.data));
  }

  updateSavedReport(id: string, req: SavedReportRequest): Observable<SavedReportResponse> {
    return this.http.put<ApiResponse<SavedReportResponse>>(`${BASE}/reports/saved/${id}`, req)
      .pipe(map(r => r.data));
  }

  deleteSavedReport(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/reports/saved/${id}`)
      .pipe(map(() => void 0));
  }

  runSavedReport(id: string): Observable<ReportExecution> {
    return this.http.post<ApiResponse<ReportExecution>>(`${BASE}/reports/saved/${id}/run`, {})
      .pipe(map(r => r.data));
  }

  // ── Ad-hoc Run / Export ───────────────────────────────────────────────────

  runAdHoc(filter: ReportFilter): Observable<ReportExecution> {
    return this.http.post<ApiResponse<ReportExecution>>(`${BASE}/reports/run`, filter)
      .pipe(map(r => r.data));
  }

  // ── Scheduled Reports ─────────────────────────────────────────────────────

  listScheduledReports(page = 0, size = 20): Observable<PageResponse<ScheduledReportResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<ScheduledReportResponse>>>(`${BASE}/reports/scheduled`, { params })
      .pipe(map(r => r.data));
  }

  createScheduledReport(req: ScheduledReportRequest): Observable<ScheduledReportResponse> {
    return this.http.post<ApiResponse<ScheduledReportResponse>>(`${BASE}/reports/scheduled`, req)
      .pipe(map(r => r.data));
  }

  updateScheduledReport(id: string, req: ScheduledReportRequest): Observable<ScheduledReportResponse> {
    return this.http.put<ApiResponse<ScheduledReportResponse>>(`${BASE}/reports/scheduled/${id}`, req)
      .pipe(map(r => r.data));
  }

  deleteScheduledReport(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/reports/scheduled/${id}`)
      .pipe(map(() => void 0));
  }

  toggleScheduledReport(id: string): Observable<ScheduledReportResponse> {
    return this.http.post<ApiResponse<ScheduledReportResponse>>(`${BASE}/reports/scheduled/${id}/toggle`, {})
      .pipe(map(r => r.data));
  }

  runScheduledNow(id: string): Observable<ReportExecution> {
    return this.http.post<ApiResponse<ReportExecution>>(`${BASE}/reports/scheduled/${id}/run-now`, {})
      .pipe(map(r => r.data));
  }

  // ── Execution History ─────────────────────────────────────────────────────

  listExecutions(page = 0, size = 20): Observable<PageResponse<ReportExecution>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<ReportExecution>>>(`${BASE}/reports/executions`, { params })
      .pipe(map(r => r.data));
  }

  getExecution(id: string): Observable<ReportExecution> {
    return this.http.get<ApiResponse<ReportExecution>>(`${BASE}/reports/executions/${id}`)
      .pipe(map(r => r.data));
  }
}
