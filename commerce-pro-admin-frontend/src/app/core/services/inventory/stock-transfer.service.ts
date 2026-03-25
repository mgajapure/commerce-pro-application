import { API_HOST } from '../../config/api-config';
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiResponse } from '../../models/common';
import { StockTransferRequest } from '../../models/inventory';

const BASE = `${API_HOST}/api/v1/inventory`;

export type TransferStatus = 'draft' | 'pending' | 'approved' | 'shipped' | 'in_transit' | 'received' | 'completed' | 'cancelled';
export type TransferPriority = 'low' | 'normal' | 'high' | 'urgent';
export type TransferType = 'standard' | 'replenishment' | 'returns' | 'consignment';

export interface TransferItem {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  image?: string;
  quantity: number;
  unitCost: number;
  totalValue: number;
  notes?: string;
}

export interface StockTransfer {
  id: string;
  transferNumber: string;
  type: TransferType;
  status: TransferStatus;
  priority: TransferPriority;
  fromWarehouseId: string;
  fromWarehouseName?: string;
  toWarehouseId: string;
  toWarehouseName?: string;
  reference?: string;
  notes?: string;
  items: TransferItem[];
  totalValue: number;
  createdBy?: string;
  approvedBy?: string;
  approvedAt?: Date;
  shippedAt?: Date;
  receivedAt?: Date;
  completedAt?: Date;
  createdAt: Date;
  updatedAt: Date;
}

export interface TransferStats {
  total: number;
  draft: number;
  pending: number;
  approved: number;
  inTransit: number;
  completed: number;
  cancelled: number;
  totalValue: number;
}

@Injectable({ providedIn: 'root' })
export class StockTransferService {
  private readonly http = inject(HttpClient);

  private transfers = signal<StockTransfer[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly allTransfers = computed(() => this.transfers());
  readonly isLoading = computed(() => this.loading());
  readonly hasError = computed(() => this.error());

  readonly stats = computed<TransferStats>(() => {
    const all = this.transfers();
    return {
      total: all.length,
      draft: all.filter(t => t.status === 'draft').length,
      pending: all.filter(t => t.status === 'pending').length,
      approved: all.filter(t => t.status === 'approved').length,
      inTransit: all.filter(t => t.status === 'in_transit' || t.status === 'shipped').length,
      completed: all.filter(t => t.status === 'completed').length,
      cancelled: all.filter(t => t.status === 'cancelled').length,
      totalValue: all.reduce((sum, t) => sum + (t.totalValue || 0), 0)
    };
  });

  createTransfer(transfer: Partial<StockTransfer>): Observable<StockTransfer> {
    const newTransfer: StockTransfer = {
      ...transfer as StockTransfer,
      id: `trf-${Date.now()}`,
      transferNumber: `TRF-${Date.now()}`,
      createdAt: new Date(),
      updatedAt: new Date()
    };
    this.transfers.update(current => [newTransfer, ...current]);
    return of(newTransfer);
  }

  updateTransfer(id: string, updates: Partial<StockTransfer>): Observable<StockTransfer> {
    this.transfers.update(current =>
      current.map(t => t.id === id ? { ...t, ...updates, updatedAt: new Date() } : t)
    );
    return of(this.transfers().find(t => t.id === id)!);
  }

  updateStatus(id: string, status: TransferStatus): Observable<StockTransfer> {
    const updates: Partial<StockTransfer> = { status };
    if (status === 'approved') updates.approvedAt = new Date();
    if (status === 'shipped') updates.shippedAt = new Date();
    if (status === 'received') updates.receivedAt = new Date();
    if (status === 'completed') updates.completedAt = new Date();
    return this.updateTransfer(id, updates);
  }

  executeTransfer(fromWarehouseId: string, toWarehouseId: string, productId: string, quantity: number, reason?: string): Observable<void> {
    return this.http.post<ApiResponse<void>>(`${BASE}/transfer`, {
      fromWarehouseId,
      toWarehouseId,
      productId,
      quantity,
      reason
    }).pipe(
      map(() => void 0),
      catchError(this.handleError<void>('executeTransfer'))
    );
  }

  deleteTransfer(id: string): Observable<void> {
    this.transfers.update(current => current.filter(t => t.id !== id));
    return of(void 0);
  }

  approveTransfer(id: string, approvedBy: string): Observable<StockTransfer> {
    return this.updateStatus(id, 'approved');
  }

  cancelTransfer(id: string): Observable<StockTransfer> {
    return this.updateStatus(id, 'cancelled');
  }

  shipTransfer(id: string): Observable<StockTransfer> {
    return this.updateStatus(id, 'shipped');
  }

  markInTransit(id: string): Observable<StockTransfer> {
    return this.updateStatus(id, 'in_transit');
  }

  receiveTransfer(id: string): Observable<StockTransfer> {
    return this.updateStatus(id, 'received');
  }

  completeTransfer(id: string): Observable<StockTransfer> {
    return this.updateStatus(id, 'completed');
  }

  retry(): void {
    this.error.set(null);
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[StockTransferService] ${operation}:`, error);
      this.error.set(error?.error?.message || error?.message || 'An error occurred');
      return of(result as T);
    };
  }
}
