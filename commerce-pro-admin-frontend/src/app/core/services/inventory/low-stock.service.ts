import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiResponse } from '../../models/common';
import {
  LowStockAlert,
  LowStockFilter,
  LowStockStats,
  ReorderSuggestion
} from '../../models/inventory';

const BASE = 'http://localhost:8080/api/v1/inventory';

@Injectable({ providedIn: 'root' })
export class LowStockService {
  private readonly http = inject(HttpClient);

  private alerts = signal<LowStockAlert[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly poSuggestions = signal<ReorderSuggestion[]>([]);

  readonly allAlerts = computed(() => this.alerts());
  readonly isLoading = computed(() => this.loading());
  readonly hasError = computed(() => this.error());

  readonly lowStockStats = computed<LowStockStats>(() => {
    const all = this.alerts();
    const criticalCount = all.filter(a => a.status === 'CRITICAL').length;
    const lowCount = all.filter(a => a.status === 'LOW').length;
    const reorderCount = all.filter(a => a.status === 'REORDER').length;
    const acknowledgedCount = all.filter(a => a.acknowledged).length;

    return {
      total: all.length,
      critical: criticalCount,
      low: lowCount,
      reorder: reorderCount,
      acknowledged: acknowledgedCount,
      unacknowledged: all.length - acknowledgedCount,
      unreadAlerts: all.filter(a => !a.isRead).length,
      totalItems: all.length,
      criticalCount,
      lowCount,
      adequateCount: 0,
      excessCount: 0,
      poSuggestions: this.poSuggestions().length,
      totalShortageValue: all.reduce((sum, a) => sum + ((a.reorderQuantity || 0) * 10), 0),
      avgDaysUntilStockout: all.length > 0
        ? all.reduce((sum, a) => sum + (a.daysUntilStockout || 0), 0) / all.length
        : 0
    };
  });

  readonly criticalAlerts = computed(() =>
    this.alerts().filter(a => a.status === 'CRITICAL')
  );

  readonly unacknowledgedAlerts = computed(() =>
    this.alerts().filter(a => !a.acknowledged && !a.resolved)
  );

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.getLowStockAlerts().subscribe({
      next: (alerts) => {
        this.alerts.set(alerts);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message);
        this.loading.set(false);
      }
    });
  }

  getLowStockAlerts(filter?: LowStockFilter): Observable<LowStockAlert[]> {
    return this.http.get<ApiResponse<LowStockAlert[]>>(`${BASE}/alerts/low-stock`)
      .pipe(map(r => r.data), catchError(this.handleError('getLowStockAlerts', [])));
  }

  getCriticalAlerts(): Observable<LowStockAlert[]> {
    return this.http.get<ApiResponse<LowStockAlert[]>>(`${BASE}/alerts/critical`)
      .pipe(map(r => r.data), catchError(this.handleError('getCriticalAlerts', [])));
  }

  getAlertsByWarehouse(warehouseId: string): Observable<LowStockAlert[]> {
    return this.http.get<ApiResponse<LowStockAlert[]>>(`${BASE}/alerts/warehouse/${warehouseId}`)
      .pipe(map(r => r.data), catchError(this.handleError('getAlertsByWarehouse', [])));
  }

  acknowledgeAlert(alertId: string): Observable<void> {
    this.alerts.update(current =>
      current.map(a =>
        a.id === alertId
          ? { ...a, acknowledged: true, isRead: true, updatedAt: new Date() }
          : a
      )
    );
    return of(void 0);
  }

  resolveAlert(alertId: string): Observable<void> {
    this.alerts.update(current =>
      current.map(a =>
        a.id === alertId
          ? { ...a, resolved: true, resolvedAt: new Date(), updatedAt: new Date() }
          : a
      )
    );
    return of(void 0);
  }

  markAsRead(alertId: string): Observable<void> {
    this.alerts.update(current =>
      current.map(a =>
        a.id === alertId
          ? { ...a, isRead: true, updatedAt: new Date() }
          : a
      )
    );
    return of(void 0);
  }

  bulkAcknowledge(alertIds: string[]): Observable<void> {
    this.alerts.update(current =>
      current.map(a =>
        alertIds.includes(a.id)
          ? { ...a, acknowledged: true, isRead: true, updatedAt: new Date() }
          : a
      )
    );
    return of(void 0);
  }

  retry(): void {
    this.loadData();
  }

  private handleError<T>(op = 'op', result?: T) {
    return (err: any): Observable<T> => {
      console.error(`[LowStockService] ${op}:`, err);
      this.error.set(err?.error?.message ?? err?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
