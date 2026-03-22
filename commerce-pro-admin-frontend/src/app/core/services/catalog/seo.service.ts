// src/app/core/services/seo.service.ts
// SEO service wired to real Spring Boot backend API

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  SeoMetadata,
  SeoTemplate,
  SeoAuditResult,
  SeoStats
} from '../../models/catalog/seo.model';
import { ApiResponse, PageResponse, PageParams, buildPageParams } from '../../models/common';

// ── Backend DTO shapes (matching SeoDto.java) ──────────────────────────────

export interface SeoListResponse {
  id: string;
  entityType: string;
  entityId: string;
  metaTitle: string;
  metaDescription: string;
  score: number;
  updatedAt: string;
}

export interface SeoResponse {
  id: string;
  entityType: string;
  entityId: string;
  metaTitle: string;
  metaDescription: string;
  metaKeywords: string;
  canonicalUrl: string;
  ogTitle: string;
  ogDescription: string;
  ogImage: string;
  robotsDirective: string;
  slug: string;
  customHeadHtml: string;
  score: number;
  createdAt: string;
  updatedAt: string;
}

export interface SeoRequest {
  metaTitle?: string;
  metaDescription?: string;
  metaKeywords?: string;
  canonicalUrl?: string;
  ogTitle?: string;
  ogDescription?: string;
  ogImage?: string;
  robotsDirective?: string;
  slug?: string;
  customHeadHtml?: string;
}

export interface SeoAuditResponse {
  score: number;
  issues: string[];
  warnings: string[];
  passed: string[];
  suggestions: Record<string, string>;
}

export interface SeoStatsResponse {
  totalEntries: number;
  averageScore: number;
  entriesWithScore80Plus: number;
  entriesWithScoreBelow50: number;
  entriesByEntityType: Record<string, number>;
}

// ── Service ────────────────────────────────────────────────────────────────

@Injectable({
  providedIn: 'root'
})
export class SeoService {
  private readonly BASE_URL = 'http://localhost:8080/api/v1/seo';
  private readonly http = inject(HttpClient);

  // ── Signals ──────────────────────────────────────────────────────────────

  private templates = signal<SeoTemplate[]>([]);
  private seoEntries = signal<SeoListResponse[]>([]);
  private stats = signal<SeoStatsResponse | null>(null);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  // ── Computed (public) ─────────────────────────────────────────────────────

  readonly allTemplates = computed(() => this.templates());
  readonly allSeoEntries = computed(() => this.seoEntries());
  readonly seoStats = computed(() => this.stats());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());

  // ── Initialisation ────────────────────────────────────────────────────────

  constructor() {
    this.loadSeoEntries();
    this.loadStats();
    this.loadTemplates();
  }

  // ── Paginated list ────────────────────────────────────────────────────────

  /**
   * GET /api/v1/seo?page=&size=&sort=
   * Loads all SEO entries (paged) and stores them in the signal.
   */
  loadSeoEntries(params: PageParams = {}): void {
    this.loading.set(true);
    this.error.set(null);

    const httpParams = new HttpParams({ fromObject: buildPageParams(params) });

    this.http
      .get<ApiResponse<PageResponse<SeoListResponse>>>(this.BASE_URL, { params: httpParams })
      .pipe(
        map(r => r.data.content),
        catchError(this.handleError('loadSeoEntries', [] as SeoListResponse[]))
      )
      .subscribe(entries => {
        this.seoEntries.set(entries);
        this.loading.set(false);
      });
  }

  getSeoEntries(params: PageParams = {}): Observable<PageResponse<SeoListResponse>> {
    const httpParams = new HttpParams({ fromObject: buildPageParams(params) });
    return this.http
      .get<ApiResponse<PageResponse<SeoListResponse>>>(this.BASE_URL, { params: httpParams })
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getSeoEntries', {
          content: [], page: 0, size: 20, totalElements: 0,
          totalPages: 0, first: true, last: true, empty: true
        } as PageResponse<SeoListResponse>))
      );
  }

  // ── Entity-level SEO ──────────────────────────────────────────────────────

  /**
   * GET /api/v1/seo/entity/{entityType}/{entityId}
   */
  getSeoForEntity(entityType: string, entityId: string): Observable<SeoResponse> {
    return this.http
      .get<ApiResponse<SeoResponse>>(`${this.BASE_URL}/entity/${entityType}/${entityId}`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getSeoForEntity', null as unknown as SeoResponse))
      );
  }

  /**
   * PUT /api/v1/seo/entity/{entityType}/{entityId}
   */
  upsertSeoForEntity(
    entityType: string,
    entityId: string,
    request: SeoRequest
  ): Observable<SeoResponse> {
    return this.http
      .put<ApiResponse<SeoResponse>>(`${this.BASE_URL}/entity/${entityType}/${entityId}`, request)
      .pipe(
        map(r => r.data),
        tap(() => this.loadSeoEntries()),
        catchError(this.handleError('upsertSeoForEntity', null as unknown as SeoResponse))
      );
  }

  /**
   * DELETE /api/v1/seo/entity/{entityType}/{entityId}
   */
  deleteSeoForEntity(entityType: string, entityId: string): Observable<string> {
    return this.http
      .delete<ApiResponse<string>>(`${this.BASE_URL}/entity/${entityType}/${entityId}`)
      .pipe(
        map(r => r.data),
        tap(() => this.loadSeoEntries()),
        catchError(this.handleError('deleteSeoForEntity', ''))
      );
  }

  // ── Audit ─────────────────────────────────────────────────────────────────

  /**
   * POST /api/v1/seo/audit/{entityType}/{entityId}
   */
  auditEntity(entityType: string, entityId: string): Observable<SeoAuditResponse> {
    return this.http
      .post<ApiResponse<SeoAuditResponse>>(`${this.BASE_URL}/audit/${entityType}/${entityId}`, {})
      .pipe(
        map(r => r.data),
        catchError(this.handleError('auditEntity', null as unknown as SeoAuditResponse))
      );
  }

  // ── Stats ─────────────────────────────────────────────────────────────────

  /**
   * GET /api/v1/seo/stats
   * Fetches stats and stores them in the signal.
   */
  loadStats(): void {
    this.http
      .get<ApiResponse<SeoStatsResponse>>(`${this.BASE_URL}/stats`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError('loadStats', null as unknown as SeoStatsResponse))
      )
      .subscribe(data => this.stats.set(data));
  }

  getStats(): Observable<SeoStatsResponse> {
    return this.http
      .get<ApiResponse<SeoStatsResponse>>(`${this.BASE_URL}/stats`)
      .pipe(
        map(r => r.data),
        catchError(this.handleError('getStats', null as unknown as SeoStatsResponse))
      );
  }

  // ── Templates (client-side only – no backend endpoint) ───────────────────

  loadTemplates(): void {
    const defaultTemplates: SeoTemplate[] = [
      {
        id: 'default_product',
        name: 'Default Product',
        entityType: 'product',
        titlePattern: '{name} - {brand} | Store Name',
        descriptionPattern: 'Buy {name} from {brand}. {shortDescription}',
        isDefault: true
      },
      {
        id: 'default_category',
        name: 'Default Category',
        entityType: 'category',
        titlePattern: '{name} - Shop {name} Online | Store Name',
        descriptionPattern: 'Browse our collection of {name}. {description}',
        isDefault: true
      }
    ];
    this.templates.set(defaultTemplates);
  }

  getTemplates(): Observable<SeoTemplate[]> {
    return of(this.templates());
  }

  getTemplate(id: string): Observable<SeoTemplate | null> {
    return of(this.templates().find(t => t.id === id) ?? null);
  }

  createTemplate(template: Partial<SeoTemplate>): Observable<SeoTemplate> {
    const newTemplate: SeoTemplate = { ...(template as SeoTemplate), id: `template_${Date.now()}` };
    this.templates.update(current => [...current, newTemplate]);
    return of(newTemplate);
  }

  updateTemplate(id: string, updates: Partial<SeoTemplate>): Observable<SeoTemplate | null> {
    this.templates.update(current =>
      current.map(t => (t.id === id ? { ...t, ...updates } : t))
    );
    return of(this.templates().find(t => t.id === id) ?? null);
  }

  deleteTemplate(id: string): Observable<void> {
    this.templates.update(current => current.filter(t => t.id !== id));
    return of(void 0);
  }

  // ── SEO Generation (client-side) ──────────────────────────────────────────

  generateMetadata(
    template: SeoTemplate,
    entityData: Record<string, string>
  ): SeoMetadata {
    let title = template.titlePattern;
    let description = template.descriptionPattern;

    Object.entries(entityData).forEach(([key, value]) => {
      title = title.replace(new RegExp(`{${key}}`, 'g'), value);
      description = description.replace(new RegExp(`{${key}}`, 'g'), value);
    });

    return {
      title,
      description,
      ogTitle: title,
      ogDescription: description,
      twitterTitle: title,
      twitterDescription: description
    };
  }

  previewMetadata(
    templateId: string,
    entityData: Record<string, string>
  ): Observable<SeoMetadata | null> {
    return this.getTemplate(templateId).pipe(
      map(template => (template ? this.generateMetadata(template, entityData) : null))
    );
  }

  // ── Sitemap / Robots (static helpers – no backend endpoint yet) ───────────

  generateSitemap(): Observable<string> {
    return this.http
      .get(`${this.BASE_URL}/sitemap`, { responseType: 'text' })
      .pipe(
        catchError(this.handleError('generateSitemap', '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"></urlset>'))
      );
  }

  generateRobotsTxt(): Observable<string> {
    return of(`User-agent: *\nAllow: /\nSitemap: https://example.com/sitemap.xml`);
  }

  // ── Error Handler ─────────────────────────────────────────────────────────

  private handleError<T>(operation = 'operation', result: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      this.error.set(error?.message ?? 'Unknown error');
      return of(result);
    };
  }
}
