import { API_HOST } from '../../config/api-config';
// src/app/core/services/product.service.ts
// Product service - Angular HTTP Client integration with Spring Boot backend

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

// Import existing models
import { 
  Product, 
  ProductSummary, 
  ProductFilterState, 
  ProductStats,
  ProductStatusCount,
  ProductDashboardView
} from '../../models/catalog/product.model';

// Import common API models
import { 
  ApiResponse, 
  PageResponse, 
  PageParams,
  buildPageParams
} from '../../models/common';

// Import catalog-specific models
import { 
  ProductRequest,
  StockUpdateRequest,
  BulkStatusUpdateRequest,
  BulkDeleteRequest,
  ProductResponse,
  ProductStatsResponse
} from '../../models/catalog';

// API Configuration
const API_CONFIG = {
  baseUrl: `${API_HOST}/api`,
  endpoints: {
    products: '/v1/products',
    search: '/v1/products/search',
    featured: '/v1/products/featured',
    stock: '/v1/products/stock',
    dashboard: '/v1/products/dashboard',
    stats: '/v1/products/stats',
    reference: '/v1/products/reference',
    validate: '/v1/products/validate',
    bulkDelete: '/v1/products/bulk-delete',
    bulkStatus: '/v1/products/bulk-status'
  }
};

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = API_CONFIG.baseUrl;

  // ==================== State Management (Optional - using Signals) ====================
  
  private products = signal<Product[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);
  private stats = signal<ProductStatsResponse | null>(null);
  
  readonly allProducts = computed(() => this.products());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());
  readonly productStats = computed(() => this.stats());

  // ==================== CRUD Operations ====================

  /**
   * Get paginated products with optional filtering
   * GET /api/products?page=0&size=20&sort=name,asc&status=active...
   */
  getProducts(
    filter?: Partial<ProductFilterState>, 
    pageParams?: PageParams
  ): Observable<PageResponse<ProductSummary>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams || {}) });
    
    // Add filter parameters if provided
    if (filter) {
      if (filter.searchQuery) params = params.set('search', filter.searchQuery);
      if (filter.status) params = params.set('status', filter.status);
      if (filter.category) params = params.set('category', filter.category);
      if (filter.stockStatus) params = params.set('stockStatus', filter.stockStatus);
      if (filter.brand) params = params.set('brand', filter.brand);
      if (filter.minPrice !== null && filter.minPrice !== undefined) {
        params = params.set('minPrice', filter.minPrice.toString());
      }
      if (filter.maxPrice !== null && filter.maxPrice !== undefined) {
        params = params.set('maxPrice', filter.maxPrice.toString());
      }
      if (filter.minRating) params = params.set('minRating', filter.minRating.toString());
    }

    return this.http
      .get<ApiResponse<PageResponse<ProductSummary>>>(`${this.baseUrl}${API_CONFIG.endpoints.products}`, { params })
      .pipe(
        map(response => response.data),
        catchError(this.handleError<PageResponse<ProductSummary>>('getProducts', { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }))
      );
  }

  /**
   * Get all products (for exports, dropdowns, etc.)
   * Note: Consider pagination for large datasets
   */
  getAllProducts(): Observable<ProductSummary[]> {
    return this.http
      .get<ApiResponse<ProductSummary[]>>(`${this.baseUrl}${API_CONFIG.endpoints.products}/all`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<ProductSummary[]>('getAllProducts', []))
      );
  }

  /**
   * Get product by ID
   * GET /api/products/{id}
   */
  getProduct(id: string): Observable<Product | null> {
    return this.http
      .get<ApiResponse<ProductResponse>>(`${this.baseUrl}${API_CONFIG.endpoints.products}/${id}`)
      .pipe(
        map(response => this.transformProductResponse(response.data)),
        catchError(this.handleError<null>('getProduct', null))
      );
  }

  /**
   * Create new product
   * POST /api/products
   */
  createProduct(product: ProductRequest): Observable<Product> {
    return this.http
      .post<ApiResponse<ProductResponse>>(`${this.baseUrl}${API_CONFIG.endpoints.products}`, product)
      .pipe(
        map(response => this.transformProductResponse(response.data)),
        tap(() => this.clearCache()),
        catchError(this.handleError<Product>('createProduct'))
      );
  }

  /**
   * Update product (full update)
   * PUT /api/products/{id}
   */
  updateProduct(id: string, product: ProductRequest): Observable<Product> {
    return this.http
      .put<ApiResponse<ProductResponse>>(`${this.baseUrl}${API_CONFIG.endpoints.products}/${id}`, product)
      .pipe(
        map(response => this.transformProductResponse(response.data)),
        tap(() => this.clearCache()),
        catchError(this.handleError<Product>('updateProduct'))
      );
  }

  /**
   * Partial update product
   * PATCH /api/products/{id}
   */
  patchProduct(id: string, updates: Partial<ProductRequest>): Observable<Product> {
    return this.http
      .patch<ApiResponse<ProductResponse>>(`${this.baseUrl}${API_CONFIG.endpoints.products}/${id}`, updates)
      .pipe(
        map(response => this.transformProductResponse(response.data)),
        tap(() => this.clearCache()),
        catchError(this.handleError<Product>('patchProduct'))
      );
  }

  /**
   * Delete product
   * DELETE /api/products/{id}
   */
  deleteProduct(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}${API_CONFIG.endpoints.products}/${id}`)
      .pipe(
        map(response => response.data),
        tap(() => this.clearCache()),
        catchError(this.handleError<void>('deleteProduct'))
      );
  }

  // ==================== Search & Filter ====================

  /**
   * Search products
   * GET /api/products/search?query={query}
   */
  searchProducts(query: string, pageParams?: PageParams): Observable<PageResponse<ProductSummary>> {
    let params = new HttpParams()
      .set('query', query);
    
    // Add pagination params
    const pagination = buildPageParams(pageParams || {});
    Object.entries(pagination).forEach(([key, value]) => {
      params = params.set(key, value);
    });

    return this.http
      .get<ApiResponse<PageResponse<ProductSummary>>>(`${this.baseUrl}${API_CONFIG.endpoints.search}`, { params })
      .pipe(
        map(response => response.data),
        catchError(this.handleError<PageResponse<ProductSummary>>('searchProducts', { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }))
      );
  }

  /**
   * Get products by category
   * GET /api/products/category/{category}
   */
  getProductsByCategory(category: string, pageParams?: PageParams): Observable<PageResponse<ProductSummary>> {
    let params = new HttpParams({ fromObject: buildPageParams(pageParams || {}) });

    return this.http
      .get<ApiResponse<PageResponse<ProductSummary>>>(
        `${this.baseUrl}${API_CONFIG.endpoints.products}/category/${encodeURIComponent(category)}`, 
        { params }
      )
      .pipe(
        map(response => response.data),
        catchError(this.handleError<PageResponse<ProductSummary>>('getProductsByCategory', { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }))
      );
  }

  /**
   * Get featured products
   * GET /api/products/featured?limit=10
   */
  getFeaturedProducts(limit: number = 10): Observable<ProductSummary[]> {
    const params = new HttpParams().set('limit', limit.toString());
    
    return this.http
      .get<ApiResponse<ProductSummary[]>>(`${this.baseUrl}${API_CONFIG.endpoints.featured}`, { params })
      .pipe(
        map(response => response.data),
        catchError(this.handleError<ProductSummary[]>('getFeaturedProducts', []))
      );
  }

  // ==================== Stock Management ====================

  /**
   * Update product stock
   * POST /api/products/{id}/stock
   */
  updateStock(id: string, stockUpdate: StockUpdateRequest): Observable<Product> {
    return this.http
      .post<ApiResponse<ProductResponse>>(`${this.baseUrl}${API_CONFIG.endpoints.products}/${id}/stock`, stockUpdate)
      .pipe(
        map(response => this.transformProductResponse(response.data)),
        catchError(this.handleError<Product>('updateStock'))
      );
  }

  /**
   * Get low stock products
   * GET /api/products/stock/low
   */
  getLowStockProducts(): Observable<ProductSummary[]> {
    return this.http
      .get<ApiResponse<ProductSummary[]>>(`${this.baseUrl}${API_CONFIG.endpoints.stock}/low`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<ProductSummary[]>('getLowStockProducts', []))
      );
  }

  /**
   * Get out of stock products
   * GET /api/products/stock/out-of-stock
   */
  getOutOfStockProducts(): Observable<ProductSummary[]> {
    return this.http
      .get<ApiResponse<ProductSummary[]>>(`${this.baseUrl}${API_CONFIG.endpoints.stock}/out-of-stock`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<ProductSummary[]>('getOutOfStockProducts', []))
      );
  }

  // ==================== Dashboard & Statistics ====================

  /**
   * Get top selling products
   * GET /api/products/dashboard/top-selling?limit=5
   */
  getTopSellingProducts(limit: number = 5): Observable<ProductDashboardView[]> {
    const params = new HttpParams().set('limit', limit.toString());
    
    return this.http
      .get<ApiResponse<ProductDashboardView[]>>(`${this.baseUrl}${API_CONFIG.endpoints.dashboard}/top-selling`, { params })
      .pipe(
        map(response => response.data),
        catchError(this.handleError<ProductDashboardView[]>('getTopSellingProducts', []))
      );
  }

  /**
   * Get top revenue products
   * GET /api/products/dashboard/top-revenue?limit=5
   */
  getTopRevenueProducts(limit: number = 5): Observable<ProductDashboardView[]> {
    const params = new HttpParams().set('limit', limit.toString());
    
    return this.http
      .get<ApiResponse<ProductDashboardView[]>>(`${this.baseUrl}${API_CONFIG.endpoints.dashboard}/top-revenue`, { params })
      .pipe(
        map(response => response.data),
        catchError(this.handleError<ProductDashboardView[]>('getTopRevenueProducts', []))
      );
  }

  /**
   * Get product statistics
   * GET /api/products/stats
   */
  getProductStats(): Observable<ProductStatsResponse> {
    return this.http
      .get<ApiResponse<ProductStatsResponse>>(`${this.baseUrl}${API_CONFIG.endpoints.stats}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<ProductStatsResponse>('getProductStats', { 
          total: 0, active: 0, lowStock: 0, outOfStock: 0, drafts: 0, revenue: 0, statusCounts: {} 
        }))
      );
  }

  /**
   * Get inventory stats (status counts)
   * Derived from stats endpoint
   */
  getInventoryStats(): Observable<ProductStatusCount[]> {
    return this.getProductStats().pipe(
      map(stats => 
        Object.entries(stats.statusCounts || {}).map(([status, count]) => ({ status, count }))
      )
    );
  }

  // ==================== Validation & Reference ====================

  /**
   * Check if SKU exists
   * GET /api/products/validate/sku?sku={sku}&excludeId={excludeId}
   */
  checkSkuExists(sku: string, excludeId?: string): Observable<boolean> {
    let params = new HttpParams().set('sku', sku);
    if (excludeId) {
      params = params.set('excludeId', excludeId);
    }

    return this.http
      .get<ApiResponse<boolean>>(`${this.baseUrl}${API_CONFIG.endpoints.validate}/sku`, { params })
      .pipe(
        map(response => response.data),
        catchError(this.handleError<boolean>('checkSkuExists', false))
      );
  }

  /**
   * Get all brands
   * GET /api/products/reference/brands
   */
  getAllBrands(): Observable<string[]> {
    return this.http
      .get<ApiResponse<string[]>>(`${this.baseUrl}${API_CONFIG.endpoints.reference}/brands`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<string[]>('getAllBrands', []))
      );
  }

  /**
   * Get all tags
   * GET /api/products/reference/tags
   */
  getAllTags(): Observable<string[]> {
    return this.http
      .get<ApiResponse<string[]>>(`${this.baseUrl}${API_CONFIG.endpoints.reference}/tags`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError<string[]>('getAllTags', []))
      );
  }

  // ==================== Bulk Operations ====================

  /**
   * Bulk delete products
   * POST /api/products/bulk-delete
   */
  bulkDelete(ids: string[]): Observable<void> {
    const request: BulkDeleteRequest = { ids };
    return this.http
      .post<ApiResponse<void>>(`${this.baseUrl}${API_CONFIG.endpoints.bulkDelete}`, request)
      .pipe(
        map(response => response.data),
        tap(() => this.clearCache()),
        catchError(this.handleError<void>('bulkDelete'))
      );
  }

  /**
   * Bulk update status
   * POST /api/products/bulk-status
   */
  bulkUpdateStatus(ids: string[], status: string): Observable<void> {
    const request: BulkStatusUpdateRequest = { ids, status };
    return this.http
      .post<ApiResponse<void>>(`${this.baseUrl}${API_CONFIG.endpoints.bulkStatus}`, request)
      .pipe(
        map(response => response.data),
        tap(() => this.clearCache()),
        catchError(this.handleError<void>('bulkUpdateStatus'))
      );
  }

  // ==================== Helper Methods ====================

  /**
   * Transform ProductResponse to Product (convert date strings to Date objects)
   */
  private transformProductResponse(response: ProductResponse): Product {
    return {
      ...response,
      createdAt: new Date(response.createdAt),
      updatedAt: new Date(response.updatedAt)
    } as Product;
  }

  /**
   * Clear local cache/state
   */
  private clearCache(): void {
    // Clear signals if needed
    this.products.set([]);
  }

  /**
   * Error handler for HTTP requests
   */
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      this.error.set(error?.error?.message || error?.message || 'An error occurred');
      
      // Return safe default result
      return of(result as T);
    };
  }
}
