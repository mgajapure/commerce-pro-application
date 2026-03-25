import { API_HOST } from '../../config/api-config';
// src/app/core/services/inventory/warehouse.service.ts
// Warehouse management service - Angular HTTP Client integration with Spring Boot backend

import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { 
  ApiResponse 
} from '../../models/common';

import {
  Warehouse
} from '../../models/inventory';

// API Configuration
const API_CONFIG = {
  baseUrl: `${API_HOST}/api`,
  endpoints: {
    warehouses: '/v1/inventory/warehouses'
  }
};

@Injectable({
  providedIn: 'root'
})
export class WarehouseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = API_CONFIG.baseUrl;
  
  // Private signals for state management
  private warehouses = signal<Warehouse[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);
  
  // Public computed signals
  readonly allWarehouses = computed(() => this.warehouses());
  readonly isLoading = computed(() => this.loading());
  readonly hasError = computed(() => this.error());
  
  // Warehouse counts computation
  readonly warehouseCounts = computed(() => {
    const all = this.warehouses();
    const activeCount = all.filter(w => w.isActive !== false).length;
    const inactiveCount = all.filter(w => w.isActive === false).length;
    
    return {
      total: all.length,
      active: activeCount,
      inactive: inactiveCount,
      default: all.find(w => w.isDefault)?.name
    };
  });
  
  // Active warehouses only
  readonly activeWarehouses = computed(() => 
    this.warehouses().filter(w => w.isActive !== false)
  );
  
  // Recent activity (mock - would come from activity service)
  readonly recentActivity = computed(() => []);

  constructor() {
    this.loadWarehouses();
  }

  // ==================== Load Operations ====================

  /**
   * Load all warehouses
   * GET /api/inventory/warehouses
   */
  loadWarehouses(): void {
    this.loading.set(true);
    this.error.set(null);
    
    this.getWarehouses().subscribe({
      next: (warehouses) => {
        this.warehouses.set(warehouses);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message);
        this.loading.set(false);
      }
    });
  }

  /**
   * Retry loading warehouses after error
   */
  retry(): void {
    this.loadWarehouses();
  }

  // ==================== Read Operations ====================

  /**
   * Get all warehouses
   * GET /api/inventory/warehouses
   */
  getWarehouses(): Observable<Warehouse[]> {
    return this.http
      .get<ApiResponse<Warehouse[]>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<Warehouse[]>('getWarehouses', []))
      );
  }

  /**
   * Get active warehouses
   * GET /api/inventory/warehouses/active
   */
  getActiveWarehouses(): Observable<Warehouse[]> {
    return this.http
      .get<ApiResponse<Warehouse[]>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}/active`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<Warehouse[]>('getActiveWarehouses', []))
      );
  }

  /**
   * Get warehouse by ID
   * GET /api/inventory/warehouses/{id}
   */
  getWarehouseById(id: string): Observable<Warehouse | null> {
    return this.http
      .get<ApiResponse<Warehouse>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}/${id}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<null>('getWarehouseById', null))
      );
  }

  // ==================== CRUD Operations ====================

  /**
   * Create new warehouse
   * POST /api/inventory/warehouses
   */
  createWarehouse(warehouse: Partial<Warehouse>): Observable<Warehouse> {
    return this.http
      .post<ApiResponse<Warehouse>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}`, warehouse)
      .pipe(
        map(response => response.data),
        tap(() => this.loadWarehouses()),
        catchError(this.handleError<Warehouse>('createWarehouse'))
      );
  }

  /**
   * Update warehouse
   * PUT /api/inventory/warehouses/{id}
   */
  updateWarehouse(id: string, updates: Partial<Warehouse>): Observable<Warehouse> {
    return this.http
      .put<ApiResponse<Warehouse>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}/${id}`, updates)
      .pipe(
        map(response => response.data),
        tap(() => this.loadWarehouses()),
        catchError(this.handleError<Warehouse>('updateWarehouse'))
      );
  }

  /**
   * Delete warehouse
   * DELETE /api/inventory/warehouses/{id}
   */
  deleteWarehouse(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}/${id}`)
      .pipe(
        map(response => response.data),
        tap(() => this.loadWarehouses()),
        catchError(this.handleError<void>('deleteWarehouse'))
      );
  }

  /**
   * Toggle warehouse active status
   */
  toggleStatus(id: string): Observable<Warehouse> {
    const warehouse = this.warehouses().find(w => w.id === id);
    if (!warehouse) {
      return of();
    }
    
    return this.updateWarehouse(id, { isActive: !warehouse.isActive });
  }

  // ==================== Helper Methods ====================

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      this.error.set(error?.error?.message || error?.message || 'An error occurred');
      return of(result as T);
    };
  }
}
