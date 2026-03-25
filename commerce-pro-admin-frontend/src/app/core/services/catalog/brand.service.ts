import { API_HOST } from '../../config/api-config';
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { Brand, BrandStats } from '../../models/catalog/brand.model';
import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

const BASE = `${API_HOST}/api/v1/brands`;

@Injectable({ providedIn: 'root' })
export class BrandService {
  private http = inject(HttpClient);

  private brands = signal<Brand[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly allBrands = computed(() => this.brands());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());

  readonly brandStats = computed<BrandStats>(() => {
    const all = this.brands();
    return {
      total: all.length,
      active: all.filter(b => b.isActive).length,
      featured: all.filter(b => b.isFeatured).length
    };
  });

  constructor() {
    this.loadBrands();
  }

  loadBrands(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<ApiResponse<Brand[]>>(`${BASE}/all`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError('loadBrands', []))
      )
      .subscribe({
        next: (brands) => {
          this.brands.set(brands);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.message);
          this.loading.set(false);
        }
      });
  }

  getBrandsPaged(pageParams?: PageParams): Observable<PageResponse<Brand>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http.get<ApiResponse<PageResponse<Brand>>>(BASE, { params })
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getBrandsPaged',
          { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }))
      );
  }

  getBrands(): Observable<Brand[]> {
    return this.http.get<ApiResponse<Brand[]>>(`${BASE}/all`)
      .pipe(map(r => r.data), catchError(this.handleError('getBrands', [])));
  }

  getFeaturedBrands(): Observable<Brand[]> {
    return this.http.get<ApiResponse<Brand[]>>(`${BASE}/featured`)
      .pipe(map(r => r.data), catchError(this.handleError('getFeaturedBrands', [])));
  }

  getBrand(id: string): Observable<Brand | null> {
    return this.http.get<ApiResponse<Brand>>(`${BASE}/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<Brand | null>('getBrand', null)));
  }

  getBrandBySlug(slug: string): Observable<Brand | null> {
    return this.http.get<ApiResponse<Brand>>(`${BASE}/slug/${slug}`)
      .pipe(map(r => r.data), catchError(this.handleError<Brand | null>('getBrandBySlug', null)));
  }

  createBrand(brand: Partial<Brand>): Observable<Brand> {
    return this.http.post<ApiResponse<Brand>>(BASE, brand)
      .pipe(
        map(r => r.data),
        tap(() => this.loadBrands()),
        catchError(this.handleError<Brand>('createBrand'))
      );
  }

  updateBrand(id: string, updates: Partial<Brand>): Observable<Brand> {
    return this.http.put<ApiResponse<Brand>>(`${BASE}/${id}`, updates)
      .pipe(
        map(r => r.data),
        tap(() => this.loadBrands()),
        catchError(this.handleError<Brand>('updateBrand'))
      );
  }

  deleteBrand(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${BASE}/${id}`)
      .pipe(
        map(() => void 0),
        tap(() => this.loadBrands()),
        catchError(this.handleError<void>('deleteBrand'))
      );
  }

  toggleActive(id: string, active: boolean): Observable<void> {
    return this.http.patch<ApiResponse<void>>(`${BASE}/${id}/active`, null, {
      params: new HttpParams().set('active', active)
    }).pipe(
      map(() => void 0),
      tap(() => this.loadBrands()),
      catchError(this.handleError<void>('toggleActive'))
    );
  }

  toggleFeatured(id: string, featured: boolean): Observable<void> {
    return this.http.patch<ApiResponse<void>>(`${BASE}/${id}/featured`, null, {
      params: new HttpParams().set('featured', featured)
    }).pipe(
      map(() => void 0),
      tap(() => this.loadBrands()),
      catchError(this.handleError<void>('toggleFeatured'))
    );
  }

  retry(): void {
    this.loadBrands();
  }

  private handleError<T>(op = 'operation', result?: T) {
    return (err: any): Observable<T> => {
      console.error(`[BrandService] ${op}:`, err);
      this.error.set(err?.error?.message ?? err?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
