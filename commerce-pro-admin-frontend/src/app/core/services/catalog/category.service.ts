import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, forkJoin } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  Category,
  CategoryTreeNode,
  CategoryTemplate,
  CategoryStats
} from '../../models/catalog/category.model';
import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

const BASE = 'http://localhost:8080/api/v1/categories';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private http = inject(HttpClient);

  private categories = signal<Category[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly allCategories = computed(() => this.categories());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());

  readonly categoryStats = computed<CategoryStats>(() => {
    const all = this.categories();
    return {
      total: all.length,
      active: all.filter(c => c.isActive).length,
      featured: all.filter(c => c.isFeatured).length,
      topLevel: all.filter(c => !c.parentId).length
    };
  });

  readonly categoryTree = computed<CategoryTreeNode[]>(() =>
    this.buildCategoryTree(null, 0)
  );

  constructor() {
    this.loadCategories();
  }

  // ==================== CRUD Operations ====================

  loadCategories(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<ApiResponse<PageResponse<Category>>>(BASE, {
      params: new HttpParams().set('size', '1000')
    }).pipe(
      map(r => r.data.content),
      catchError(this.handleError('loadCategories', []))
    ).subscribe({
      next: (cats) => {
        this.categories.set(cats);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message);
        this.loading.set(false);
      }
    });
  }

  getCategoriesPaged(pageParams?: PageParams): Observable<PageResponse<Category>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http.get<ApiResponse<PageResponse<Category>>>(BASE, { params })
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getCategoriesPaged',
          { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }))
      );
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<ApiResponse<PageResponse<Category>>>(BASE, {
      params: new HttpParams().set('size', '1000')
    }).pipe(
      map(r => r.data.content),
      catchError(this.handleError('getCategories', []))
    );
  }

  getCategoryTree(): Observable<any[]> {
    return this.http.get<ApiResponse<any[]>>(`${BASE}/tree`)
      .pipe(map(r => r.data), catchError(this.handleError('getCategoryTree', [])));
  }

  getMenuCategories(): Observable<Category[]> {
    return this.http.get<ApiResponse<Category[]>>(`${BASE}/menu`)
      .pipe(map(r => r.data), catchError(this.handleError('getMenuCategories', [])));
  }

  getCategory(id: string): Observable<Category | null> {
    return this.http.get<ApiResponse<Category>>(`${BASE}/${id}`)
      .pipe(map(r => r.data), catchError(this.handleError<Category | null>('getCategory', null)));
  }

  getCategoryBySlug(slug: string): Observable<Category | null> {
    return this.http.get<ApiResponse<Category>>(`${BASE}/slug/${slug}`)
      .pipe(map(r => r.data), catchError(this.handleError<Category | null>('getCategoryBySlug', null)));
  }

  createCategory(category: Partial<Category>): Observable<Category> {
    return this.http.post<ApiResponse<Category>>(BASE, this.toRequest(category))
      .pipe(
        map(r => r.data),
        tap(() => this.loadCategories()),
        catchError(this.handleError<Category>('createCategory'))
      );
  }

  updateCategory(id: string, updates: Partial<Category>): Observable<Category> {
    return this.http.put<ApiResponse<Category>>(`${BASE}/${id}`, this.toRequest(updates))
      .pipe(
        map(r => r.data),
        tap(() => this.loadCategories()),
        catchError(this.handleError<Category>('updateCategory'))
      );
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<ApiResponse<string>>(`${BASE}/${id}`)
      .pipe(
        map(() => void 0),
        tap(() => this.loadCategories()),
        catchError(this.handleError<void>('deleteCategory'))
      );
  }

  restoreCategory(id: string): Observable<Category> {
    return this.http.post<ApiResponse<Category>>(`${BASE}/${id}/restore`, {})
      .pipe(
        map(r => r.data),
        tap(() => this.loadCategories()),
        catchError(this.handleError<Category>('restoreCategory'))
      );
  }

  getSubcategories(id: string): Observable<Category[]> {
    return this.http.get<ApiResponse<Category[]>>(`${BASE}/${id}/subcategories`)
      .pipe(map(r => r.data), catchError(this.handleError('getSubcategories', [])));
  }

  moveCategory(id: string, newParentId: string | null): Observable<Category> {
    return this.http.post<ApiResponse<Category>>(`${BASE}/${id}/move`, { newParentId })
      .pipe(
        map(r => r.data),
        tap(() => this.loadCategories()),
        catchError(this.handleError<Category>('moveCategory'))
      );
  }

  reorderCategory(id: string, newSortOrder: number): Observable<void> {
    return this.http.post<ApiResponse<string>>(`${BASE}/${id}/reorder`, { newSortOrder })
      .pipe(
        map(() => void 0),
        tap(() => this.loadCategories()),
        catchError(this.handleError<void>('reorderCategory'))
      );
  }

  getCategoryPath(id: string): Observable<Category[]> {
    return this.http.get<ApiResponse<Category[]>>(`${BASE}/${id}/path`)
      .pipe(map(r => r.data), catchError(this.handleError('getCategoryPath', [])));
  }

  toggleActive(id: string, active: boolean): Observable<void> {
    return this.http.patch<ApiResponse<string>>(`${BASE}/${id}/active`, null, {
      params: new HttpParams().set('active', active)
    }).pipe(
      map(() => void 0),
      tap(() => this.loadCategories()),
      catchError(this.handleError<void>('toggleActive'))
    );
  }

  getStatistics(): Observable<Record<string, number>> {
    return this.http.get<ApiResponse<Record<string, number>>>(`${BASE}/statistics`)
      .pipe(map(r => r.data), catchError(this.handleError('getStatistics', {})));
  }

  getCustomFields(id: string): Observable<Record<string, any>> {
    return this.http.get<ApiResponse<Record<string, any>>>(`${BASE}/${id}/custom-fields`)
      .pipe(map(r => r.data), catchError(this.handleError('getCustomFields', {})));
  }

  updateCustomFields(id: string, fields: Record<string, any>): Observable<void> {
    return this.http.put<ApiResponse<string>>(`${BASE}/${id}/custom-fields`, { fields })
      .pipe(
        map(() => void 0),
        catchError(this.handleError<void>('updateCustomFields'))
      );
  }

  bulkUpdateStatus(ids: string[], active: boolean): Observable<void> {
    if (ids.length === 0) return of(void 0);
    return forkJoin(ids.map(id => this.toggleActive(id, active))).pipe(
      map(() => void 0),
      catchError(this.handleError<void>('bulkUpdateStatus'))
    );
  }

  bulkDelete(ids: string[]): Observable<void> {
    if (ids.length === 0) return of(void 0);
    return forkJoin(ids.map(id => this.deleteCategory(id))).pipe(
      map(() => void 0),
      catchError(this.handleError<void>('bulkDelete'))
    );
  }

  // ==================== Tree Operations ====================

  private buildCategoryTree(parentId: string | null, level: number): CategoryTreeNode[] {
    return this.categories()
      .filter(c => c.parentId === parentId)
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(c => ({
        ...c,
        level,
        isExpanded: false,
        isEditing: false,
        subcategories: this.buildCategoryTree(c.id, level + 1)
      }));
  }

  getFlatCategories(): Observable<Category[]> {
    return this.getCategories().pipe(
      map(cats => this.flattenCategories(cats))
    );
  }

  getAvailableParents(currentId?: string): Observable<Category[]> {
    return this.getCategories().pipe(
      map(cats => {
        if (!currentId) return cats;
        const childrenIds = new Set<string>();
        const collectChildren = (parentId: string) => {
          cats.filter(c => c.parentId === parentId).forEach(c => {
            childrenIds.add(c.id);
            collectChildren(c.id);
          });
        };
        collectChildren(currentId);
        return cats.filter(c => c.id !== currentId && !childrenIds.has(c.id));
      })
    );
  }

  // ==================== Templates (client-side) ====================

  getTemplates(): Observable<CategoryTemplate[]> {
    return of([
      { id: 'electronics', name: 'Electronics', description: 'For gadgets and devices', icon: 'laptop', color: '#6366f1', defaultFields: { icon: 'laptop', color: '#6366f1', isFeatured: true } },
      { id: 'fashion', name: 'Fashion', description: 'For clothing and apparel', icon: 'shirt', color: '#f59e0b', defaultFields: { icon: 'shirt', color: '#f59e0b', isFeatured: true } },
      { id: 'home', name: 'Home & Garden', description: 'For home and living', icon: 'house', color: '#10b981', defaultFields: { icon: 'house', color: '#10b981', isFeatured: false } },
      { id: 'sports', name: 'Sports', description: 'For sports and fitness', icon: 'bicycle', color: '#f43f5e', defaultFields: { icon: 'bicycle', color: '#f43f5e', isFeatured: false } }
    ]);
  }

  // ==================== Helper Methods ====================

  retry(): void {
    this.loadCategories();
  }

  private toRequest(category: Partial<Category>): any {
    return {
      name: category.name,
      slug: category.slug,
      description: category.description,
      parentId: category.parentId,
      imageUrl: category.image,
      isActive: category.isActive,
      sortOrder: category.sortOrder,
      showInMenu: category.showInMenu,
      seoTitle: category.seoTitle,
      seoDescription: category.seoDescription,
      metaKeywords: category.metaKeywords,
      customFields: category.customFields
    };
  }

  private flattenCategories(categories: Category[], parentId: string | null = null): Category[] {
    const result: Category[] = [];
    categories
      .filter(c => c.parentId === parentId)
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .forEach(c => {
        result.push(c);
        result.push(...this.flattenCategories(categories, c.id));
      });
    return result;
  }

  private handleError<T>(op = 'operation', result?: T) {
    return (err: any): Observable<T> => {
      console.error(`[CategoryService] ${op}:`, err);
      this.error.set(err?.error?.message ?? err?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
