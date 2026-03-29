import { API_HOST } from '../../config/api-config';
// src/app/core/services/customer/customer-api.service.ts
// Full backend integration — calls Commerce Pro Spring Boot API

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import {
  CustomerSummary, CustomerResponse, CustomerStats,
  CustomerAddress, AddressRequest,
  CustomerGroup, GroupRequest,
  CommunicationLog, CommunicationLogRequest,
  Wishlist,
  SavedCart,
  CustomerOrderHistory,
  CustomerRequest, CustomerFilter,
  BlacklistRequest, FraudFlagRequest, FraudResolveRequest,
  LoyaltyAdjustRequest, TagsUpdateRequest
  formatFilterDateTime,
} from '../../models/customers/customers.model';

import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

const BASE = `${API_HOST}/api`;

@Injectable({ providedIn: 'root' })
export class CustomerApiService {

  private http = inject(HttpClient);

  // ─── Reactive state ──────────────────────────────────────────────────────
  private _loading = signal(false);
  private _error   = signal<string | null>(null);
  private _stats   = signal<CustomerStats | null>(null);

  readonly isLoading    = computed(() => this._loading());
  readonly currentError = computed(() => this._error());
  readonly customerStats = computed(() => this._stats());

  // ─── Customers ───────────────────────────────────────────────────────────

  getCustomers(
    filter?: CustomerFilter,
    pageParams?: PageParams
  ): Observable<PageResponse<CustomerSummary>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });

    if (filter) {
      if (filter.search)          params = params.set('search',           filter.search);
      if (filter.status)          params = params.set('status',           filter.status);
      if (filter.tier)            params = params.set('tier',             filter.tier);
      if (filter.groupId)         params = params.set('groupId',          filter.groupId);
      if (filter.tag)             params = params.set('tag',              filter.tag);
      if (filter.acquisitionSource) params = params.set('acquisitionSource', filter.acquisitionSource);
      if (filter.isBlacklisted !== undefined)
                                  params = params.set('isBlacklisted',    String(filter.isBlacklisted));
      if (filter.isFraudFlagged !== undefined)
                                  params = params.set('isFraudFlagged',   String(filter.isFraudFlagged));
      if (filter.marketingOptIn !== undefined)
                                  params = params.set('marketingOptIn',   String(filter.marketingOptIn));
      if (filter.minLifetimeSpend !== undefined)
                                  params = params.set('minLifetimeSpend', String(filter.minLifetimeSpend));
      if (filter.maxLifetimeSpend !== undefined)
                                  params = params.set('maxLifetimeSpend', String(filter.maxLifetimeSpend));
      if (filter.minTotalOrders !== undefined)
                                  params = params.set('minTotalOrders',   String(filter.minTotalOrders));
      if (filter.createdFrom)     params = params.set('createdFrom',      formatFilterDateTime(filter.createdFrom));
      if (filter.createdTo)       params = params.set('createdTo',        formatFilterDateTime(filter.createdTo, true));
      if (filter.lastOrderFrom)   params = params.set('lastOrderFrom',    formatFilterDateTime(filter.lastOrderFrom));
      if (filter.lastOrderTo)     params = params.set('lastOrderTo',      formatFilterDateTime(filter.lastOrderTo, true));
    }

    return this.http
      .get<ApiResponse<PageResponse<CustomerSummary>>>(`${BASE}/v1/customers`, { params })
      .pipe(
        map(r => r.data),
        catchError(this.handleError<PageResponse<CustomerSummary>>('getCustomers', {
          content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
          first: true, last: true, empty: true
        }))
      );
  }

  getCustomerById(id: string): Observable<CustomerResponse> {
    return this.http
      .get<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('getCustomerById')));
  }

  getStats(): Observable<CustomerStats> {
    return this.http
      .get<ApiResponse<CustomerStats>>(`${BASE}/v1/customers/stats`)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerStats>('getStats', {
        totalCustomers: 0, activeCustomers: 0, newCustomersToday: 0, newCustomersThisMonth: 0,
        blacklistedCustomers: 0, fraudFlaggedCustomers: 0, byTier: {}, byStatus: {},
        totalLifetimeRevenue: 0, averageLifetimeValue: 0, loyaltyPointsOutstanding: 0
      })));
  }

  createCustomer(req: CustomerRequest): Observable<CustomerResponse> {
    return this.http
      .post<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('createCustomer')));
  }

  updateCustomer(id: string, req: CustomerRequest): Observable<CustomerResponse> {
    return this.http
      .put<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('updateCustomer')));
  }

  deleteCustomer(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/customers/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deleteCustomer')));
  }

  // ─── Status & Tier ────────────────────────────────────────────────────────

  setStatus(id: string, status: string): Observable<CustomerResponse> {
    return this.http
      .post<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}/status`, null, {
        params: new HttpParams().set('status', status)
      })
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('setStatus')));
  }

  evaluateTier(id: string): Observable<CustomerResponse> {
    return this.http
      .post<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}/evaluate-tier`, {})
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('evaluateTier')));
  }

  // ─── Blacklist & Fraud ────────────────────────────────────────────────────

  blacklist(id: string, req: BlacklistRequest): Observable<CustomerResponse> {
    return this.http
      .post<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}/blacklist`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('blacklist')));
  }

  unblacklist(id: string, reason: string): Observable<CustomerResponse> {
    return this.http
      .post<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}/unblacklist`, null, {
        params: new HttpParams().set('reason', reason)
      })
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('unblacklist')));
  }

  flagFraud(id: string, req: FraudFlagRequest): Observable<CustomerResponse> {
    return this.http
      .post<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}/fraud-flag`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('flagFraud')));
  }

  resolveFraud(id: string, req: FraudResolveRequest): Observable<CustomerResponse> {
    return this.http
      .post<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}/fraud-resolve`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('resolveFraud')));
  }

  // ─── Loyalty ──────────────────────────────────────────────────────────────

  adjustLoyalty(id: string, req: LoyaltyAdjustRequest): Observable<CustomerResponse> {
    return this.http
      .post<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}/loyalty/adjust`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('adjustLoyalty')));
  }

  // ─── Tags ─────────────────────────────────────────────────────────────────

  updateTags(id: string, req: TagsUpdateRequest): Observable<CustomerResponse> {
    return this.http
      .put<ApiResponse<CustomerResponse>>(`${BASE}/v1/customers/${id}/tags`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerResponse>('updateTags')));
  }

  // ─── Order history ────────────────────────────────────────────────────────

  getOrderHistory(
    id: string,
    pageParams?: PageParams
  ): Observable<PageResponse<CustomerOrderHistory>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<CustomerOrderHistory>>>(`${BASE}/v1/customers/${id}/orders`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<CustomerOrderHistory>>('getOrderHistory', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  // ─── Addresses ───────────────────────────────────────────────────────────

  getAddresses(id: string): Observable<CustomerAddress[]> {
    return this.http
      .get<ApiResponse<CustomerAddress[]>>(`${BASE}/v1/customers/${id}/addresses`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<CustomerAddress[]>('getAddresses', [])));
  }

  addAddress(id: string, req: AddressRequest): Observable<CustomerAddress> {
    return this.http
      .post<ApiResponse<CustomerAddress>>(`${BASE}/v1/customers/${id}/addresses`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerAddress>('addAddress')));
  }

  updateAddress(customerId: string, addressId: string, req: AddressRequest): Observable<CustomerAddress> {
    return this.http
      .put<ApiResponse<CustomerAddress>>(`${BASE}/v1/customers/${customerId}/addresses/${addressId}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerAddress>('updateAddress')));
  }

  deleteAddress(customerId: string, addressId: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/customers/${customerId}/addresses/${addressId}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deleteAddress')));
  }

  setDefaultAddress(customerId: string, addressId: string): Observable<CustomerAddress> {
    return this.http
      .post<ApiResponse<CustomerAddress>>(
        `${BASE}/v1/customers/${customerId}/addresses/${addressId}/set-default`, {}
      )
      .pipe(map(r => r.data), catchError(this.handleError<CustomerAddress>('setDefaultAddress')));
  }

  // ─── Communication Logs ───────────────────────────────────────────────────

  getCommunicationLogs(
    id: string,
    pageParams?: PageParams
  ): Observable<PageResponse<CommunicationLog>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<CommunicationLog>>>(`${BASE}/v1/customers/${id}/communications`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<CommunicationLog>>('getCommunicationLogs', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  addCommunicationLog(id: string, req: CommunicationLogRequest): Observable<CommunicationLog> {
    return this.http
      .post<ApiResponse<CommunicationLog>>(`${BASE}/v1/customers/${id}/communications`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CommunicationLog>('addCommunicationLog')));
  }

  resolveCommunicationLog(customerId: string, logId: string): Observable<CommunicationLog> {
    return this.http
      .post<ApiResponse<CommunicationLog>>(
        `${BASE}/v1/customers/${customerId}/communications/${logId}/resolve`, {}
      )
      .pipe(map(r => r.data), catchError(this.handleError<CommunicationLog>('resolveCommunicationLog')));
  }

  // ─── Wishlists ────────────────────────────────────────────────────────────

  getWishlists(id: string): Observable<Wishlist[]> {
    return this.http
      .get<ApiResponse<Wishlist[]>>(`${BASE}/v1/customers/${id}/wishlists`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<Wishlist[]>('getWishlists', [])));
  }

  // ─── Saved Carts ──────────────────────────────────────────────────────────

  getSavedCarts(id: string): Observable<SavedCart[]> {
    return this.http
      .get<ApiResponse<SavedCart[]>>(`${BASE}/v1/customers/${id}/saved-carts`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<SavedCart[]>('getSavedCarts', [])));
  }

  // ─── Groups ───────────────────────────────────────────────────────────────

  getGroups(pageParams?: PageParams): Observable<PageResponse<CustomerGroup>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<CustomerGroup>>>(`${BASE}/v1/customer-groups`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<CustomerGroup>>('getGroups', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  getActiveGroups(): Observable<CustomerGroup[]> {
    return this.http
      .get<ApiResponse<CustomerGroup[]>>(`${BASE}/v1/customer-groups/active`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<CustomerGroup[]>('getActiveGroups', [])));
  }

  createGroup(req: GroupRequest): Observable<CustomerGroup> {
    return this.http
      .post<ApiResponse<CustomerGroup>>(`${BASE}/v1/customer-groups`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerGroup>('createGroup')));
  }

  updateGroup(id: string, req: GroupRequest): Observable<CustomerGroup> {
    return this.http
      .put<ApiResponse<CustomerGroup>>(`${BASE}/v1/customer-groups/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<CustomerGroup>('updateGroup')));
  }

  deleteGroup(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/customer-groups/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deleteGroup')));
  }

  // ─── Error handler ────────────────────────────────────────────────────────

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[CustomerApiService] ${operation} failed:`, error);
      this._error.set(
        error?.error?.message ?? error?.message ?? 'An unexpected error occurred'
      );
      return of(result as T);
    };
  }
}
