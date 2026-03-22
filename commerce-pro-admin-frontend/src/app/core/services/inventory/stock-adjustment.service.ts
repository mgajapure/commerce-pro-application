import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiResponse } from '../../models/common';
import { StockMovement } from '../../models/inventory';

const BASE = 'http://localhost:8080/api/v1/inventory';

export type AdjustmentType = 'count' | 'damage' | 'receiving' | 'return' | 'transfer' | 'correction' | 'expiry';
export type AdjustmentStatus = 'draft' | 'pending' | 'approved' | 'rejected' | 'completed';
export type AdjustmentReason = 'damage' | 'expired' | 'lost' | 'found' | 'theft' | 'correction' | 'system_error' | 'other';

export interface AdjustmentItem {
  id: string;
  inventoryItemId: string;
  productId: string;
  productName: string;
  sku: string;
  image?: string;
  systemQty: number;
  countedQty: number;
  difference: number;
  unitCost: number;
  totalValue: number;
  reason: AdjustmentReason;
  notes?: string;
}

export interface AdjustmentBatch {
  id: string;
  adjustmentNumber: string;
  type: AdjustmentType;
  status: AdjustmentStatus;
  warehouseId: string;
  warehouseName?: string;
  reference?: string;
  notes?: string;
  items: AdjustmentItem[];
  totalValue: number;
  createdBy?: string;
  approvedBy?: string;
  approvedAt?: Date;
  createdAt: Date;
  updatedAt: Date;
}

export interface AdjustmentStats {
  total: number;
  draft: number;
  pending: number;
  approved: number;
  completed: number;
  rejected: number;
  totalValue: number;
}

@Injectable({ providedIn: 'root' })
export class StockAdjustmentService {
  private readonly http = inject(HttpClient);

  private adjustments = signal<AdjustmentBatch[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly allAdjustments = computed(() => this.adjustments());
  readonly isLoading = computed(() => this.loading());
  readonly hasError = computed(() => this.error());

  readonly stats = computed<AdjustmentStats>(() => {
    const all = this.adjustments();
    return {
      total: all.length,
      draft: all.filter(a => a.status === 'draft').length,
      pending: all.filter(a => a.status === 'pending').length,
      approved: all.filter(a => a.status === 'approved').length,
      completed: all.filter(a => a.status === 'completed').length,
      rejected: all.filter(a => a.status === 'rejected').length,
      totalValue: all.reduce((sum, a) => sum + (a.totalValue || 0), 0)
    };
  });

  createAdjustment(batch: Partial<AdjustmentBatch>): Observable<AdjustmentBatch> {
    const newBatch: AdjustmentBatch = {
      ...batch as AdjustmentBatch,
      id: `adj-${Date.now()}`,
      adjustmentNumber: `ADJ-${Date.now()}`,
      createdAt: new Date(),
      updatedAt: new Date()
    };
    this.adjustments.update(current => [newBatch, ...current]);
    return of(newBatch);
  }

  updateAdjustment(id: string, updates: Partial<AdjustmentBatch>): Observable<AdjustmentBatch> {
    this.adjustments.update(current =>
      current.map(a => a.id === id ? { ...a, ...updates, updatedAt: new Date() } : a)
    );
    const updated = this.adjustments().find(a => a.id === id);
    return of(updated!);
  }

  applyAdjustment(
    inventoryItemId: string,
    newQuantity: number,
    reason: string,
    notes?: string
  ): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${BASE}/${inventoryItemId}/stock`, {
      quantity: newQuantity,
      reason,
      notes,
      type: 'ADJUSTMENT'
    }).pipe(
      map(r => r.data),
      catchError(this.handleError<any>('applyAdjustment'))
    );
  }

  deleteAdjustment(id: string): Observable<void> {
    this.adjustments.update(current => current.filter(a => a.id !== id));
    return of(void 0);
  }

  approveAdjustment(id: string, approvedBy: string): Observable<AdjustmentBatch> {
    this.adjustments.update(current =>
      current.map(a =>
        a.id === id
          ? { ...a, status: 'approved' as AdjustmentStatus, approvedBy, approvedAt: new Date(), updatedAt: new Date() }
          : a
      )
    );
    return of(this.adjustments().find(a => a.id === id)!);
  }

  rejectAdjustment(id: string, rejectedBy: string, reason: string): Observable<AdjustmentBatch> {
    this.adjustments.update(current =>
      current.map(a =>
        a.id === id
          ? { ...a, status: 'rejected' as AdjustmentStatus, updatedAt: new Date() }
          : a
      )
    );
    return of(this.adjustments().find(a => a.id === id)!);
  }

  completeAdjustment(id: string): Observable<AdjustmentBatch> {
    this.adjustments.update(current =>
      current.map(a =>
        a.id === id
          ? { ...a, status: 'completed' as AdjustmentStatus, updatedAt: new Date() }
          : a
      )
    );
    return of(this.adjustments().find(a => a.id === id)!);
  }

  submitForApproval(id: string): Observable<AdjustmentBatch> {
    this.adjustments.update(current =>
      current.map(a =>
        a.id === id
          ? { ...a, status: 'pending' as AdjustmentStatus, updatedAt: new Date() }
          : a
      )
    );
    return of(this.adjustments().find(a => a.id === id)!);
  }

  retry(): void {
    this.error.set(null);
  }

  private handleError<T>(op = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[StockAdjustmentService] ${op}:`, error);
      this.error.set(error?.error?.message || error?.message || 'An error occurred');
      return of(result as T);
    };
  }
}
