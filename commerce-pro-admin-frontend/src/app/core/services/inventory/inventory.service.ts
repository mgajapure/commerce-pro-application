// src/app/core/services/inventory/inventory.service.ts
// Inventory service - Angular HTTP Client integration with Spring Boot backend

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { 
  ApiResponse, 
  PageResponse, 
  PageParams,
  buildPageParams
} from '../../models/common';

import {
  InventoryItem,
  InventoryStats,
  StockMovement,
  Warehouse,
  StockUpdateRequest,
  StockTransferRequest,
  InventoryFilter
} from '../../models/inventory';

// API Configuration
const API_CONFIG = {
  baseUrl: 'http://localhost:8080/api',
  endpoints: {
    inventory: '/v1/inventory',
    warehouses: '/v1/inventory/warehouses',
    stock: '/v1/inventory/stock',
    stats: '/v1/inventory/stats',
    transfer: '/v1/inventory/transfer'
  }
};

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = API_CONFIG.baseUrl;

  // ==================== State Management with Signals ====================
  
  private inventoryItems = signal<InventoryItem[]>([]);
  private warehouses = signal<Warehouse[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);
  private stats = signal<InventoryStats | null>(null);
  
  readonly allItems = computed(() => this.inventoryItems());
  readonly allWarehouses = computed(() => this.warehouses());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());
  readonly inventoryStats = computed(() => this.stats());

  // ==================== Warehouse Operations ====================

  /**
   * Get all warehouses
   * GET /api/inventory/warehouses
   */
  getWarehouses(): Observable<Warehouse[]> {
    return this.http
      .get<ApiResponse<Warehouse[]>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}`)
      .pipe(
        map(response => response.data),
        tap(warehouses => this.warehouses.set(warehouses)),
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
  getWarehouse(id: string): Observable<Warehouse | null> {
    return this.http
      .get<ApiResponse<Warehouse>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}/${id}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<null>('getWarehouse', null))
      );
  }

  /**
   * Create warehouse
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
  updateWarehouse(id: string, warehouse: Partial<Warehouse>): Observable<Warehouse> {
    return this.http
      .put<ApiResponse<Warehouse>>(`${this.baseUrl}${API_CONFIG.endpoints.warehouses}/${id}`, warehouse)
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

  // ==================== Inventory Operations ====================

  /**
   * Get inventory with filtering and pagination
   * GET /api/inventory?warehouseId=&status=&search=&page=&size=
   */
  getInventory(
    filter?: Partial<InventoryFilter>, 
    pageParams?: PageParams
  ): Observable<PageResponse<InventoryItem>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams || {}) });
    
    if (filter) {
      if (filter.searchQuery) params = params.set('search', filter.searchQuery);
      if (filter.warehouseId) params = params.set('warehouseId', filter.warehouseId);
      if (filter.status) params = params.set('status', filter.status);
      if (filter.category) params = params.set('category', filter.category);
      if (filter.lowStockOnly) params = params.set('lowStockOnly', 'true');
      if (filter.outOfStockOnly) params = params.set('outOfStockOnly', 'true');
    }

    return this.http
      .get<ApiResponse<PageResponse<InventoryItem>>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}`, { params })
      .pipe(
        map(response => response.data),
        catchError(this.handleError<PageResponse<InventoryItem>>('getInventory', { 
          content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true 
        }))
      );
  }

  /**
   * Get inventory by ID
   * GET /api/inventory/{id}
   */
  getInventoryById(id: string): Observable<InventoryItem | null> {
    return this.http
      .get<ApiResponse<InventoryItem>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}/${id}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<null>('getInventoryById', null))
      );
  }

  /**
   * Get inventory by product ID
   * GET /api/inventory/product/{productId}
   */
  getInventoryByProduct(productId: string): Observable<InventoryItem[]> {
    return this.http
      .get<ApiResponse<InventoryItem[]>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}/product/${productId}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<InventoryItem[]>('getInventoryByProduct', []))
      );
  }

  /**
   * Get inventory by warehouse ID
   * GET /api/inventory/warehouse/{warehouseId}
   */
  getInventoryByWarehouse(warehouseId: string): Observable<InventoryItem[]> {
    return this.http
      .get<ApiResponse<InventoryItem[]>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}/warehouse/${warehouseId}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<InventoryItem[]>('getInventoryByWarehouse', []))
      );
  }

  /**
   * Create inventory record
   * POST /api/inventory
   */
  createInventory(inventory: Partial<InventoryItem>): Observable<InventoryItem> {
    return this.http
      .post<ApiResponse<InventoryItem>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}`, inventory)
      .pipe(
        map(response => response.data),
        tap(() => this.clearCache()),
        catchError(this.handleError<InventoryItem>('createInventory'))
      );
  }

  /**
   * Update inventory record
   * PUT /api/inventory/{id}
   */
  updateInventory(id: string, inventory: Partial<InventoryItem>): Observable<InventoryItem> {
    return this.http
      .put<ApiResponse<InventoryItem>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}/${id}`, inventory)
      .pipe(
        map(response => response.data),
        tap(() => this.clearCache()),
        catchError(this.handleError<InventoryItem>('updateInventory'))
      );
  }

  /**
   * Delete inventory record
   * DELETE /api/inventory/{id}
   */
  deleteInventory(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}/${id}`)
      .pipe(
        map(response => response.data),
        tap(() => this.clearCache()),
        catchError(this.handleError<void>('deleteInventory'))
      );
  }

  // ==================== Stock Operations ====================

  /**
   * Adjust stock for inventory item
   * POST /api/inventory/{id}/stock
   */
  adjustStock(id: string, update: StockUpdateRequest): Observable<InventoryItem> {
    return this.http
      .post<ApiResponse<InventoryItem>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}/${id}/stock`, update)
      .pipe(
        map(response => response.data),
        tap(() => this.clearCache()),
        catchError(this.handleError<InventoryItem>('adjustStock'))
      );
  }

  /**
   * Transfer stock between warehouses
   * POST /api/inventory/transfer
   */
  transferStock(transfer: StockTransferRequest): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.baseUrl}${API_CONFIG.endpoints.transfer}`, transfer)
      .pipe(
        map(response => response.data),
        tap(() => this.clearCache()),
        catchError(this.handleError<void>('transferStock'))
      );
  }

  // ==================== Stock Movements ====================

  /**
   * Get stock movements for inventory item
   * GET /api/inventory/{id}/movements
   */
  getStockMovements(inventoryId: string): Observable<StockMovement[]> {
    return this.http
      .get<ApiResponse<StockMovement[]>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}/${inventoryId}/movements`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<StockMovement[]>('getStockMovements', []))
      );
  }

  /**
   * Get stock movements for product
   * GET /api/inventory/product/{productId}/movements
   */
  getProductStockMovements(productId: string): Observable<StockMovement[]> {
    return this.http
      .get<ApiResponse<StockMovement[]>>(`${this.baseUrl}${API_CONFIG.endpoints.inventory}/product/${productId}/movements`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<StockMovement[]>('getProductStockMovements', []))
      );
  }

  // ==================== Low/Out of Stock ====================

  /**
   * Get low stock items
   * GET /api/inventory/stock/low
   */
  getLowStockItems(): Observable<InventoryItem[]> {
    return this.http
      .get<ApiResponse<InventoryItem[]>>(`${this.baseUrl}${API_CONFIG.endpoints.stock}/low`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<InventoryItem[]>('getLowStockItems', []))
      );
  }

  /**
   * Get out of stock items
   * GET /api/inventory/stock/out-of-stock
   */
  getOutOfStockItems(): Observable<InventoryItem[]> {
    return this.http
      .get<ApiResponse<InventoryItem[]>>(`${this.baseUrl}${API_CONFIG.endpoints.stock}/out-of-stock`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<InventoryItem[]>('getOutOfStockItems', []))
      );
  }

  // ==================== Statistics ====================

  /**
   * Get inventory statistics
   * GET /api/inventory/stats
   */
  getInventoryStats(): Observable<InventoryStats> {
    return this.http
      .get<ApiResponse<InventoryStats>>(`${this.baseUrl}${API_CONFIG.endpoints.stats}`)
      .pipe(
        map(response => response.data),
        tap(stats => this.stats.set(stats)),
        catchError(this.handleError<InventoryStats>('getInventoryStats', {
          totalItems: 0, totalProducts: 0, totalWarehouses: 0,
          inStockCount: 0, lowStockCount: 0, outOfStockCount: 0, overstockCount: 0, notTrackedCount: 0,
          totalInventoryValue: 0, totalUnits: 0, totalReserved: 0, totalAvailable: 0, totalIncoming: 0,
          averageUnitCost: 0, statusBreakdown: {}, warehouseBreakdown: {}
        }))
      );
  }

  // ==================== Helper Methods ====================

  /**
   * Load warehouses into signal
   */
  loadWarehouses(): void {
    this.getWarehouses().subscribe();
  }

  /**
   * Clear local cache/state
   */
  private clearCache(): void {
    this.inventoryItems.set([]);
    this.stats.set(null);
  }

  /**
   * Error handler for HTTP requests
   */
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      this.error.set(error?.error?.message || error?.message || 'An error occurred');
      return of(result as T);
    };
  }
}
