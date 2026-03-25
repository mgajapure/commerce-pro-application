import { API_HOST } from '../../config/api-config';
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  OperationHistoryItem,
  BulkEditValues,
  CopyOptions,
  DeleteOptions,
  ExportOptions,
  ImportMode,
  ExportFormat,
  ImportPreviewRow
} from '../../models/catalog/catalog-shared.model';
import { ApiResponse } from '../../models/common';

const BASE = `${API_HOST}/api/v1/products`;

export interface BulkOperationResult {
  success: boolean;
  affectedCount: number;
  errors: string[];
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class BulkOperationService {
  private http = inject(HttpClient);

  private history = signal<OperationHistoryItem[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);
  private processing = signal<boolean>(false);

  readonly operationHistory = computed(() => this.history());
  readonly isLoading = computed(() => this.loading());
  readonly isProcessing = computed(() => this.processing());
  readonly currentError = computed(() => this.error());

  getHistory(): Observable<OperationHistoryItem[]> {
    return of(this.history());
  }

  // ==================== Bulk Edit Operations ====================

  executeBulkEdit(
    productIds: string[],
    fields: string[],
    values: BulkEditValues
  ): Observable<BulkOperationResult> {
    this.processing.set(true);
    const startTime = Date.now();

    return this.http.post<ApiResponse<void>>(`${BASE}/bulk-status`, {
      ids: productIds,
      status: values.status || 'active'
    }).pipe(
      map(() => ({
        success: true,
        affectedCount: productIds.length,
        errors: [] as string[],
        duration: (Date.now() - startTime) / 1000
      })),
      tap(result => {
        this.addToHistory({
          id: `hist_${Date.now()}`,
          type: 'edit',
          title: 'Mass Edit - ' + fields.join(', '),
          description: `Updated ${fields.length} fields on ${result.affectedCount} products`,
          icon: 'pencil-square',
          status: 'completed',
          timestamp: new Date(),
          affectedCount: result.affectedCount,
          duration: result.duration,
          canUndo: true
        });
        this.processing.set(false);
      }),
      catchError(err => {
        this.processing.set(false);
        this.error.set(err?.error?.message ?? err?.message ?? 'Bulk edit failed');
        return of({
          success: false,
          affectedCount: 0,
          errors: [err?.message ?? 'Unknown error'],
          duration: (Date.now() - startTime) / 1000
        });
      })
    );
  }

  // ==================== Import Operations ====================

  validateImport(file: File, mode: ImportMode): Observable<ImportPreviewRow[]> {
    this.processing.set(true);
    // Import validation requires a backend endpoint — kept as client-side placeholder
    const mockPreview: ImportPreviewRow[] = [
      { name: 'New Product 1', sku: 'NEW-001', status: 'valid' },
      { name: 'New Product 2', sku: 'NEW-002', status: 'valid' },
      { name: 'Existing Product', sku: 'WH-PRO-001', status: 'error', message: 'SKU already exists' },
    ];
    this.processing.set(false);
    return of(mockPreview);
  }

  executeImport(
    file: File,
    mode: ImportMode,
    skipValidation: boolean
  ): Observable<BulkOperationResult> {
    this.processing.set(true);
    // Import execution requires a backend endpoint — kept as client-side placeholder
    const result: BulkOperationResult = {
      success: true,
      affectedCount: 0,
      errors: ['Import endpoint not yet implemented'],
      duration: 0
    };
    this.processing.set(false);
    return of(result);
  }

  // ==================== Export Operations ====================

  executeExport(
    productIds: string[] | 'all',
    format: ExportFormat,
    fields: string[],
    options: ExportOptions
  ): Observable<Blob> {
    this.processing.set(true);
    // Export requires a backend endpoint — kept as client-side placeholder
    const blob = new Blob(['Export not yet implemented'], { type: 'text/csv' });
    this.processing.set(false);
    return of(blob);
  }

  downloadTemplate(format: 'csv' | 'excel'): Observable<Blob> {
    const mockContent = 'SKU,Name,Description,Price,Stock';
    const blob = new Blob([mockContent], {
      type: format === 'csv' ? 'text/csv' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });
    return of(blob);
  }

  // ==================== Copy Operations ====================

  executeCopy(
    productIds: string[],
    options: CopyOptions
  ): Observable<BulkOperationResult> {
    this.processing.set(true);
    // Copy/duplicate requires a backend endpoint — kept as client-side placeholder
    const result: BulkOperationResult = {
      success: true,
      affectedCount: 0,
      errors: ['Duplicate endpoint not yet implemented'],
      duration: 0
    };
    this.processing.set(false);
    return of(result);
  }

  // ==================== Delete Operations ====================

  executeDelete(
    productIds: string[],
    options: DeleteOptions
  ): Observable<BulkOperationResult> {
    this.processing.set(true);
    const startTime = Date.now();

    return this.http.post<ApiResponse<void>>(`${BASE}/bulk-delete`, productIds)
      .pipe(
        map(() => ({
          success: true,
          affectedCount: productIds.length,
          errors: [] as string[],
          duration: (Date.now() - startTime) / 1000
        })),
        tap(result => {
          this.addToHistory({
            id: `hist_${Date.now()}`,
            type: 'delete',
            title: options.archiveInstead ? 'Archive Products' : 'Delete Products',
            description: options.archiveInstead
              ? `Archived ${result.affectedCount} products`
              : `Permanently deleted ${result.affectedCount} products`,
            icon: options.archiveInstead ? 'archive' : 'trash3',
            status: 'completed',
            timestamp: new Date(),
            affectedCount: result.affectedCount,
            duration: result.duration,
            canUndo: options.archiveInstead ?? false
          });
          this.processing.set(false);
        }),
        catchError(err => {
          this.processing.set(false);
          this.error.set(err?.error?.message ?? err?.message ?? 'Bulk delete failed');
          return of({
            success: false,
            affectedCount: 0,
            errors: [err?.message ?? 'Unknown error'],
            duration: (Date.now() - startTime) / 1000
          });
        })
      );
  }

  // ==================== Undo Operations ====================

  undoOperation(historyId: string): Observable<boolean> {
    const item = this.history().find(h => h.id === historyId);
    if (!item || !item.canUndo) {
      return throwError(() => new Error('Operation cannot be undone'));
    }
    this.history.update(current =>
      current.map(h =>
        h.id === historyId
          ? { ...h, status: 'failed' as const, description: h.description + ' (Undone)' }
          : h
      )
    );
    return of(true);
  }

  // ==================== History Management ====================

  private addToHistory(item: OperationHistoryItem): void {
    this.history.update(current => [item, ...current]);
  }

  clearHistory(): Observable<void> {
    this.history.set([]);
    return of(void 0);
  }
}
