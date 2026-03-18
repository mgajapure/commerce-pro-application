// src/app/core/services/fulfillment/fulfillment.service.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  FulfillmentQueueItem, FulfillmentStats,
  PickList, PickListSummary,
  CreatePickListRequest, AssignPickListRequest, UpdatePickItemRequest, PickListItem,
  Carrier, CarrierRequest,
  ShippingRule, ShippingRuleRequest
} from '../../models/fulfillment/fulfillment.model';
import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

const BASE = 'http://localhost:8080/api/v1/fulfillment';

@Injectable({ providedIn: 'root' })
export class FulfillmentService {
  private http = inject(HttpClient);

  private _loading = signal(false);
  private _error   = signal<string | null>(null);
  readonly isLoading    = computed(() => this._loading());
  readonly currentError = computed(() => this._error());

  // ── Queue ──────────────────────────────────────────────────────────────────
  getQueue(): Observable<FulfillmentQueueItem[]> {
    return this.http.get<ApiResponse<FulfillmentQueueItem[]>>(`${BASE}/queue`)
      .pipe(map(r => r.data), catchError(this.handleError('getQueue', [])));
  }

  getStats(): Observable<FulfillmentStats> {
    return this.http.get<ApiResponse<FulfillmentStats>>(`${BASE}/stats`)
      .pipe(map(r => r.data), catchError(this.handleError('getStats', {} as FulfillmentStats)));
  }

  // ── Pick Lists ─────────────────────────────────────────────────────────────
  getPickLists(filter?: any, pageParams?: PageParams): Observable<PageResponse<PickListSummary>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    if (filter) {
      if (filter.search)          params = params.set('search',          filter.search);
      if (filter.status)          params = params.set('status',          filter.status);
      if (filter.type)            params = params.set('type',            filter.type);
      if (filter.assignedToUserId)params = params.set('assignedToUserId',filter.assignedToUserId);
      if (filter.createdFrom)     params = params.set('createdFrom',     filter.createdFrom);
      if (filter.createdTo)       params = params.set('createdTo',       filter.createdTo);
    }
    return this.http.get<ApiResponse<PageResponse<PickListSummary>>>(`${BASE}/pick-lists`, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getPickLists',
        { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true })));
  }

  getPickList(id: string): Observable<PickList> {
    return this.http.get<ApiResponse<PickList>>(`${BASE}/pick-lists/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<PickList>('getPickList')));
  }

  generatePickList(req: CreatePickListRequest): Observable<PickList> {
    return this.http.post<ApiResponse<PickList>>(`${BASE}/pick-lists`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PickList>('generatePickList')));
  }

  assignPickList(id: string, req: AssignPickListRequest): Observable<PickList> {
    return this.http.post<ApiResponse<PickList>>(`${BASE}/pick-lists/${id}/assign`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PickList>('assignPickList')));
  }

  startPickList(id: string): Observable<PickList> {
    return this.http.post<ApiResponse<PickList>>(`${BASE}/pick-lists/${id}/start`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PickList>('startPickList')));
  }

  updatePickItem(pickListId: string, itemId: string, req: UpdatePickItemRequest): Observable<PickListItem> {
    return this.http.patch<ApiResponse<PickListItem>>(`${BASE}/pick-lists/${pickListId}/items/${itemId}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PickListItem>('updatePickItem')));
  }

  completePickList(id: string): Observable<PickList> {
    return this.http.post<ApiResponse<PickList>>(`${BASE}/pick-lists/${id}/complete`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PickList>('completePickList')));
  }

  cancelPickList(id: string): Observable<PickList> {
    return this.http.post<ApiResponse<PickList>>(`${BASE}/pick-lists/${id}/cancel`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PickList>('cancelPickList')));
  }

  // ── Carriers ───────────────────────────────────────────────────────────────
  getCarriers(): Observable<Carrier[]> {
    return this.http.get<ApiResponse<Carrier[]>>(`${BASE}/carriers`)
      .pipe(map(r => r.data), catchError(this.handleError('getCarriers', [])));
  }

  createCarrier(req: CarrierRequest): Observable<Carrier> {
    return this.http.post<ApiResponse<Carrier>>(`${BASE}/carriers`, req)
      .pipe(map(r => r.data), catchError(this.handleError<Carrier>('createCarrier')));
  }

  updateCarrier(id: string, req: CarrierRequest): Observable<Carrier> {
    return this.http.put<ApiResponse<Carrier>>(`${BASE}/carriers/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<Carrier>('updateCarrier')));
  }

  deleteCarrier(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/carriers/${id}`)
      .pipe(map(() => void 0), catchError(this.handleError<void>('deleteCarrier')));
  }

  // ── Shipping Rules ─────────────────────────────────────────────────────────
  getRules(): Observable<ShippingRule[]> {
    return this.http.get<ApiResponse<ShippingRule[]>>(`${BASE}/shipping-rules`)
      .pipe(map(r => r.data), catchError(this.handleError('getRules', [])));
  }

  createRule(req: ShippingRuleRequest): Observable<ShippingRule> {
    return this.http.post<ApiResponse<ShippingRule>>(`${BASE}/shipping-rules`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ShippingRule>('createRule')));
  }

  updateRule(id: string, req: ShippingRuleRequest): Observable<ShippingRule> {
    return this.http.put<ApiResponse<ShippingRule>>(`${BASE}/shipping-rules/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ShippingRule>('updateRule')));
  }

  deleteRule(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/shipping-rules/${id}`)
      .pipe(map(() => void 0), catchError(this.handleError<void>('deleteRule')));
  }

  private handleError<T>(op = 'op', result?: T) {
    return (err: any): Observable<T> => {
      console.error(`[FulfillmentService] ${op}:`, err);
      this._error.set(err?.error?.message ?? err?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
