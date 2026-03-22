// src/app/core/services/collection.service.ts
// Collection service wired to real backend API

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { Collection, CollectionStats, CollectionCondition } from '../../models/catalog/collection.model';
import {
  ApiResponse,
  PageResponse,
  PageParams,
  buildPageParams
} from '../../models/common';

const BASE = 'http://localhost:8080/api/v1/collections';

// ── Backend DTO shapes ────────────────────────────────────────────────────────

interface CollectionListResponse {
  id: string;
  name: string;
  slug: string;
  type: string;
  status: string;
  imageUrl: string;
  isFeatured: boolean;
  sortOrder: number;
  productCount: number;
  createdAt: string;
}

interface CollectionResponse {
  id: string;
  name: string;
  slug: string;
  description: string;
  type: string;
  status: string;
  imageUrl: string;
  conditions: string;          // JSON string stored by backend
  productIds: string[];
  sortOrder: number;
  isFeatured: boolean;
  startDate: string;
  endDate: string;
  createdAt: string;
  updatedAt: string;
}

interface CollectionStatsResponse {
  totalCollections: number;
  activeCollections: number;
  draftCollections: number;
  archivedCollections: number;
  manualCollections: number;
  automatedCollections: number;
  featuredCollections: number;
}

interface CollectionRequest {
  name: string;
  slug: string;
  description?: string;
  type?: string;
  status?: string;
  imageUrl?: string;
  conditions?: string;
  productIds?: string[];
  sortOrder?: number;
  isFeatured?: boolean;
  startDate?: string;
  endDate?: string;
}

// ── Mappers ───────────────────────────────────────────────────────────────────

function mapListResponseToCollection(dto: CollectionListResponse): Collection {
  return {
    id: dto.id,
    name: dto.name,
    slug: dto.slug,
    type: dto.type === 'automated' ? 'automated' : 'manual',
    isActive: dto.status === 'ACTIVE',
    isFeatured: dto.isFeatured ?? false,
    sortOrder: dto.sortOrder ?? 0,
    productCount: dto.productCount ?? 0,
    createdAt: new Date(dto.createdAt),
    updatedAt: new Date(dto.createdAt) // ListResponse has no updatedAt
  };
}

function mapResponseToCollection(dto: CollectionResponse): Collection {
  let parsedConditions: CollectionCondition[] | undefined;
  if (dto.conditions) {
    try {
      parsedConditions = JSON.parse(dto.conditions);
    } catch {
      parsedConditions = undefined;
    }
  }

  return {
    id: dto.id,
    name: dto.name,
    slug: dto.slug,
    description: dto.description,
    image: dto.imageUrl,
    type: dto.type === 'automated' ? 'automated' : 'manual',
    isActive: dto.status === 'ACTIVE',
    isFeatured: dto.isFeatured ?? false,
    sortOrder: dto.sortOrder ?? 0,
    productCount: dto.productIds?.length ?? 0,
    productIds: dto.productIds,
    conditions: parsedConditions,
    createdAt: new Date(dto.createdAt),
    updatedAt: new Date(dto.updatedAt)
  };
}

function mapCollectionToRequest(collection: Partial<Collection>): CollectionRequest {
  return {
    name: collection.name!,
    slug: collection.slug!,
    description: collection.description,
    type: collection.type,
    status: collection.isActive ? 'ACTIVE' : 'DRAFT',
    imageUrl: collection.image,
    conditions: collection.conditions ? JSON.stringify(collection.conditions) : undefined,
    productIds: collection.productIds,
    sortOrder: collection.sortOrder,
    isFeatured: collection.isFeatured
  };
}

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({
  providedIn: 'root'
})
export class CollectionService {
  private readonly http = inject(HttpClient);

  private collections = signal<Collection[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly allCollections = computed(() => this.collections());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());

  readonly collectionStats = computed<CollectionStats>(() => {
    const all = this.collections();
    return {
      total: all.length,
      manual: all.filter(c => c.type === 'manual').length,
      automated: all.filter(c => c.type === 'automated').length,
      active: all.filter(c => c.isActive).length
    };
  });

  constructor() {
    this.loadCollections();
  }

  // ── State loader ────────────────────────────────────────────────────────────

  loadCollections(params: PageParams = { page: 0, size: 100, sort: 'createdAt', direction: 'desc' }): void {
    this.loading.set(true);
    this.error.set(null);

    const httpParams = new HttpParams({ fromObject: buildPageParams(params) });

    this.http
      .get<ApiResponse<PageResponse<CollectionListResponse>>>(BASE, { params: httpParams })
      .pipe(
        map(r => r.data.content.map(mapListResponseToCollection)),
        catchError(this.handleError('loadCollections', [] as Collection[]))
      )
      .subscribe({
        next: colls => {
          this.collections.set(colls);
          this.loading.set(false);
        },
        error: err => {
          this.error.set(err.message);
          this.loading.set(false);
        }
      });
  }

  // ── Queries ─────────────────────────────────────────────────────────────────

  getCollections(params: PageParams = { page: 0, size: 100, sort: 'createdAt', direction: 'desc' }): Observable<Collection[]> {
    const httpParams = new HttpParams({ fromObject: buildPageParams(params) });
    return this.http
      .get<ApiResponse<PageResponse<CollectionListResponse>>>(BASE, { params: httpParams })
      .pipe(
        map(r => r.data.content.map(mapListResponseToCollection)),
        catchError(this.handleError('getCollections', [] as Collection[]))
      );
  }

  getCollectionsPaged(params: PageParams = {}): Observable<PageResponse<Collection>> {
    const httpParams = new HttpParams({ fromObject: buildPageParams(params) });
    return this.http
      .get<ApiResponse<PageResponse<CollectionListResponse>>>(BASE, { params: httpParams })
      .pipe(
        map(r => ({
          ...r.data,
          content: r.data.content.map(mapListResponseToCollection)
        })),
        catchError(this.handleError('getCollectionsPaged', {
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
          empty: true
        } as PageResponse<Collection>))
      );
  }

  getCollection(id: string): Observable<Collection | null> {
    return this.http
      .get<ApiResponse<CollectionResponse>>(`${BASE}/${id}`)
      .pipe(
        map(r => mapResponseToCollection(r.data)),
        catchError(this.handleError('getCollection', null as Collection | null))
      );
  }

  getCollectionBySlug(slug: string): Observable<Collection | null> {
    return this.http
      .get<ApiResponse<CollectionResponse>>(`${BASE}/slug/${slug}`)
      .pipe(
        map(r => mapResponseToCollection(r.data)),
        catchError(this.handleError('getCollectionBySlug', null as Collection | null))
      );
  }

  getFeaturedCollections(): Observable<Collection[]> {
    return this.http
      .get<ApiResponse<CollectionListResponse[]>>(`${BASE}/featured`)
      .pipe(
        map(r => r.data.map(mapListResponseToCollection)),
        catchError(this.handleError('getFeaturedCollections', [] as Collection[]))
      );
  }

  getStats(): Observable<CollectionStatsResponse> {
    return this.http
      .get<ApiResponse<CollectionStatsResponse>>(`${BASE}/stats`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getStats', {} as CollectionStatsResponse))
      );
  }

  // ── Mutations ───────────────────────────────────────────────────────────────

  createCollection(collection: Partial<Collection>): Observable<Collection> {
    const body = mapCollectionToRequest(collection);
    return this.http
      .post<ApiResponse<CollectionResponse>>(BASE, body)
      .pipe(
        map(r => mapResponseToCollection(r.data)),
        tap(() => this.loadCollections()),
        catchError(this.handleError('createCollection', null as unknown as Collection))
      );
  }

  updateCollection(id: string, updates: Partial<Collection>): Observable<Collection> {
    const body = mapCollectionToRequest(updates);
    return this.http
      .put<ApiResponse<CollectionResponse>>(`${BASE}/${id}`, body)
      .pipe(
        map(r => mapResponseToCollection(r.data)),
        tap(() => this.loadCollections()),
        catchError(this.handleError('updateCollection', null as unknown as Collection))
      );
  }

  deleteCollection(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<string>>(`${BASE}/${id}`)
      .pipe(
        map(() => void 0),
        tap(() => this.loadCollections()),
        catchError(this.handleError('deleteCollection', void 0))
      );
  }

  // ── Product management ──────────────────────────────────────────────────────

  addProductToCollection(collectionId: string, productId: string): Observable<Collection> {
    return this.http
      .post<ApiResponse<CollectionResponse>>(`${BASE}/${collectionId}/products`, { productIds: [productId] })
      .pipe(
        map(r => mapResponseToCollection(r.data)),
        tap(() => this.loadCollections()),
        catchError(this.handleError('addProductToCollection', null as unknown as Collection))
      );
  }

  removeProductFromCollection(collectionId: string, productId: string): Observable<Collection> {
    return this.http
      .delete<ApiResponse<CollectionResponse>>(`${BASE}/${collectionId}/products`, {
        body: { productIds: [productId] }
      })
      .pipe(
        map(r => mapResponseToCollection(r.data)),
        tap(() => this.loadCollections()),
        catchError(this.handleError('removeProductFromCollection', null as unknown as Collection))
      );
  }

  addProductsToCollection(collectionId: string, productIds: string[]): Observable<Collection> {
    return this.http
      .post<ApiResponse<CollectionResponse>>(`${BASE}/${collectionId}/products`, { productIds })
      .pipe(
        map(r => mapResponseToCollection(r.data)),
        tap(() => this.loadCollections()),
        catchError(this.handleError('addProductsToCollection', null as unknown as Collection))
      );
  }

  removeProductsFromCollection(collectionId: string, productIds: string[]): Observable<Collection> {
    return this.http
      .delete<ApiResponse<CollectionResponse>>(`${BASE}/${collectionId}/products`, {
        body: { productIds }
      })
      .pipe(
        map(r => mapResponseToCollection(r.data)),
        tap(() => this.loadCollections()),
        catchError(this.handleError('removeProductsFromCollection', null as unknown as Collection))
      );
  }

  // ── Condition helpers (local convenience — delegate to updateCollection) ────

  addCondition(collectionId: string, condition: Partial<CollectionCondition>): Observable<Collection> {
    return this.getCollection(collectionId).pipe(
      switchMap(collection => {
        if (!collection) throw new Error('Collection not found');
        const newCondition: CollectionCondition = {
          id: `cond_${Date.now()}`,
          field: condition.field!,
          relation: condition.relation!,
          value: condition.value!
        };
        const conditions = [...(collection.conditions || []), newCondition];
        return this.updateCollection(collectionId, { conditions });
      }),
      catchError(this.handleError('addCondition', null as unknown as Collection))
    );
  }

  removeCondition(collectionId: string, conditionId: string): Observable<Collection> {
    return this.getCollection(collectionId).pipe(
      switchMap(collection => {
        if (!collection) throw new Error('Collection not found');
        const conditions = (collection.conditions || []).filter(c => c.id !== conditionId);
        return this.updateCollection(collectionId, { conditions });
      }),
      catchError(this.handleError('removeCondition', null as unknown as Collection))
    );
  }

  // ── Error handler ───────────────────────────────────────────────────────────

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error.message ?? error);
      this.error.set(`${operation} failed: ${error.message ?? 'Unknown error'}`);
      return of(result as T);
    };
  }
}
