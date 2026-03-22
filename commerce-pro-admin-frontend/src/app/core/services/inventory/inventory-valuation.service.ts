import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';
import {
  InventoryValuation,
  ValuationMethodType,
  ValuationStatus,
  ValuationSummary,
  ValuationMethodConfig
} from '../../models/inventory/inventory-valuation.model';

const BASE = 'http://localhost:8080/api/v1/inventory/valuations';

export interface ValuationStats {
  totalValuations: number;
  byMethod: Record<ValuationMethodType, number>;
  byStatus: Record<ValuationStatus, number>;
  totalValue: number;
  totalQuantity: number;
}

@Injectable({ providedIn: 'root' })
export class InventoryValuationService {
  private http = inject(HttpClient);

  private valuations = signal<InventoryValuation[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly allValuations = computed(() => this.valuations());
  readonly isLoading = computed(() => this.loading());
  readonly hasError = computed(() => this.error());

  readonly valuationStats = computed<ValuationStats>(() => {
    const all = this.valuations();
    const byMethod = all.reduce((acc, v) => {
      acc[v.valuationMethod] = (acc[v.valuationMethod] || 0) + 1;
      return acc;
    }, { fifo: 0, lifo: 0, weighted_average: 0, specific_identification: 0 } as Record<ValuationMethodType, number>);

    const byStatus = all.reduce((acc, v) => {
      acc[v.status] = (acc[v.status] || 0) + 1;
      return acc;
    }, {} as Record<ValuationStatus, number>);

    return {
      totalValuations: all.length,
      byMethod,
      byStatus,
      totalValue: all.reduce((sum, v) => sum + v.totalValue, 0),
      totalQuantity: all.reduce((sum, v) => sum + v.totalQuantity, 0)
    };
  });

  readonly valuationSummary = computed<ValuationSummary>(() => {
    const stats = this.valuationStats();
    const all = this.valuations();

    const methodBreakdown: Record<ValuationMethodType, { count: number; value: number }> = {
      fifo: { count: 0, value: 0 },
      lifo: { count: 0, value: 0 },
      weighted_average: { count: 0, value: 0 },
      specific_identification: { count: 0, value: 0 }
    };
    all.forEach(v => {
      methodBreakdown[v.valuationMethod].count++;
      methodBreakdown[v.valuationMethod].value += v.totalValue;
    });

    const warehouseBreakdown: Record<string, { name: string; value: number; quantity: number }> = {};
    all.forEach(v => {
      const whId = v.warehouseId || 'unspecified';
      if (!warehouseBreakdown[whId]) {
        warehouseBreakdown[whId] = { name: v.warehouseName || 'Unspecified', value: 0, quantity: 0 };
      }
      warehouseBreakdown[whId].value += v.totalValue;
      warehouseBreakdown[whId].quantity += v.totalQuantity;
    });

    return {
      totalInventoryValue: stats.totalValue,
      totalQuantity: stats.totalQuantity,
      averageUnitCost: stats.totalQuantity > 0 ? stats.totalValue / stats.totalQuantity : 0,
      methodBreakdown,
      warehouseBreakdown,
      categoryBreakdown: {},
      generatedAt: new Date()
    };
  });

  readonly activeValuations = computed(() =>
    this.valuations().filter(v => v.status === 'active')
  );

  constructor() {
    this.loadValuations();
  }

  // ==================== Load Operations ====================

  loadValuations(): void {
    this.loading.set(true);
    this.error.set(null);

    this.http.get<ApiResponse<any>>(BASE).pipe(
      map(r => {
        const data = r.data;
        return Array.isArray(data) ? data : (data?.content ?? []);
      }),
      map(valuations => this.transformDates(valuations)),
      catchError(this.handleError('loadValuations', []))
    ).subscribe({
      next: (valuations) => {
        this.valuations.set(valuations);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message);
        this.loading.set(false);
      }
    });
  }

  retry(): void {
    this.loadValuations();
  }

  // ==================== Read Operations ====================

  getValuations(): Observable<InventoryValuation[]> {
    return this.http.get<ApiResponse<any>>(BASE).pipe(
      map(r => {
        const data = r.data;
        return Array.isArray(data) ? data : (data?.content ?? []);
      }),
      map(valuations => this.transformDates(valuations)),
      catchError(this.handleError('getValuations', []))
    );
  }

  getValuationById(id: string): Observable<InventoryValuation | null> {
    return this.http.get<ApiResponse<InventoryValuation>>(`${BASE}/${id}`).pipe(
      map(r => r.data ? this.transformDate(r.data) : null),
      catchError(this.handleError('getValuationById', null))
    );
  }

  getValuationByProduct(productId: string): Observable<InventoryValuation | null> {
    return this.http.get<ApiResponse<InventoryValuation[]>>(`${BASE}/product/${productId}`).pipe(
      map(r => (r.data && r.data.length > 0) ? this.transformDate(r.data[0]) : null),
      catchError(this.handleError('getValuationByProduct', null))
    );
  }

  getValuationsByWarehouse(warehouseId: string): Observable<InventoryValuation[]> {
    return this.http.get<ApiResponse<InventoryValuation[]>>(`${BASE}/warehouse/${warehouseId}`).pipe(
      map(r => this.transformDates(r.data ?? [])),
      catchError(this.handleError('getValuationsByWarehouse', []))
    );
  }

  getValuationsByMethod(method: ValuationMethodType): Observable<InventoryValuation[]> {
    return this.getValuations().pipe(
      map(valuations => valuations.filter(v => v.valuationMethod === method))
    );
  }

  // ==================== CRUD Operations ====================

  createValuation(valuation: Partial<InventoryValuation>): Observable<InventoryValuation> {
    return this.http.post<ApiResponse<InventoryValuation>>(BASE, valuation).pipe(
      map(r => r.data),
      tap(() => this.loadValuations()),
      catchError(this.handleError<InventoryValuation>('createValuation'))
    );
  }

  updateValuation(id: string, updates: Partial<InventoryValuation>): Observable<InventoryValuation> {
    return this.http.put<ApiResponse<InventoryValuation>>(`${BASE}/${id}`, updates).pipe(
      map(r => r.data),
      tap(() => this.loadValuations()),
      catchError(this.handleError<InventoryValuation>('updateValuation'))
    );
  }

  deleteValuation(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/${id}`).pipe(
      map(() => void 0),
      tap(() => this.loadValuations()),
      catchError(this.handleError<void>('deleteValuation'))
    );
  }

  // ==================== Valuation Operations ====================

  recalculateValuations(
    productIds?: string[],
    method?: ValuationMethodType
  ): Observable<InventoryValuation[]> {
    this.loading.set(true);
    return this.http.post<ApiResponse<InventoryValuation[]>>(`${BASE}/recalculate`, {
      productIds,
      method
    }).pipe(
      map(r => r.data ?? []),
      tap(() => {
        this.loading.set(false);
        this.loadValuations();
      }),
      catchError(err => {
        this.loading.set(false);
        return this.handleError('recalculateValuations', [] as InventoryValuation[])(err);
      })
    );
  }

  getValuationSummary(): Observable<ValuationSummary> {
    return this.http.get<ApiResponse<any>>(`${BASE}/summary`).pipe(
      map(r => r.data ?? this.valuationSummary()),
      catchError(this.handleError('getValuationSummary', this.valuationSummary()))
    );
  }

  changeValuationMethod(id: string, method: ValuationMethodType): Observable<InventoryValuation> {
    return this.http.post<ApiResponse<InventoryValuation>>(`${BASE}/${id}/change-method`, null, {
      params: new HttpParams().set('method', method)
    }).pipe(
      map(r => r.data),
      tap(() => this.loadValuations()),
      catchError(this.handleError<InventoryValuation>('changeValuationMethod'))
    );
  }

  // ==================== Method Config Operations ====================

  getMethodConfigs(): Observable<ValuationMethodConfig[]> {
    return of([]);
  }

  createMethodConfig(config: Partial<ValuationMethodConfig>): Observable<ValuationMethodConfig> {
    return of(config as ValuationMethodConfig);
  }

  // ==================== Helper Methods ====================

  private transformDates(valuations: InventoryValuation[]): InventoryValuation[] {
    return valuations.map(v => this.transformDate(v));
  }

  private transformDate(valuation: InventoryValuation): InventoryValuation {
    return {
      ...valuation,
      calculatedAt: new Date(valuation.calculatedAt),
      lastAdjustmentAt: valuation.lastAdjustmentAt ? new Date(valuation.lastAdjustmentAt) : undefined,
      oldestCostLayerDate: valuation.oldestCostLayerDate ? new Date(valuation.oldestCostLayerDate) : undefined,
      newestCostLayerDate: valuation.newestCostLayerDate ? new Date(valuation.newestCostLayerDate) : undefined,
      costLayers: valuation.costLayers?.map(l => ({
        ...l,
        receiptDate: new Date(l.receiptDate),
        expiryDate: l.expiryDate ? new Date(l.expiryDate) : undefined
      })) || []
    };
  }

  private handleError<T>(op = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[InventoryValuationService] ${op}:`, error);
      this.error.set(error?.error?.message ?? error?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
