import { API_HOST } from '../../config/api-config';
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import {
  PaymentTransactionDTO, PaymentTransactionSummaryDTO, PaymentStatsDTO,
  RefundRequestDTO, RefundRequestSummaryDTO,
  ChargebackDisputeDTO, ChargebackSummaryDTO,
  ReconciliationReportDTO, ReconciliationSummaryDTO,
  PayoutDTO, PayoutSummaryDTO,
  PaymentLinkDTO, PaymentLinkSummaryDTO,
  PaymentMethodDTO, PaymentGatewayConfigDTO,
  CreatePaymentRequest, CreateRefundRequest, ReviewRefundRequest,
  CreateChargebackRequest, SubmitEvidenceRequest, ResolveChargebackRequest,
  GenerateReconciliationRequest, CreatePayoutRequest,
  CreatePaymentLinkRequest, CreateGatewayConfigRequest,
  TransactionStatus, TransactionType, GatewayProvider,
  RefundStatus, ChargebackStatus, PayoutStatus, PaymentLinkStatus
} from '../../models/payment/payment.model';

import { ApiResponse, PageResponse } from '../../models/common';
import { buildPageParams, PageParams } from '../../models/common';

const BASE = `${API_HOST}/api/v1/payments`;

const EMPTY_PAGE = <T>(): PageResponse<T> => ({
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true
});

@Injectable({ providedIn: 'root' })
export class PaymentService {

  private http = inject(HttpClient);

  private _loading = signal(false);
  private _error   = signal<string | null>(null);

  readonly isLoading    = computed(() => this._loading());
  readonly currentError = computed(() => this._error());

  // ── Transactions ──────────────────────────────────────────────────────────

  getTransactions(filter?: {
    search?: string; status?: TransactionStatus; type?: TransactionType;
    gateway?: GatewayProvider; orderId?: string; customerId?: string;
    isFlagged?: boolean; from?: string; to?: string;
  }, pageParams?: PageParams): Observable<PageResponse<PaymentTransactionSummaryDTO>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    if (filter?.search)    params = params.set('search', filter.search);
    if (filter?.status)    params = params.set('status', filter.status);
    if (filter?.type)      params = params.set('type', filter.type);
    if (filter?.gateway)   params = params.set('gateway', filter.gateway);
    if (filter?.orderId)   params = params.set('orderId', filter.orderId);
    if (filter?.customerId)params = params.set('customerId', filter.customerId);
    if (filter?.isFlagged !== undefined) params = params.set('isFlagged', String(filter.isFlagged));
    if (filter?.from)      params = params.set('from', filter.from);
    if (filter?.to)        params = params.set('to', filter.to);
    return this.http.get<ApiResponse<PageResponse<PaymentTransactionSummaryDTO>>>(`${BASE}/transactions`, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getTransactions', EMPTY_PAGE<PaymentTransactionSummaryDTO>())));
  }

  getTransactionById(id: string): Observable<PaymentTransactionDTO> {
    return this.http.get<ApiResponse<PaymentTransactionDTO>>(`${BASE}/transactions/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<PaymentTransactionDTO>('getTransactionById')));
  }

  getTransactionsByOrder(orderId: string, pageParams?: PageParams): Observable<PageResponse<PaymentTransactionSummaryDTO>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http.get<ApiResponse<PageResponse<PaymentTransactionSummaryDTO>>>(`${BASE}/transactions/order/${orderId}`, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getTransactionsByOrder', EMPTY_PAGE<PaymentTransactionSummaryDTO>())));
  }

  getPaymentStats(): Observable<PaymentStatsDTO> {
    return this.http.get<ApiResponse<PaymentStatsDTO>>(`${BASE}/transactions/stats`)
      .pipe(map(r => r.data), catchError(this.handleError<PaymentStatsDTO>('getStats', {
        totalTransactions: 0, transactionsToday: 0, totalRevenue: 0, revenueToday: 0,
        pendingCaptureAmount: 0, pendingRefunds: 0, pendingRefundAmount: 0,
        activeDisputes: 0, disputedAmount: 0, activePaymentLinks: 0, nextPayoutAmount: 0
      })));
  }

  createPayment(req: CreatePaymentRequest): Observable<PaymentTransactionDTO> {
    return this.http.post<ApiResponse<PaymentTransactionDTO>>(`${BASE}/transactions`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PaymentTransactionDTO>('createPayment')));
  }

  captureTransaction(id: string, amount?: number): Observable<PaymentTransactionDTO> {
    const params = amount ? new HttpParams().set('amount', String(amount)) : new HttpParams();
    return this.http.post<ApiResponse<PaymentTransactionDTO>>(`${BASE}/transactions/${id}/capture`, {}, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PaymentTransactionDTO>('capture')));
  }

  voidTransaction(id: string): Observable<PaymentTransactionDTO> {
    return this.http.post<ApiResponse<PaymentTransactionDTO>>(`${BASE}/transactions/${id}/void`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PaymentTransactionDTO>('void')));
  }

  flagTransaction(id: string, flagged: boolean, reason?: string): Observable<PaymentTransactionDTO> {
    return this.http.patch<ApiResponse<PaymentTransactionDTO>>(`${BASE}/transactions/${id}/flag`, { flagged, reason })
      .pipe(map(r => r.data), catchError(this.handleError<PaymentTransactionDTO>('flag')));
  }

  // ── Pending Payments ──────────────────────────────────────────────────────

  getPendingPayments(pageParams?: PageParams): Observable<PaymentTransactionSummaryDTO[]> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http.get<ApiResponse<PaymentTransactionSummaryDTO[]>>(`${BASE}/pending`, { params })
      .pipe(map(r => r.data ?? []), catchError(this.handleError('getPending', [])));
  }

  getExpiringAuthorizations(): Observable<PaymentTransactionSummaryDTO[]> {
    return this.http.get<ApiResponse<PaymentTransactionSummaryDTO[]>>(`${BASE}/pending/expiring`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError('getExpiring', [])));
  }

  capturePending(id: string, amount?: number): Observable<PaymentTransactionDTO> {
    return this.captureTransaction(id, amount);
  }

  voidPending(id: string): Observable<PaymentTransactionDTO> {
    return this.voidTransaction(id);
  }

  // ── Refunds ───────────────────────────────────────────────────────────────

  getRefunds(status?: RefundStatus, orderId?: string, pageParams?: PageParams): Observable<PageResponse<RefundRequestSummaryDTO>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    if (status)  params = params.set('status', status);
    if (orderId) params = params.set('orderId', orderId);
    return this.http.get<ApiResponse<PageResponse<RefundRequestSummaryDTO>>>(`${BASE}/refunds`, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getRefunds', EMPTY_PAGE<RefundRequestSummaryDTO>())));
  }

  getRefundById(id: string): Observable<RefundRequestDTO> {
    return this.http.get<ApiResponse<RefundRequestDTO>>(`${BASE}/refunds/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<RefundRequestDTO>('getRefundById')));
  }

  getRefundsByOrder(orderId: string): Observable<RefundRequestSummaryDTO[]> {
    return this.http.get<ApiResponse<RefundRequestSummaryDTO[]>>(`${BASE}/refunds/order/${orderId}`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError('getRefundsByOrder', [])));
  }

  createRefund(req: CreateRefundRequest): Observable<RefundRequestDTO> {
    return this.http.post<ApiResponse<RefundRequestDTO>>(`${BASE}/refunds`, req)
      .pipe(map(r => r.data), catchError(this.handleError<RefundRequestDTO>('createRefund')));
  }

  approveRefund(id: string, notes?: string): Observable<RefundRequestDTO> {
    return this.http.post<ApiResponse<RefundRequestDTO>>(`${BASE}/refunds/${id}/approve`, { notes })
      .pipe(map(r => r.data), catchError(this.handleError<RefundRequestDTO>('approveRefund')));
  }

  rejectRefund(id: string, notes?: string): Observable<RefundRequestDTO> {
    return this.http.post<ApiResponse<RefundRequestDTO>>(`${BASE}/refunds/${id}/reject`, { notes })
      .pipe(map(r => r.data), catchError(this.handleError<RefundRequestDTO>('rejectRefund')));
  }

  processRefund(id: string): Observable<RefundRequestDTO> {
    return this.http.post<ApiResponse<RefundRequestDTO>>(`${BASE}/refunds/${id}/process`, {})
      .pipe(map(r => r.data), catchError(this.handleError<RefundRequestDTO>('processRefund')));
  }

  // ── Chargebacks ───────────────────────────────────────────────────────────

  getChargebacks(status?: ChargebackStatus, orderId?: string, pageParams?: PageParams): Observable<PageResponse<ChargebackSummaryDTO>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    if (status)  params = params.set('status', status);
    if (orderId) params = params.set('orderId', orderId);
    return this.http.get<ApiResponse<PageResponse<ChargebackSummaryDTO>>>(`${BASE}/chargebacks`, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getChargebacks', EMPTY_PAGE<ChargebackSummaryDTO>())));
  }

  getChargebackById(id: string): Observable<ChargebackDisputeDTO> {
    return this.http.get<ApiResponse<ChargebackDisputeDTO>>(`${BASE}/chargebacks/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<ChargebackDisputeDTO>('getChargebackById')));
  }

  getDeadlineAlerts(): Observable<ChargebackSummaryDTO[]> {
    return this.http.get<ApiResponse<ChargebackSummaryDTO[]>>(`${BASE}/chargebacks/deadline-alerts`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError('getDeadlineAlerts', [])));
  }

  intakeChargeback(req: CreateChargebackRequest): Observable<ChargebackDisputeDTO> {
    return this.http.post<ApiResponse<ChargebackDisputeDTO>>(`${BASE}/chargebacks`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ChargebackDisputeDTO>('intakeChargeback')));
  }

  submitEvidence(id: string, req: SubmitEvidenceRequest): Observable<ChargebackDisputeDTO> {
    return this.http.post<ApiResponse<ChargebackDisputeDTO>>(`${BASE}/chargebacks/${id}/submit-evidence`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ChargebackDisputeDTO>('submitEvidence')));
  }

  acceptChargeback(id: string, notes?: string): Observable<ChargebackDisputeDTO> {
    return this.http.post<ApiResponse<ChargebackDisputeDTO>>(`${BASE}/chargebacks/${id}/accept`, { notes })
      .pipe(map(r => r.data), catchError(this.handleError<ChargebackDisputeDTO>('acceptChargeback')));
  }

  resolveChargeback(id: string, req: ResolveChargebackRequest): Observable<ChargebackDisputeDTO> {
    return this.http.post<ApiResponse<ChargebackDisputeDTO>>(`${BASE}/chargebacks/${id}/resolve`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ChargebackDisputeDTO>('resolveChargeback')));
  }

  // ── Reconciliation ────────────────────────────────────────────────────────

  getReconciliationReports(pageParams?: PageParams): Observable<PageResponse<ReconciliationSummaryDTO>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http.get<ApiResponse<PageResponse<ReconciliationSummaryDTO>>>(`${BASE}/reconciliation`, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getReconReports', EMPTY_PAGE<ReconciliationSummaryDTO>())));
  }

  getReconciliationById(id: string): Observable<ReconciliationReportDTO> {
    return this.http.get<ApiResponse<ReconciliationReportDTO>>(`${BASE}/reconciliation/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<ReconciliationReportDTO>('getReconById')));
  }

  generateReconciliation(req: GenerateReconciliationRequest): Observable<ReconciliationReportDTO> {
    return this.http.post<ApiResponse<ReconciliationReportDTO>>(`${BASE}/reconciliation/generate`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ReconciliationReportDTO>('generateRecon')));
  }

  closeReconciliation(id: string): Observable<ReconciliationReportDTO> {
    return this.http.post<ApiResponse<ReconciliationReportDTO>>(`${BASE}/reconciliation/${id}/close`, {})
      .pipe(map(r => r.data), catchError(this.handleError<ReconciliationReportDTO>('closeRecon')));
  }

  // ── Payouts ───────────────────────────────────────────────────────────────

  getPayouts(status?: PayoutStatus, pageParams?: PageParams): Observable<PageResponse<PayoutSummaryDTO>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResponse<PayoutSummaryDTO>>>(`${BASE}/payouts`, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getPayouts', EMPTY_PAGE<PayoutSummaryDTO>())));
  }

  getPayoutById(id: string): Observable<PayoutDTO> {
    return this.http.get<ApiResponse<PayoutDTO>>(`${BASE}/payouts/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<PayoutDTO>('getPayoutById')));
  }

  createPayout(req: CreatePayoutRequest): Observable<PayoutDTO> {
    return this.http.post<ApiResponse<PayoutDTO>>(`${BASE}/payouts`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PayoutDTO>('createPayout')));
  }

  approvePayout(id: string): Observable<PayoutDTO> {
    return this.http.post<ApiResponse<PayoutDTO>>(`${BASE}/payouts/${id}/approve`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PayoutDTO>('approvePayout')));
  }

  initiatePayout(id: string): Observable<PayoutDTO> {
    return this.http.post<ApiResponse<PayoutDTO>>(`${BASE}/payouts/${id}/initiate`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PayoutDTO>('initiatePayout')));
  }

  completePayout(id: string): Observable<PayoutDTO> {
    return this.http.post<ApiResponse<PayoutDTO>>(`${BASE}/payouts/${id}/complete`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PayoutDTO>('completePayout')));
  }

  cancelPayout(id: string, reason: string): Observable<PayoutDTO> {
    return this.http.post<ApiResponse<PayoutDTO>>(`${BASE}/payouts/${id}/cancel`, { reason })
      .pipe(map(r => r.data), catchError(this.handleError<PayoutDTO>('cancelPayout')));
  }

  // ── Payment Links ─────────────────────────────────────────────────────────

  getPaymentLinks(status?: PaymentLinkStatus, pageParams?: PageParams): Observable<PageResponse<PaymentLinkSummaryDTO>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResponse<PaymentLinkSummaryDTO>>>(`${BASE}/links`, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getLinks', EMPTY_PAGE<PaymentLinkSummaryDTO>())));
  }

  getPaymentLinkById(id: string): Observable<PaymentLinkDTO> {
    return this.http.get<ApiResponse<PaymentLinkDTO>>(`${BASE}/links/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<PaymentLinkDTO>('getLinkById')));
  }

  createPaymentLink(req: CreatePaymentLinkRequest): Observable<PaymentLinkDTO> {
    return this.http.post<ApiResponse<PaymentLinkDTO>>(`${BASE}/links`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PaymentLinkDTO>('createLink')));
  }

  deactivatePaymentLink(id: string): Observable<PaymentLinkDTO> {
    return this.http.post<ApiResponse<PaymentLinkDTO>>(`${BASE}/links/${id}/deactivate`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PaymentLinkDTO>('deactivateLink')));
  }

  // ── Payment Methods ───────────────────────────────────────────────────────

  getPaymentMethodsByCustomer(customerId: string): Observable<PaymentMethodDTO[]> {
    return this.http.get<ApiResponse<PaymentMethodDTO[]>>(`${BASE}/methods/customer/${customerId}`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError('getMethods', [])));
  }

  removePaymentMethod(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/methods/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('removeMethod')));
  }

  setDefaultPaymentMethod(id: string): Observable<PaymentMethodDTO> {
    return this.http.post<ApiResponse<PaymentMethodDTO>>(`${BASE}/methods/${id}/set-default`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PaymentMethodDTO>('setDefault')));
  }

  // ── Gateways ──────────────────────────────────────────────────────────────

  getGateways(): Observable<PaymentGatewayConfigDTO[]> {
    return this.http.get<ApiResponse<PaymentGatewayConfigDTO[]>>(`${BASE}/gateways`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError('getGateways', [])));
  }

  createGatewayConfig(req: CreateGatewayConfigRequest): Observable<PaymentGatewayConfigDTO> {
    return this.http.post<ApiResponse<PaymentGatewayConfigDTO>>(`${BASE}/gateways`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PaymentGatewayConfigDTO>('createGateway')));
  }

  activateGateway(id: string): Observable<PaymentGatewayConfigDTO> {
    return this.http.post<ApiResponse<PaymentGatewayConfigDTO>>(`${BASE}/gateways/${id}/activate`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PaymentGatewayConfigDTO>('activateGateway')));
  }

  setDefaultGateway(id: string): Observable<PaymentGatewayConfigDTO> {
    return this.http.post<ApiResponse<PaymentGatewayConfigDTO>>(`${BASE}/gateways/${id}/set-default`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PaymentGatewayConfigDTO>('setDefaultGateway')));
  }

  updateGatewayConfig(id: string, req: Partial<CreateGatewayConfigRequest>): Observable<PaymentGatewayConfigDTO> {
    return this.http.put<ApiResponse<PaymentGatewayConfigDTO>>(`${BASE}/gateways/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PaymentGatewayConfigDTO>('updateGateway')));
  }

  // ── Error handler ─────────────────────────────────────────────────────────

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[PaymentService] ${operation} failed:`, error);
      this._error.set(error?.error?.message ?? error?.message ?? 'An unexpected error occurred');
      return of(result as T);
    };
  }
}
