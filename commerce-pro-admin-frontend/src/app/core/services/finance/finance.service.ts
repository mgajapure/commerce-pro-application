import { API_HOST } from '../../config/api-config';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiResponse, PageResponse } from '../../models/common';
import { buildPageParams, PageParams } from '../../models/common';
import {
  FinancialPeriodDTO, CreatePeriodRequest,
  VendorDTO, VendorSummaryDTO, CreateVendorRequest,
  CustomerInvoiceDTO, CustomerInvoiceSummaryDTO, ARSummaryDTO, CreateCustomerInvoiceRequest, RecordPaymentRequest,
  VendorInvoiceDTO, VendorInvoiceSummaryDTO, APSummaryDTO, CreateVendorInvoiceRequest,
  TaxRateDTO, TaxJurisdictionDTO, TaxReportDTO, CreateTaxRateRequest,
  ExpenseCategoryDTO, ExpenseDTO, ExpenseSummaryDTO, ExpenseStatsDTO, CreateExpenseRequest,
  JournalEntryDTO, JournalEntrySummaryDTO, CreateJournalEntryRequest,
  BudgetDTO, CreateBudgetRequest,
  RevenueOverviewDTO, ProfitLossDTO,
  CashFlowForecastDTO,
  ExchangeRateDTO, ConversionResultDTO, AddExchangeRateRequest,
  InvoiceStatus, VendorStatus, VendorInvoiceStatus, ExpenseStatus, JournalEntryType,
} from '../../models/finance/finance.model';

const BASE = `${API_HOST}/api/v1/finance`;
const empty = <T>(): PageResponse<T> => ({ content:[], page:0, size:20, totalElements:0, totalPages:0, first:true, last:true, empty:true });

@Injectable({ providedIn: 'root' })
export class FinanceService {

  private http = inject(HttpClient);

  // ── Revenue / P&L ──────────────────────────────────────────────────────────

  getRevenueOverview(): Observable<RevenueOverviewDTO> {
    return this.http.get<ApiResponse<RevenueOverviewDTO>>(`${BASE}/revenue/overview`)
      .pipe(map(r => r.data), catchError(this.err('getOverview', {} as RevenueOverviewDTO)));
  }

  getPnL(from: string, to: string, label?: string): Observable<ProfitLossDTO> {
    let p = new HttpParams().set('from', from).set('to', to);
    if (label) p = p.set('label', label);
    return this.http.get<ApiResponse<ProfitLossDTO>>(`${BASE}/revenue/pnl`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getPnL', {} as ProfitLossDTO)));
  }

  getPnLYTD(): Observable<ProfitLossDTO> {
    return this.http.get<ApiResponse<ProfitLossDTO>>(`${BASE}/revenue/pnl/ytd`)
      .pipe(map(r => r.data), catchError(this.err('getPnLYTD', {} as ProfitLossDTO)));
  }

  getPnLMTD(): Observable<ProfitLossDTO> {
    return this.http.get<ApiResponse<ProfitLossDTO>>(`${BASE}/revenue/pnl/mtd`)
      .pipe(map(r => r.data), catchError(this.err('getPnLMTD', {} as ProfitLossDTO)));
  }

  // ── Cash Flow ──────────────────────────────────────────────────────────────

  getCashFlowForecast(): Observable<CashFlowForecastDTO> {
    return this.http.get<ApiResponse<CashFlowForecastDTO>>(`${BASE}/cashflow/forecast`)
      .pipe(map(r => r.data), catchError(this.err('getCashFlow', {} as CashFlowForecastDTO)));
  }

  // ── Financial Periods ──────────────────────────────────────────────────────

  getPeriods(pageParams?: PageParams): Observable<PageResponse<FinancialPeriodDTO>> {
    const p = new HttpParams({ fromObject: buildPageParams(pageParams ?? { size: 24 }) });
    return this.http.get<ApiResponse<PageResponse<FinancialPeriodDTO>>>(`${BASE}/periods`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getPeriods', empty<FinancialPeriodDTO>())));
  }

  getCurrentPeriod(): Observable<FinancialPeriodDTO> {
    return this.http.get<ApiResponse<FinancialPeriodDTO>>(`${BASE}/periods/current`)
      .pipe(map(r => r.data), catchError(this.err('getCurrentPeriod', null as any)));
  }

  getPeriodById(id: string): Observable<FinancialPeriodDTO> {
    return this.http.get<ApiResponse<FinancialPeriodDTO>>(`${BASE}/periods/${id}`)
      .pipe(map(r => r.data), catchError(this.err('getPeriodById', null as any)));
  }

  getPeriodsByYear(year: number): Observable<FinancialPeriodDTO[]> {
    return this.http.get<ApiResponse<FinancialPeriodDTO[]>>(`${BASE}/periods/year/${year}`)
      .pipe(map(r => r.data ?? []), catchError(this.err('getPeriodsByYear', [])));
  }

  createPeriod(req: CreatePeriodRequest): Observable<FinancialPeriodDTO> {
    return this.http.post<ApiResponse<FinancialPeriodDTO>>(`${BASE}/periods`, req)
      .pipe(map(r => r.data), catchError(this.err('createPeriod', null as any)));
  }

  closePeriod(id: string, notes?: string): Observable<FinancialPeriodDTO> {
    return this.http.post<ApiResponse<FinancialPeriodDTO>>(`${BASE}/periods/${id}/close`, { notes })
      .pipe(map(r => r.data), catchError(this.err('closePeriod', null as any)));
  }

  lockPeriod(id: string, notes?: string): Observable<FinancialPeriodDTO> {
    return this.http.post<ApiResponse<FinancialPeriodDTO>>(`${BASE}/periods/${id}/lock`, { notes })
      .pipe(map(r => r.data), catchError(this.err('lockPeriod', null as any)));
  }

  reopenPeriod(id: string, notes?: string): Observable<FinancialPeriodDTO> {
    return this.http.post<ApiResponse<FinancialPeriodDTO>>(`${BASE}/periods/${id}/reopen`, { notes })
      .pipe(map(r => r.data), catchError(this.err('reopenPeriod', null as any)));
  }

  // ── Vendors ────────────────────────────────────────────────────────────────

  getVendors(search?: string, status?: VendorStatus, category?: string, pageParams?: PageParams): Observable<PageResponse<VendorSummaryDTO>> {
    let p = new HttpParams({ fromObject: buildPageParams(pageParams ?? { size: 20 }) });
    if (search)   p = p.set('search', search);
    if (status)   p = p.set('status', status);
    if (category) p = p.set('category', category);
    return this.http.get<ApiResponse<PageResponse<VendorSummaryDTO>>>(`${BASE}/vendors`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getVendors', empty<VendorSummaryDTO>())));
  }

  getVendorById(id: string): Observable<VendorDTO> {
    return this.http.get<ApiResponse<VendorDTO>>(`${BASE}/vendors/${id}`)
      .pipe(map(r => r.data), catchError(this.err('getVendorById', null as any)));
  }

  createVendor(req: CreateVendorRequest): Observable<VendorDTO> {
    return this.http.post<ApiResponse<VendorDTO>>(`${BASE}/vendors`, req)
      .pipe(map(r => r.data), catchError(this.err('createVendor', null as any)));
  }

  updateVendor(id: string, req: Partial<CreateVendorRequest>): Observable<VendorDTO> {
    return this.http.put<ApiResponse<VendorDTO>>(`${BASE}/vendors/${id}`, req)
      .pipe(map(r => r.data), catchError(this.err('updateVendor', null as any)));
  }

  changeVendorStatus(id: string, status: VendorStatus): Observable<VendorDTO> {
    return this.http.patch<ApiResponse<VendorDTO>>(`${BASE}/vendors/${id}/status?status=${status}`, {})
      .pipe(map(r => r.data), catchError(this.err('changeVendorStatus', null as any)));
  }

  // ── Customer Invoices (AR) ─────────────────────────────────────────────────

  getCustomerInvoices(filter?: { search?: string; status?: InvoiceStatus; customerId?: string; periodId?: string; from?: string; to?: string; overdue?: boolean }, pageParams?: PageParams): Observable<PageResponse<CustomerInvoiceSummaryDTO>> {
    let p = new HttpParams({ fromObject: buildPageParams(pageParams ?? { size: 20, sort: 'createdAt', direction: 'desc' }) });
    if (filter?.search)     p = p.set('search', filter.search);
    if (filter?.status)     p = p.set('status', filter.status);
    if (filter?.customerId) p = p.set('customerId', filter.customerId);
    if (filter?.periodId)   p = p.set('periodId', filter.periodId);
    if (filter?.from)       p = p.set('from', filter.from);
    if (filter?.to)         p = p.set('to', filter.to);
    if (filter?.overdue !== undefined) p = p.set('overdue', String(filter.overdue));
    return this.http.get<ApiResponse<PageResponse<CustomerInvoiceSummaryDTO>>>(`${BASE}/invoices/customer`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getCustomerInvoices', empty<CustomerInvoiceSummaryDTO>())));
  }

  getCustomerInvoiceById(id: string): Observable<CustomerInvoiceDTO> {
    return this.http.get<ApiResponse<CustomerInvoiceDTO>>(`${BASE}/invoices/customer/${id}`)
      .pipe(map(r => r.data), catchError(this.err('getCustomerInvoiceById', null as any)));
  }

  getARSummary(): Observable<ARSummaryDTO> {
    return this.http.get<ApiResponse<ARSummaryDTO>>(`${BASE}/invoices/customer/ar/summary`)
      .pipe(map(r => r.data), catchError(this.err('getARSummary', {} as ARSummaryDTO)));
  }

  createCustomerInvoice(req: CreateCustomerInvoiceRequest): Observable<CustomerInvoiceDTO> {
    return this.http.post<ApiResponse<CustomerInvoiceDTO>>(`${BASE}/invoices/customer`, req)
      .pipe(map(r => r.data), catchError(this.err('createCustomerInvoice', null as any)));
  }

  sendInvoice(id: string): Observable<CustomerInvoiceDTO> {
    return this.http.post<ApiResponse<CustomerInvoiceDTO>>(`${BASE}/invoices/customer/${id}/send`, {})
      .pipe(map(r => r.data), catchError(this.err('sendInvoice', null as any)));
  }

  recordInvoicePayment(id: string, req: RecordPaymentRequest): Observable<CustomerInvoiceDTO> {
    return this.http.post<ApiResponse<CustomerInvoiceDTO>>(`${BASE}/invoices/customer/${id}/record-payment`, req)
      .pipe(map(r => r.data), catchError(this.err('recordPayment', null as any)));
  }

  voidInvoice(id: string, reason: string): Observable<CustomerInvoiceDTO> {
    return this.http.post<ApiResponse<CustomerInvoiceDTO>>(`${BASE}/invoices/customer/${id}/void`, { reason })
      .pipe(map(r => r.data), catchError(this.err('voidInvoice', null as any)));
  }

  writeOffInvoice(id: string, reason: string): Observable<CustomerInvoiceDTO> {
    return this.http.post<ApiResponse<CustomerInvoiceDTO>>(`${BASE}/invoices/customer/${id}/write-off`, { reason })
      .pipe(map(r => r.data), catchError(this.err('writeOffInvoice', null as any)));
  }

  // ── Vendor Invoices (AP) ───────────────────────────────────────────────────

  getVendorInvoices(filter?: { search?: string; status?: VendorInvoiceStatus; vendorId?: string; from?: string; to?: string }, pageParams?: PageParams): Observable<PageResponse<VendorInvoiceSummaryDTO>> {
    let p = new HttpParams({ fromObject: buildPageParams(pageParams ?? { size: 20, sort: 'dueDate', direction: 'asc' }) });
    if (filter?.search)   p = p.set('search', filter.search);
    if (filter?.status)   p = p.set('status', filter.status);
    if (filter?.vendorId) p = p.set('vendorId', filter.vendorId);
    if (filter?.from)     p = p.set('from', filter.from);
    if (filter?.to)       p = p.set('to', filter.to);
    return this.http.get<ApiResponse<PageResponse<VendorInvoiceSummaryDTO>>>(`${BASE}/invoices/vendor`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getVendorInvoices', empty<VendorInvoiceSummaryDTO>())));
  }

  getVendorInvoiceById(id: string): Observable<VendorInvoiceDTO> {
    return this.http.get<ApiResponse<VendorInvoiceDTO>>(`${BASE}/invoices/vendor/${id}`)
      .pipe(map(r => r.data), catchError(this.err('getVendorInvoiceById', null as any)));
  }

  getAPSummary(): Observable<APSummaryDTO> {
    return this.http.get<ApiResponse<APSummaryDTO>>(`${BASE}/invoices/vendor/ap/summary`)
      .pipe(map(r => r.data), catchError(this.err('getAPSummary', {} as APSummaryDTO)));
  }

  getDueSoonInvoices(days = 14): Observable<VendorInvoiceSummaryDTO[]> {
    return this.http.get<ApiResponse<VendorInvoiceSummaryDTO[]>>(`${BASE}/invoices/vendor/ap/due-soon?days=${days}`)
      .pipe(map(r => r.data ?? []), catchError(this.err('getDueSoon', [])));
  }

  createVendorInvoice(req: CreateVendorInvoiceRequest): Observable<VendorInvoiceDTO> {
    return this.http.post<ApiResponse<VendorInvoiceDTO>>(`${BASE}/invoices/vendor`, req)
      .pipe(map(r => r.data), catchError(this.err('createVendorInvoice', null as any)));
  }

  approveVendorInvoice(id: string): Observable<VendorInvoiceDTO> {
    return this.http.post<ApiResponse<VendorInvoiceDTO>>(`${BASE}/invoices/vendor/${id}/approve`, {})
      .pipe(map(r => r.data), catchError(this.err('approveVendorInvoice', null as any)));
  }

  rejectVendorInvoice(id: string, reason: string): Observable<VendorInvoiceDTO> {
    return this.http.post<ApiResponse<VendorInvoiceDTO>>(`${BASE}/invoices/vendor/${id}/reject`, { reason })
      .pipe(map(r => r.data), catchError(this.err('rejectVendorInvoice', null as any)));
  }

  scheduleVendorPayment(id: string, date: string): Observable<VendorInvoiceDTO> {
    return this.http.post<ApiResponse<VendorInvoiceDTO>>(`${BASE}/invoices/vendor/${id}/schedule-payment`, { scheduledPaymentDate: date })
      .pipe(map(r => r.data), catchError(this.err('schedulePayment', null as any)));
  }

  recordVendorPayment(id: string, req: RecordPaymentRequest): Observable<VendorInvoiceDTO> {
    return this.http.post<ApiResponse<VendorInvoiceDTO>>(`${BASE}/invoices/vendor/${id}/record-payment`, req)
      .pipe(map(r => r.data), catchError(this.err('recordVendorPayment', null as any)));
  }

  // ── Tax ────────────────────────────────────────────────────────────────────

  getTaxRates(activeOnly = false): Observable<TaxRateDTO[]> {
    return this.http.get<ApiResponse<TaxRateDTO[]>>(`${BASE}/tax/rates?activeOnly=${activeOnly}`)
      .pipe(map(r => r.data ?? []), catchError(this.err('getTaxRates', [])));
  }

  createTaxRate(req: CreateTaxRateRequest): Observable<TaxRateDTO> {
    return this.http.post<ApiResponse<TaxRateDTO>>(`${BASE}/tax/rates`, req)
      .pipe(map(r => r.data), catchError(this.err('createTaxRate', null as any)));
  }

  activateTaxRate(id: string): Observable<TaxRateDTO> {
    return this.http.patch<ApiResponse<TaxRateDTO>>(`${BASE}/tax/rates/${id}/activate`, {})
      .pipe(map(r => r.data), catchError(this.err('activateTaxRate', null as any)));
  }

  deactivateTaxRate(id: string): Observable<TaxRateDTO> {
    return this.http.patch<ApiResponse<TaxRateDTO>>(`${BASE}/tax/rates/${id}/deactivate`, {})
      .pipe(map(r => r.data), catchError(this.err('deactivateTaxRate', null as any)));
  }

  getJurisdictions(): Observable<TaxJurisdictionDTO[]> {
    return this.http.get<ApiResponse<TaxJurisdictionDTO[]>>(`${BASE}/tax/jurisdictions`)
      .pipe(map(r => r.data ?? []), catchError(this.err('getJurisdictions', [])));
  }

  getTaxReports(pageParams?: PageParams): Observable<PageResponse<TaxReportDTO>> {
    const p = new HttpParams({ fromObject: buildPageParams(pageParams ?? { size: 20 }) });
    return this.http.get<ApiResponse<PageResponse<TaxReportDTO>>>(`${BASE}/tax/reports`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getTaxReports', empty<TaxReportDTO>())));
  }

  generateTaxReport(periodId: string, reportType: string, jurisdictionId?: string): Observable<TaxReportDTO> {
    return this.http.post<ApiResponse<TaxReportDTO>>(`${BASE}/tax/reports/generate`, { periodId, reportType, jurisdictionId })
      .pipe(map(r => r.data), catchError(this.err('generateTaxReport', null as any)));
  }

  fileTaxReport(id: string): Observable<TaxReportDTO> {
    return this.http.post<ApiResponse<TaxReportDTO>>(`${BASE}/tax/reports/${id}/file`, {})
      .pipe(map(r => r.data), catchError(this.err('fileTaxReport', null as any)));
  }

  // ── Expenses ───────────────────────────────────────────────────────────────

  getExpenses(filter?: { search?: string; status?: ExpenseStatus; categoryId?: string; from?: string; to?: string }, pageParams?: PageParams): Observable<PageResponse<ExpenseSummaryDTO>> {
    let p = new HttpParams({ fromObject: buildPageParams(pageParams ?? { size: 20 }) });
    if (filter?.search)     p = p.set('search', filter.search);
    if (filter?.status)     p = p.set('status', filter.status);
    if (filter?.categoryId) p = p.set('categoryId', filter.categoryId);
    if (filter?.from)       p = p.set('from', filter.from);
    if (filter?.to)         p = p.set('to', filter.to);
    return this.http.get<ApiResponse<PageResponse<ExpenseSummaryDTO>>>(`${BASE}/expenses`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getExpenses', empty<ExpenseSummaryDTO>())));
  }

  getExpenseById(id: string): Observable<ExpenseDTO> {
    return this.http.get<ApiResponse<ExpenseDTO>>(`${BASE}/expenses/${id}`)
      .pipe(map(r => r.data), catchError(this.err('getExpenseById', null as any)));
  }

  getExpenseStats(): Observable<ExpenseStatsDTO> {
    return this.http.get<ApiResponse<ExpenseStatsDTO>>(`${BASE}/expenses/stats`)
      .pipe(map(r => r.data), catchError(this.err('getExpenseStats', {} as ExpenseStatsDTO)));
  }

  getExpenseCategories(): Observable<ExpenseCategoryDTO[]> {
    return this.http.get<ApiResponse<ExpenseCategoryDTO[]>>(`${BASE}/expenses/categories`)
      .pipe(map(r => r.data ?? []), catchError(this.err('getCategories', [])));
  }

  createExpense(req: CreateExpenseRequest): Observable<ExpenseDTO> {
    return this.http.post<ApiResponse<ExpenseDTO>>(`${BASE}/expenses`, req)
      .pipe(map(r => r.data), catchError(this.err('createExpense', null as any)));
  }

  approveExpense(id: string): Observable<ExpenseDTO> {
    return this.http.post<ApiResponse<ExpenseDTO>>(`${BASE}/expenses/${id}/approve`, {})
      .pipe(map(r => r.data), catchError(this.err('approveExpense', null as any)));
  }

  rejectExpense(id: string, reason: string): Observable<ExpenseDTO> {
    return this.http.post<ApiResponse<ExpenseDTO>>(`${BASE}/expenses/${id}/reject`, { reason })
      .pipe(map(r => r.data), catchError(this.err('rejectExpense', null as any)));
  }

  markExpensePaid(id: string): Observable<ExpenseDTO> {
    return this.http.post<ApiResponse<ExpenseDTO>>(`${BASE}/expenses/${id}/mark-paid`, {})
      .pipe(map(r => r.data), catchError(this.err('markExpensePaid', null as any)));
  }

  createExpenseCategory(req: { categoryCode: string; name: string; glAccountCode?: string }): Observable<ExpenseCategoryDTO> {
    return this.http.post<ApiResponse<ExpenseCategoryDTO>>(`${BASE}/expenses/categories`, req)
      .pipe(map(r => r.data), catchError(this.err('createCategory', null as any)));
  }

  // ── Journal ────────────────────────────────────────────────────────────────

  getJournalEntries(filter?: { search?: string; entryType?: JournalEntryType; periodId?: string; status?: string; from?: string; to?: string }, pageParams?: PageParams): Observable<PageResponse<JournalEntrySummaryDTO>> {
    let p = new HttpParams({ fromObject: buildPageParams(pageParams ?? { size: 20 }) });
    if (filter?.search)    p = p.set('search', filter.search);
    if (filter?.entryType) p = p.set('entryType', filter.entryType);
    if (filter?.periodId)  p = p.set('periodId', filter.periodId);
    if (filter?.status)    p = p.set('status', filter.status);
    if (filter?.from)      p = p.set('from', filter.from);
    if (filter?.to)        p = p.set('to', filter.to);
    return this.http.get<ApiResponse<PageResponse<JournalEntrySummaryDTO>>>(`${BASE}/journal`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getJournalEntries', empty<JournalEntrySummaryDTO>())));
  }

  getJournalEntryById(id: string): Observable<JournalEntryDTO> {
    return this.http.get<ApiResponse<JournalEntryDTO>>(`${BASE}/journal/${id}`)
      .pipe(map(r => r.data), catchError(this.err('getJournalEntryById', null as any)));
  }

  postJournalEntry(req: CreateJournalEntryRequest): Observable<JournalEntryDTO> {
    return this.http.post<ApiResponse<JournalEntryDTO>>(`${BASE}/journal`, req)
      .pipe(map(r => r.data), catchError(this.err('postJournalEntry', null as any)));
  }

  reverseJournalEntry(id: string, reason: string): Observable<JournalEntryDTO> {
    return this.http.post<ApiResponse<JournalEntryDTO>>(`${BASE}/journal/${id}/reverse`, { reason })
      .pipe(map(r => r.data), catchError(this.err('reverseJournalEntry', null as any)));
  }

  // ── Budgets ────────────────────────────────────────────────────────────────

  getBudgets(fiscalYear?: number, pageParams?: PageParams): Observable<PageResponse<BudgetDTO>> {
    let p = new HttpParams({ fromObject: buildPageParams(pageParams ?? { size: 20 }) });
    if (fiscalYear) p = p.set('fiscalYear', String(fiscalYear));
    return this.http.get<ApiResponse<PageResponse<BudgetDTO>>>(`${BASE}/budgets`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('getBudgets', empty<BudgetDTO>())));
  }

  getBudgetById(id: string): Observable<BudgetDTO> {
    return this.http.get<ApiResponse<BudgetDTO>>(`${BASE}/budgets/${id}`)
      .pipe(map(r => r.data), catchError(this.err('getBudgetById', null as any)));
  }

  getBudgetVariance(id: string): Observable<BudgetDTO> {
    return this.http.get<ApiResponse<BudgetDTO>>(`${BASE}/budgets/${id}/variance`)
      .pipe(map(r => r.data), catchError(this.err('getBudgetVariance', null as any)));
  }

  createBudget(req: CreateBudgetRequest): Observable<BudgetDTO> {
    return this.http.post<ApiResponse<BudgetDTO>>(`${BASE}/budgets`, req)
      .pipe(map(r => r.data), catchError(this.err('createBudget', null as any)));
  }

  approveBudget(id: string): Observable<BudgetDTO> {
    return this.http.post<ApiResponse<BudgetDTO>>(`${BASE}/budgets/${id}/approve`, {})
      .pipe(map(r => r.data), catchError(this.err('approveBudget', null as any)));
  }

  // ── Exchange Rates ─────────────────────────────────────────────────────────

  getExchangeRates(): Observable<ExchangeRateDTO[]> {
    return this.http.get<ApiResponse<ExchangeRateDTO[]>>(`${BASE}/exchange-rates`)
      .pipe(map(r => r.data ?? []), catchError(this.err('getExchangeRates', [])));
  }

  convertCurrency(from: string, to: string, amount: number): Observable<ConversionResultDTO> {
    const p = new HttpParams().set('from', from).set('to', to).set('amount', String(amount));
    return this.http.get<ApiResponse<ConversionResultDTO>>(`${BASE}/exchange-rates/convert`, { params: p })
      .pipe(map(r => r.data), catchError(this.err('convertCurrency', null as any)));
  }

  addExchangeRate(req: AddExchangeRateRequest): Observable<ExchangeRateDTO> {
    return this.http.post<ApiResponse<ExchangeRateDTO>>(`${BASE}/exchange-rates`, req)
      .pipe(map(r => r.data), catchError(this.err('addExchangeRate', null as any)));
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private err<T>(op: string, fallback: T) {
    return (e: any): import('rxjs').Observable<T> => {
      console.error(`[FinanceService.${op}]`, e?.error?.message ?? e?.message ?? e);
      return of(fallback);
    };
  }
}
