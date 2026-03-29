import { API_HOST } from '../../config/api-config';
// src/app/core/services/suppliers/suppliers.service.ts
// Full backend integration — calls Commerce Pro Spring Boot API

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import {
  Supplier, SupplierRequest, SupplierStats, SupplierFilter,
  SupplierProduct, SupplierProductRequest,
  PurchaseOrder, PurchaseOrderRequest,
  ProcurementRequest, ProcurementRequestData,
  SupplierEvaluation, SupplierEvaluationRequest,
  SupplierContract, SupplierContractRequest
} from '../../models/suppliers/suppliers.model';

import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

const BASE = `${API_HOST}/api`;

@Injectable({ providedIn: 'root' })
export class SupplierApiService {

  private http = inject(HttpClient);

  // ─── Reactive state ──────────────────────────────────────────────────────
  private _loading = signal(false);
  private _error   = signal<string | null>(null);

  readonly isLoading    = computed(() => this._loading());
  readonly currentError = computed(() => this._error());

  // ─── Suppliers ─────────────────────────────────────────────────────────────

  getSuppliers(
    filter?: SupplierFilter,
    pageParams?: PageParams
  ): Observable<PageResponse<Supplier>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });

    if (filter) {
      if (filter.search)  params = params.set('search', filter.search);
      if (filter.status)  params = params.set('status', filter.status);
      if (filter.isPreferred !== undefined)
                          params = params.set('isPreferred', String(filter.isPreferred));
    }

    return this.http
      .get<ApiResponse<PageResponse<Supplier>>>(`${BASE}/v1/suppliers`, { params })
      .pipe(
        map(r => r.data),
        catchError(this.handleError<PageResponse<Supplier>>('getSuppliers', {
          content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
          first: true, last: true, empty: true
        }))
      );
  }

  getActiveSuppliers(): Observable<Supplier[]> {
    return this.http
      .get<ApiResponse<Supplier[]>>(`${BASE}/v1/suppliers/all`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<Supplier[]>('getActiveSuppliers', [])));
  }

  getPreferredSuppliers(): Observable<Supplier[]> {
    return this.http
      .get<ApiResponse<Supplier[]>>(`${BASE}/v1/suppliers/preferred`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<Supplier[]>('getPreferredSuppliers', [])));
  }

  getSupplierById(id: string): Observable<Supplier> {
    return this.http
      .get<ApiResponse<Supplier>>(`${BASE}/v1/suppliers/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<Supplier>('getSupplierById')));
  }

  getSupplierStats(): Observable<SupplierStats> {
    return this.http
      .get<ApiResponse<SupplierStats>>(`${BASE}/v1/suppliers/stats`)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierStats>('getSupplierStats', {
        total: 0, active: 0, preferred: 0
      })));
  }

  createSupplier(req: SupplierRequest): Observable<Supplier> {
    return this.http
      .post<ApiResponse<Supplier>>(`${BASE}/v1/suppliers`, req)
      .pipe(map(r => r.data), catchError(this.handleError<Supplier>('createSupplier')));
  }

  updateSupplier(id: string, req: SupplierRequest): Observable<Supplier> {
    return this.http
      .put<ApiResponse<Supplier>>(`${BASE}/v1/suppliers/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<Supplier>('updateSupplier')));
  }

  deleteSupplier(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/suppliers/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deleteSupplier')));
  }

  toggleActive(id: string, active: boolean): Observable<void> {
    return this.http
      .patch<ApiResponse<void>>(`${BASE}/v1/suppliers/${id}/active`, null, {
        params: new HttpParams().set('active', String(active))
      })
      .pipe(map(() => undefined), catchError(this.handleError<void>('toggleActive')));
  }

  togglePreferred(id: string, preferred: boolean): Observable<void> {
    return this.http
      .patch<ApiResponse<void>>(`${BASE}/v1/suppliers/${id}/preferred`, null, {
        params: new HttpParams().set('preferred', String(preferred))
      })
      .pipe(map(() => undefined), catchError(this.handleError<void>('togglePreferred')));
  }

  // ─── Supplier Products ─────────────────────────────────────────────────────

  getSupplierProducts(pageParams?: PageParams): Observable<PageResponse<SupplierProduct>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<SupplierProduct>>>(`${BASE}/v1/supplier-products`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<SupplierProduct>>('getSupplierProducts', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  getProductsBySupplier(supplierId: string): Observable<SupplierProduct[]> {
    return this.http
      .get<ApiResponse<SupplierProduct[]>>(`${BASE}/v1/supplier-products/supplier/${supplierId}`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<SupplierProduct[]>('getProductsBySupplier', [])));
  }

  getProductsByProduct(productId: string): Observable<SupplierProduct[]> {
    return this.http
      .get<ApiResponse<SupplierProduct[]>>(`${BASE}/v1/supplier-products/product/${productId}`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<SupplierProduct[]>('getProductsByProduct', [])));
  }

  getSupplierProductById(id: string): Observable<SupplierProduct> {
    return this.http
      .get<ApiResponse<SupplierProduct>>(`${BASE}/v1/supplier-products/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierProduct>('getSupplierProductById')));
  }

  createSupplierProduct(req: SupplierProductRequest): Observable<SupplierProduct> {
    return this.http
      .post<ApiResponse<SupplierProduct>>(`${BASE}/v1/supplier-products`, req)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierProduct>('createSupplierProduct')));
  }

  updateSupplierProduct(id: string, req: SupplierProductRequest): Observable<SupplierProduct> {
    return this.http
      .put<ApiResponse<SupplierProduct>>(`${BASE}/v1/supplier-products/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierProduct>('updateSupplierProduct')));
  }

  deleteSupplierProduct(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/supplier-products/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deleteSupplierProduct')));
  }

  // ─── Purchase Orders ───────────────────────────────────────────────────────

  getPurchaseOrders(pageParams?: PageParams): Observable<PageResponse<PurchaseOrder>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<PurchaseOrder>>>(`${BASE}/v1/purchase-orders`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<PurchaseOrder>>('getPurchaseOrders', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  getPurchaseOrdersBySupplier(supplierId: string): Observable<PurchaseOrder[]> {
    return this.http
      .get<ApiResponse<PurchaseOrder[]>>(`${BASE}/v1/purchase-orders/supplier/${supplierId}`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<PurchaseOrder[]>('getPurchaseOrdersBySupplier', [])));
  }

  getPurchaseOrderById(id: string): Observable<PurchaseOrder> {
    return this.http
      .get<ApiResponse<PurchaseOrder>>(`${BASE}/v1/purchase-orders/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<PurchaseOrder>('getPurchaseOrderById')));
  }

  getPurchaseOrderStats(): Observable<any> {
    return this.http
      .get<ApiResponse<any>>(`${BASE}/v1/purchase-orders/stats`)
      .pipe(map(r => r.data), catchError(this.handleError<any>('getPurchaseOrderStats', {})));
  }

  createPurchaseOrder(req: PurchaseOrderRequest): Observable<PurchaseOrder> {
    return this.http
      .post<ApiResponse<PurchaseOrder>>(`${BASE}/v1/purchase-orders`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PurchaseOrder>('createPurchaseOrder')));
  }

  updatePurchaseOrder(id: string, req: PurchaseOrderRequest): Observable<PurchaseOrder> {
    return this.http
      .put<ApiResponse<PurchaseOrder>>(`${BASE}/v1/purchase-orders/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<PurchaseOrder>('updatePurchaseOrder')));
  }

  deletePurchaseOrder(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/purchase-orders/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deletePurchaseOrder')));
  }

  updatePurchaseOrderStatus(id: string, status: string): Observable<PurchaseOrder> {
    return this.http
      .patch<ApiResponse<PurchaseOrder>>(`${BASE}/v1/purchase-orders/${id}/status`, null, {
        params: new HttpParams().set('status', status)
      })
      .pipe(map(r => r.data), catchError(this.handleError<PurchaseOrder>('updatePurchaseOrderStatus')));
  }

  receivePurchaseOrder(id: string): Observable<PurchaseOrder> {
    return this.http
      .post<ApiResponse<PurchaseOrder>>(`${BASE}/v1/purchase-orders/${id}/receive`, {})
      .pipe(map(r => r.data), catchError(this.handleError<PurchaseOrder>('receivePurchaseOrder')));
  }

  // ─── Procurement Requests ──────────────────────────────────────────────────

  getProcurementRequests(pageParams?: PageParams): Observable<PageResponse<ProcurementRequest>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<ProcurementRequest>>>(`${BASE}/v1/procurement-requests`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<ProcurementRequest>>('getProcurementRequests', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  getProcurementRequestById(id: string): Observable<ProcurementRequest> {
    return this.http
      .get<ApiResponse<ProcurementRequest>>(`${BASE}/v1/procurement-requests/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<ProcurementRequest>('getProcurementRequestById')));
  }

  getProcurementStats(): Observable<any> {
    return this.http
      .get<ApiResponse<any>>(`${BASE}/v1/procurement-requests/stats`)
      .pipe(map(r => r.data), catchError(this.handleError<any>('getProcurementStats', {})));
  }

  createProcurementRequest(req: ProcurementRequestData): Observable<ProcurementRequest> {
    return this.http
      .post<ApiResponse<ProcurementRequest>>(`${BASE}/v1/procurement-requests`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ProcurementRequest>('createProcurementRequest')));
  }

  updateProcurementRequest(id: string, req: ProcurementRequestData): Observable<ProcurementRequest> {
    return this.http
      .put<ApiResponse<ProcurementRequest>>(`${BASE}/v1/procurement-requests/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<ProcurementRequest>('updateProcurementRequest')));
  }

  deleteProcurementRequest(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/procurement-requests/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deleteProcurementRequest')));
  }

  approveProcurementRequest(id: string): Observable<ProcurementRequest> {
    return this.http
      .post<ApiResponse<ProcurementRequest>>(`${BASE}/v1/procurement-requests/${id}/approve`, {})
      .pipe(map(r => r.data), catchError(this.handleError<ProcurementRequest>('approveProcurementRequest')));
  }

  rejectProcurementRequest(id: string, reason: string): Observable<ProcurementRequest> {
    return this.http
      .post<ApiResponse<ProcurementRequest>>(`${BASE}/v1/procurement-requests/${id}/reject`, null, {
        params: new HttpParams().set('reason', reason)
      })
      .pipe(map(r => r.data), catchError(this.handleError<ProcurementRequest>('rejectProcurementRequest')));
  }

  // ─── Evaluations ───────────────────────────────────────────────────────────

  getEvaluations(pageParams?: PageParams): Observable<PageResponse<SupplierEvaluation>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<SupplierEvaluation>>>(`${BASE}/v1/supplier-evaluations`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<SupplierEvaluation>>('getEvaluations', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  getEvaluationsBySupplier(
    supplierId: string,
    pageParams?: PageParams
  ): Observable<PageResponse<SupplierEvaluation>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<SupplierEvaluation>>>(`${BASE}/v1/supplier-evaluations/supplier/${supplierId}`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<SupplierEvaluation>>('getEvaluationsBySupplier', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  getEvaluationById(id: string): Observable<SupplierEvaluation> {
    return this.http
      .get<ApiResponse<SupplierEvaluation>>(`${BASE}/v1/supplier-evaluations/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierEvaluation>('getEvaluationById')));
  }

  getSupplierAverageScores(supplierId: string): Observable<any> {
    return this.http
      .get<ApiResponse<any>>(`${BASE}/v1/supplier-evaluations/supplier/${supplierId}/scores`)
      .pipe(map(r => r.data), catchError(this.handleError<any>('getSupplierAverageScores', {})));
  }

  createEvaluation(req: SupplierEvaluationRequest): Observable<SupplierEvaluation> {
    return this.http
      .post<ApiResponse<SupplierEvaluation>>(`${BASE}/v1/supplier-evaluations`, req)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierEvaluation>('createEvaluation')));
  }

  updateEvaluation(id: string, req: SupplierEvaluationRequest): Observable<SupplierEvaluation> {
    return this.http
      .put<ApiResponse<SupplierEvaluation>>(`${BASE}/v1/supplier-evaluations/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierEvaluation>('updateEvaluation')));
  }

  deleteEvaluation(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/supplier-evaluations/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deleteEvaluation')));
  }

  submitEvaluation(id: string): Observable<SupplierEvaluation> {
    return this.http
      .post<ApiResponse<SupplierEvaluation>>(`${BASE}/v1/supplier-evaluations/${id}/submit`, {})
      .pipe(map(r => r.data), catchError(this.handleError<SupplierEvaluation>('submitEvaluation')));
  }

  // ─── Contracts ─────────────────────────────────────────────────────────────

  getContracts(pageParams?: PageParams): Observable<PageResponse<SupplierContract>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http
      .get<ApiResponse<PageResponse<SupplierContract>>>(`${BASE}/v1/supplier-contracts`, { params })
      .pipe(map(r => r.data), catchError(this.handleError<PageResponse<SupplierContract>>('getContracts', {
        content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      })));
  }

  getContractsBySupplier(supplierId: string): Observable<SupplierContract[]> {
    return this.http
      .get<ApiResponse<SupplierContract[]>>(`${BASE}/v1/supplier-contracts/supplier/${supplierId}`)
      .pipe(map(r => r.data ?? []), catchError(this.handleError<SupplierContract[]>('getContractsBySupplier', [])));
  }

  getContractById(id: string): Observable<SupplierContract> {
    return this.http
      .get<ApiResponse<SupplierContract>>(`${BASE}/v1/supplier-contracts/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierContract>('getContractById')));
  }

  getContractStats(): Observable<any> {
    return this.http
      .get<ApiResponse<any>>(`${BASE}/v1/supplier-contracts/stats`)
      .pipe(map(r => r.data), catchError(this.handleError<any>('getContractStats', {})));
  }

  getExpiringContracts(days?: number): Observable<SupplierContract[]> {
    let params = new HttpParams();
    if (days !== undefined) {
      params = params.set('days', String(days));
    }
    return this.http
      .get<ApiResponse<SupplierContract[]>>(`${BASE}/v1/supplier-contracts/expiring`, { params })
      .pipe(map(r => r.data ?? []), catchError(this.handleError<SupplierContract[]>('getExpiringContracts', [])));
  }

  createContract(req: SupplierContractRequest): Observable<SupplierContract> {
    return this.http
      .post<ApiResponse<SupplierContract>>(`${BASE}/v1/supplier-contracts`, req)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierContract>('createContract')));
  }

  updateContract(id: string, req: SupplierContractRequest): Observable<SupplierContract> {
    return this.http
      .put<ApiResponse<SupplierContract>>(`${BASE}/v1/supplier-contracts/${id}`, req)
      .pipe(map(r => r.data), catchError(this.handleError<SupplierContract>('updateContract')));
  }

  deleteContract(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${BASE}/v1/supplier-contracts/${id}`)
      .pipe(map(() => undefined), catchError(this.handleError<void>('deleteContract')));
  }

  activateContract(id: string): Observable<SupplierContract> {
    return this.http
      .post<ApiResponse<SupplierContract>>(`${BASE}/v1/supplier-contracts/${id}/activate`, {})
      .pipe(map(r => r.data), catchError(this.handleError<SupplierContract>('activateContract')));
  }

  terminateContract(id: string): Observable<SupplierContract> {
    return this.http
      .post<ApiResponse<SupplierContract>>(`${BASE}/v1/supplier-contracts/${id}/terminate`, {})
      .pipe(map(r => r.data), catchError(this.handleError<SupplierContract>('terminateContract')));
  }

  // ─── Error handler ────────────────────────────────────────────────────────

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[SupplierApiService] ${operation} failed:`, error);
      this._error.set(
        error?.error?.message ?? error?.message ?? 'An unexpected error occurred'
      );
      return of(result as T);
    };
  }
}
