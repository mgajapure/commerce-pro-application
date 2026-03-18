// src/app/core/services/order.service.ts
// Order service - Angular HTTP Client integration with Spring Boot backend
// Follows the exact same pattern as product.service.ts

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import {
  OrderResponse,
  OrderSummaryResponse,
  OrderStatsResponse,
  CreateOrderRequest,
  UpdateOrderRequest,
  HoldOrderRequest,
  CancelOrderRequest,
  TrackingUpdateRequest,
  BulkOrderActionRequest,
  OrderStatus
} from '../../models/order/order.model';

import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

// ─── API config — mirrors product.service.ts structure ───────────────────────

const API_CONFIG = {
  baseUrl: 'http://localhost:8080/api',
  endpoints: {
    orders:     '/v1/orders',
    stats:      '/v1/orders/stats'
  }
};

@Injectable({ providedIn: 'root' })
export class OrderService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = API_CONFIG.baseUrl;

  // ─── Reactive state (signals) ─────────────────────────────────────────────

  private _loading = signal(false);
  private _error   = signal<string | null>(null);
  private _stats   = signal<OrderStatsResponse | null>(null);

  readonly isLoading    = computed(() => this._loading());
  readonly currentError = computed(() => this._error());
  readonly orderStats   = computed(() => this._stats());

  // ─── List / Search ────────────────────────────────────────────────────────

  /**
   * GET /api/v1/orders
   * Supports filter params + pagination
   */
  getOrders(
    filter?: {
      search?:        string;
      status?:        string;
      paymentStatus?: string;
      source?:        string;
      isFlagged?:     boolean;
      createdFrom?:   string;
      createdTo?:     string;
    },
    pageParams?: PageParams
  ): Observable<PageResponse<OrderSummaryResponse>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });

    if (filter) {
      if (filter.search)        params = params.set('search',        filter.search);
      if (filter.status)        params = params.set('status',        filter.status);
      if (filter.paymentStatus) params = params.set('paymentStatus', filter.paymentStatus);
      if (filter.source)        params = params.set('source',        filter.source);
      if (filter.isFlagged !== undefined)
                                params = params.set('isFlagged',     String(filter.isFlagged));
      if (filter.createdFrom)   params = params.set('createdFrom',   filter.createdFrom);
      if (filter.createdTo)     params = params.set('createdTo',     filter.createdTo);
    }

    return this.http
      .get<ApiResponse<PageResponse<OrderSummaryResponse>>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}`, { params }
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<PageResponse<OrderSummaryResponse>>(
          'getOrders',
          { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }
        ))
      );
  }

  /**
   * GET /api/v1/orders/{id}
   */
  getOrderById(id: string): Observable<OrderResponse> {
    return this.http
      .get<ApiResponse<OrderResponse>>(`${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('getOrderById'))
      );
  }

  /**
   * GET /api/v1/orders/number/{orderNumber}
   */
  getOrderByNumber(orderNumber: string): Observable<OrderResponse> {
    return this.http
      .get<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/number/${orderNumber}`
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('getOrderByNumber'))
      );
  }

  /**
   * GET /api/v1/orders/customer/{customerId}
   */
  getOrdersByCustomer(
    customerId: string,
    pageParams?: PageParams
  ): Observable<PageResponse<OrderSummaryResponse>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<OrderSummaryResponse>>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/customer/${customerId}`, { params }
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<PageResponse<OrderSummaryResponse>>(
          'getOrdersByCustomer',
          { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }
        ))
      );
  }

  /**
   * GET /api/v1/orders/stats
   */
  getStats(): Observable<OrderStatsResponse> {
    return this.http
      .get<ApiResponse<OrderStatsResponse>>(`${this.baseUrl}${API_CONFIG.endpoints.stats}`)
      .pipe(
        map(r => r.data),
        tap(stats => this._stats.set(stats)),
        catchError(this.handleError<OrderStatsResponse>('getStats', {
          totalOrders: 0, ordersToday: 0, pendingOrders: 0, processingOrders: 0,
          confirmedOrders: 0, shippedOrders: 0, deliveredOrders: 0, cancelledOrders: 0,
          flaggedOrders: 0, onHoldOrders: 0, totalRevenue: 0, revenueToday: 0,
          averageOrderValue: 0, statusBreakdown: {}
        }))
      );
  }

  // ─── Mutations ────────────────────────────────────────────────────────────

  /**
   * POST /api/v1/orders
   */
  createOrder(request: CreateOrderRequest): Observable<OrderResponse> {
    return this.http
      .post<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}`, request
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('createOrder'))
      );
  }

  /**
   * PUT /api/v1/orders/{id}
   */
  updateOrder(id: string, request: UpdateOrderRequest): Observable<OrderResponse> {
    return this.http
      .put<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}`, request
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('updateOrder'))
      );
  }

  /**
   * POST /api/v1/orders/{id}/confirm
   */
  confirmOrder(id: string): Observable<OrderResponse> {
    return this.http
      .post<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}/confirm`, {}
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('confirmOrder'))
      );
  }

  /**
   * POST /api/v1/orders/{id}/process
   */
  markProcessing(id: string): Observable<OrderResponse> {
    return this.http
      .post<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}/process`, {}
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('markProcessing'))
      );
  }

  /**
   * POST /api/v1/orders/{id}/ship
   */
  markShipped(id: string, trackingNumber: string, carrier: string): Observable<OrderResponse> {
    const body: TrackingUpdateRequest = { trackingNumber, carrier };
    return this.http
      .post<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}/ship`, body
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('markShipped'))
      );
  }

  /**
   * POST /api/v1/orders/{id}/deliver
   */
  markDelivered(id: string): Observable<OrderResponse> {
    return this.http
      .post<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}/deliver`, {}
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('markDelivered'))
      );
  }

  /**
   * POST /api/v1/orders/{id}/hold
   */
  holdOrder(id: string, reason: string): Observable<OrderResponse> {
    const body: HoldOrderRequest = { reason };
    return this.http
      .post<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}/hold`, body
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('holdOrder'))
      );
  }

  /**
   * POST /api/v1/orders/{id}/release-hold
   */
  releaseHold(id: string): Observable<OrderResponse> {
    return this.http
      .post<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}/release-hold`, {}
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('releaseHold'))
      );
  }

  /**
   * POST /api/v1/orders/{id}/close
   */
  closeOrder(id: string): Observable<OrderResponse> {
    return this.http
      .post<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}/close`, {}
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('closeOrder'))
      );
  }

  /**
   * DELETE /api/v1/orders/{id}   (cancel)
   * Body carries the cancellation reason
   */
  cancelOrder(id: string, reason: string): Observable<OrderResponse> {
    const body: CancelOrderRequest = { reason };
    return this.http
      .request<ApiResponse<OrderResponse>>(
        'DELETE',
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}`,
        { body }
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('cancelOrder'))
      );
  }

  /**
   * PATCH /api/v1/orders/{id}/tracking
   */
  updateTracking(id: string, trackingNumber: string, carrier: string): Observable<OrderResponse> {
    const body: TrackingUpdateRequest = { trackingNumber, carrier };
    return this.http
      .patch<ApiResponse<OrderResponse>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/${id}/tracking`, body
      )
      .pipe(
        map(r => r.data),
        catchError(this.handleError<OrderResponse>('updateTracking'))
      );
  }

  /**
   * POST /api/v1/orders/bulk-action
   */
  bulkAction(
    orderIds: string[],
    targetStatus: OrderStatus,
    reason?: string
  ): Observable<string[]> {
    const body: BulkOrderActionRequest = { orderIds, targetStatus, reason };
    return this.http
      .post<ApiResponse<string[]>>(
        `${this.baseUrl}${API_CONFIG.endpoints.orders}/bulk-action`, body
      )
      .pipe(
        map(r => r.data ?? []),
        catchError(this.handleError<string[]>('bulkAction', []))
      );
  }

  // ─── Error handler — mirrors product.service.ts ───────────────────────────

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[OrderService] ${operation} failed:`, error);
      this._error.set(
        error?.error?.message ?? error?.message ?? 'An unexpected error occurred'
      );
      return of(result as T);
    };
  }
}
