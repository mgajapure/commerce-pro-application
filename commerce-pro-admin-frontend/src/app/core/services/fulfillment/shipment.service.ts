// src/app/core/services/fulfillment/shipment.service.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  ShipmentDetail, ShipmentSummary, ShipmentStats,
  CreateShipmentRequest, AddTrackingEventRequest
} from '../../models/fulfillment/fulfillment.model';
import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

const BASE = 'http://localhost:8080/api/v1/shipments';

@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private http = inject(HttpClient);

  private _loading = signal(false);
  private _error   = signal<string | null>(null);
  readonly isLoading    = computed(() => this._loading());
  readonly currentError = computed(() => this._error());

  getShipments(filter?: any, pageParams?: PageParams): Observable<PageResponse<ShipmentSummary>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    if (filter) {
      if (filter.search)    params = params.set('search',    filter.search);
      if (filter.status)    params = params.set('status',    filter.status);
      if (filter.carrierId) params = params.set('carrierId', filter.carrierId);
      if (filter.orderId)   params = params.set('orderId',   filter.orderId);
      if (filter.createdFrom) params = params.set('createdFrom', filter.createdFrom);
      if (filter.createdTo)   params = params.set('createdTo',   filter.createdTo);
    }
    return this.http.get<ApiResponse<PageResponse<ShipmentSummary>>>(BASE, { params })
      .pipe(map(r => r.data), catchError(this.handleError('getShipments',
        { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true })));
  }

  getStats(): Observable<ShipmentStats> {
    return this.http.get<ApiResponse<ShipmentStats>>(`${BASE}/stats`)
      .pipe(map(r => r.data), catchError(this.handleError('getStats', {} as ShipmentStats)));
  }

  getShipment(id: string): Observable<ShipmentDetail> {
    return this.http.get<ApiResponse<ShipmentDetail>>(`${BASE}/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<ShipmentDetail>('getShipment')));
  }

  getShipmentsByOrder(orderId: string): Observable<ShipmentSummary[]> {
    return this.http.get<ApiResponse<ShipmentSummary[]>>(`${BASE}/order/${orderId}`)
      .pipe(map(r => r.data), catchError(this.handleError('getShipmentsByOrder', [])));
  }

  createShipment(req: CreateShipmentRequest): Observable<ShipmentDetail> {
    return this.http.post<ApiResponse<ShipmentDetail>>(BASE, req)
      .pipe(map(r => r.data), catchError(this.handleError<ShipmentDetail>('createShipment')));
  }

  addTrackingEvent(id: string, req: AddTrackingEventRequest): Observable<ShipmentDetail> {
    return this.http.post<ApiResponse<ShipmentDetail>>(`${BASE}/${id}/tracking-events`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ShipmentDetail>('addTrackingEvent')));
  }

  markDelivered(id: string): Observable<ShipmentDetail> {
    return this.http.post<ApiResponse<ShipmentDetail>>(`${BASE}/${id}/deliver`, {})
      .pipe(map(r => r.data), catchError(this.handleError<ShipmentDetail>('markDelivered')));
  }

  deleteShipment(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/${id}`)
      .pipe(map(() => void 0), catchError(this.handleError<void>('deleteShipment')));
  }

  private handleError<T>(op = 'op', result?: T) {
    return (err: any): Observable<T> => {
      console.error(`[ShipmentService] ${op}:`, err);
      this._error.set(err?.error?.message ?? err?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
