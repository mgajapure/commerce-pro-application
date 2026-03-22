// src/app/core/services/attribute.service.ts
// Attribute service wired to real backend API

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  Attribute,
  AttributeType,
  AttributeOption,
  AttributeStats
} from '../../models/catalog/attribute.model';
import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

const BASE = 'http://localhost:8080/api/v1/attributes';

@Injectable({ providedIn: 'root' })
export class AttributeService {
  private http = inject(HttpClient);

  private attributes = signal<Attribute[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly allAttributes = computed(() => this.attributes());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());

  readonly attributeStats = computed<AttributeStats>(() => {
    const all = this.attributes();
    return {
      total: all.length,
      active: all.filter(a => a.isActive).length,
      filterable: all.filter(a => a.isFilterable).length,
      variant: all.filter(a => a.isVariant).length,
      required: all.filter(a => a.isRequired).length,
      withOptions: all.filter(a => a.options && a.options.length > 0).length
    };
  });

  constructor() {
    this.loadAttributes();
  }

  // ==================== CRUD Operations ====================

  loadAttributes(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<ApiResponse<Attribute[]>>(`${BASE}/all`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError('loadAttributes', []))
      )
      .subscribe({
        next: (attrs) => {
          this.attributes.set(attrs);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.message);
          this.loading.set(false);
        }
      });
  }

  getAttributesPaged(pageParams?: PageParams): Observable<PageResponse<Attribute>> {
    const params = new HttpParams({ fromObject: buildPageParams(pageParams ?? {}) });
    return this.http.get<ApiResponse<PageResponse<Attribute>>>(BASE, { params }).pipe(
      map(r => r.data),
      catchError(this.handleError('getAttributesPaged', {
        content: [], page: 0, size: 20,
        totalElements: 0, totalPages: 0,
        first: true, last: true, empty: true
      }))
    );
  }

  getAttributes(): Observable<Attribute[]> {
    return this.http.get<ApiResponse<Attribute[]>>(`${BASE}/all`).pipe(
      map(r => r.data),
      catchError(this.handleError('getAttributes', []))
    );
  }

  getAttribute(id: string): Observable<Attribute | null> {
    return this.http.get<ApiResponse<Attribute>>(`${BASE}/${id}`).pipe(
      map(r => r.data),
      catchError(this.handleError<Attribute | null>('getAttribute', null))
    );
  }

  createAttribute(attribute: Partial<Attribute>): Observable<Attribute> {
    return this.http.post<ApiResponse<Attribute>>(BASE, attribute).pipe(
      map(r => r.data),
      tap(() => this.loadAttributes()),
      catchError(this.handleError<Attribute>('createAttribute'))
    );
  }

  updateAttribute(id: string, updates: Partial<Attribute>): Observable<Attribute> {
    return this.http.put<ApiResponse<Attribute>>(`${BASE}/${id}`, updates).pipe(
      map(r => r.data),
      tap(() => this.loadAttributes()),
      catchError(this.handleError<Attribute>('updateAttribute'))
    );
  }

  deleteAttribute(id: string): Observable<void> {
    return this.http.delete<ApiResponse<string>>(`${BASE}/${id}`).pipe(
      map(() => void 0),
      tap(() => this.loadAttributes()),
      catchError(this.handleError<void>('deleteAttribute'))
    );
  }

  toggleActive(id: string, active: boolean): Observable<void> {
    return this.http.patch<ApiResponse<string>>(`${BASE}/${id}/active`, null, {
      params: new HttpParams().set('active', active)
    }).pipe(
      map(() => void 0),
      tap(() => this.loadAttributes()),
      catchError(this.handleError<void>('toggleActive'))
    );
  }

  // ==================== Options Management ====================

  addOption(attributeId: string, option: Partial<AttributeOption>): Observable<Attribute> {
    const attribute = this.attributes().find(a => a.id === attributeId);
    if (!attribute) {
      return this.handleError<Attribute>('addOption')
        (new Error('Attribute not found'));
    }
    const updatedOptions = [
      ...attribute.options,
      {
        id: '',
        label: option.label || '',
        value: option.value || '',
        sortOrder: attribute.options.length
      } as AttributeOption
    ];
    return this.updateAttribute(attributeId, { options: updatedOptions });
  }

  removeOption(attributeId: string, optionId: string): Observable<Attribute> {
    const attribute = this.attributes().find(a => a.id === attributeId);
    if (!attribute) {
      return this.handleError<Attribute>('removeOption')
        (new Error('Attribute not found'));
    }
    return this.updateAttribute(attributeId, {
      options: attribute.options.filter(o => o.id !== optionId)
    });
  }

  reorderOptions(attributeId: string, optionIds: string[]): Observable<Attribute> {
    const attribute = this.attributes().find(a => a.id === attributeId);
    if (!attribute) {
      return this.handleError<Attribute>('reorderOptions')
        (new Error('Attribute not found'));
    }
    const optionMap = new Map(attribute.options.map(o => [o.id, o]));
    const reordered = optionIds
      .map((id, index) => ({ ...optionMap.get(id)!, sortOrder: index }))
      .filter(Boolean);
    return this.updateAttribute(attributeId, { options: reordered });
  }

  // ==================== Bulk Operations ====================

  bulkUpdateStatus(ids: string[], isActive: boolean): Observable<void> {
    const requests = ids.map(id => this.toggleActive(id, isActive));
    // Fire all PATCH calls sequentially via a reduce, but return void immediately
    // after triggering them; signal reload handled by each toggleActive tap.
    requests.forEach(r => r.subscribe());
    return of(void 0);
  }

  bulkDelete(ids: string[]): Observable<void> {
    ids.forEach(id =>
      this.http.delete<ApiResponse<string>>(`${BASE}/${id}`)
        .pipe(catchError(this.handleError<void>(`bulkDelete:${id}`)))
        .subscribe()
    );
    // Reload once after all deletes are fired
    setTimeout(() => this.loadAttributes(), 0);
    return of(void 0);
  }

  bulkSetFilterable(ids: string[], isFilterable: boolean): Observable<void> {
    ids.forEach(id => {
      const attr = this.attributes().find(a => a.id === id);
      if (attr) {
        this.http.put<ApiResponse<Attribute>>(`${BASE}/${id}`, { ...attr, isFilterable })
          .pipe(catchError(this.handleError<Attribute>(`bulkSetFilterable:${id}`)))
          .subscribe();
      }
    });
    setTimeout(() => this.loadAttributes(), 0);
    return of(void 0);
  }

  // ==================== Type Helpers ====================

  getAttributeTypes(): { value: AttributeType; label: string; icon: string }[] {
    return [
      { value: 'select',      label: 'Select',       icon: 'menu-down'    },
      { value: 'multiselect', label: 'Multi-Select',  icon: 'check2-square'},
      { value: 'text',        label: 'Text',          icon: 'type'         },
      { value: 'textarea',    label: 'Text Area',     icon: 'textarea'     },
      { value: 'color',       label: 'Color Swatch',  icon: 'palette'      },
      { value: 'image',       label: 'Image Swatch',  icon: 'image'        },
      { value: 'boolean',     label: 'Yes/No',        icon: 'toggle-on'    },
      { value: 'number',      label: 'Number',        icon: '123'          },
      { value: 'date',        label: 'Date',          icon: 'calendar'     }
    ];
  }

  // ==================== Misc ====================

  retry(): void {
    this.loadAttributes();
  }

  // ==================== Private Helpers ====================

  private handleError<T>(op = 'operation', result?: T) {
    return (err: any): Observable<T> => {
      console.error(`[AttributeService] ${op}:`, err);
      this.error.set(err?.error?.message ?? err?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
