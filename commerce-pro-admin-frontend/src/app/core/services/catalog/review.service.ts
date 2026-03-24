import { API_HOST } from '../../config/api-config';
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { Review, ReviewStats, ReviewReply, ReviewStatus } from '../../models/catalog/review.model';
import { ApiResponse, PageParams, PageResponse } from '../../models/common';

const BASE = `${API_HOST}/api/v1/reviews`;

export interface ReviewFilterParams extends PageParams {
  productId?: string;
  status?: string;
  rating?: number;
}

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);

  private reviews = signal<Review[]>([]);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);

  readonly allReviews = computed(() => this.reviews());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());

  readonly reviewStats = computed<ReviewStats>(() => {
    const all = this.reviews();
    const ratings = all.map(r => r.rating);
    return {
      total: all.length,
      pending: all.filter(r => r.status === 'pending').length,
      approved: all.filter(r => r.status === 'approved').length,
      rejected: all.filter(r => r.status === 'rejected').length,
      averageRating: ratings.length > 0
        ? ratings.reduce((a, b) => a + b, 0) / ratings.length
        : 0,
      fiveStar: ratings.filter(r => r === 5).length,
      fourStar: ratings.filter(r => r === 4).length,
      threeStar: ratings.filter(r => r === 3).length,
      twoStar: ratings.filter(r => r === 2).length,
      oneStar: ratings.filter(r => r === 1).length
    };
  });

  constructor() {
    this.loadReviews();
  }

  loadReviews(filters?: ReviewFilterParams): void {
    this.loading.set(true);
    this.error.set(null);
    let params = this.buildParams(filters);

    this.http.get<ApiResponse<PageResponse<any>>>(BASE, { params }).pipe(
      map(r => (r.data?.content ?? []).map((item: any) => this.mapToReview(item))),
      catchError(this.handleError('loadReviews', [] as Review[]))
    ).subscribe({
      next: (items) => {
        this.reviews.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message);
        this.loading.set(false);
      }
    });
  }

  getReviews(filters?: ReviewFilterParams): Observable<PageResponse<Review>> {
    let params = this.buildParams(filters);
    return this.http.get<ApiResponse<PageResponse<any>>>(BASE, { params }).pipe(
      map(r => ({
        ...r.data,
        content: (r.data?.content ?? []).map((item: any) => this.mapToReview(item))
      })),
      catchError(this.handleError('getReviews',
        { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }))
    );
  }

  getReviewsByProduct(productId: string, pageParams?: PageParams): Observable<PageResponse<Review>> {
    let params = new HttpParams();
    if (pageParams?.page != null) params = params.set('page', String(pageParams.page));
    if (pageParams?.size != null) params = params.set('size', String(pageParams.size));

    return this.http.get<ApiResponse<PageResponse<any>>>(`${BASE}/product/${productId}`, { params }).pipe(
      map(r => ({
        ...r.data,
        content: (r.data?.content ?? []).map((item: any) => this.mapToReview(item))
      })),
      catchError(this.handleError('getReviewsByProduct',
        { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }))
    );
  }

  getReview(id: string): Observable<Review | null> {
    return this.http.get<ApiResponse<any>>(`${BASE}/${id}`).pipe(
      map(r => r.data ? this.mapToReview(r.data) : null),
      catchError(this.handleError('getReview', null))
    );
  }

  createReview(review: Partial<Review>): Observable<Review> {
    return this.http.post<ApiResponse<any>>(BASE, {
      productId: review.productId,
      productName: review.productName,
      customerName: review.customerName,
      rating: review.rating,
      title: review.title,
      content: review.content,
      isVerifiedPurchase: review.isVerifiedPurchase
    }).pipe(
      map(r => this.mapToReview(r.data)),
      tap(() => this.loadReviews()),
      catchError(this.handleError<Review>('createReview'))
    );
  }

  updateReview(id: string, updates: Partial<Review>): Observable<Review> {
    return this.http.put<ApiResponse<any>>(`${BASE}/${id}`, updates).pipe(
      map(r => this.mapToReview(r.data)),
      tap(() => this.loadReviews()),
      catchError(this.handleError<Review>('updateReview'))
    );
  }

  deleteReview(id: string): Observable<void> {
    return this.http.delete<ApiResponse<any>>(`${BASE}/${id}`).pipe(
      map(() => void 0),
      tap(() => this.loadReviews()),
      catchError(this.handleError<void>('deleteReview'))
    );
  }

  approveReview(id: string): Observable<Review> {
    return this.http.post<ApiResponse<any>>(`${BASE}/${id}/approve`, {}).pipe(
      map(r => this.mapToReview(r.data)),
      tap(() => this.loadReviews()),
      catchError(this.handleError<Review>('approveReview'))
    );
  }

  rejectReview(id: string): Observable<Review> {
    return this.http.post<ApiResponse<any>>(`${BASE}/${id}/reject`, {}).pipe(
      map(r => this.mapToReview(r.data)),
      tap(() => this.loadReviews()),
      catchError(this.handleError<Review>('rejectReview'))
    );
  }

  addReply(reviewId: string, reply: { content: string; authorName?: string }): Observable<Review> {
    return this.http.post<ApiResponse<any>>(`${BASE}/${reviewId}/reply`, {
      reply: reply.content,
      repliedBy: reply.authorName ?? 'Admin'
    }).pipe(
      map(r => this.mapToReview(r.data)),
      tap(() => this.loadReviews()),
      catchError(this.handleError<Review>('addReply'))
    );
  }

  bulkUpdateStatus(ids: string[], status: ReviewStatus): Observable<void> {
    const requests = ids.map(id =>
      status === 'approved' ? this.approveReview(id) : this.rejectReview(id)
    );
    if (requests.length === 0) return of(void 0);
    requests.forEach(r => r.subscribe());
    return of(void 0);
  }

  bulkDelete(ids: string[]): Observable<void> {
    ids.forEach(id => this.deleteReview(id).subscribe());
    return of(void 0);
  }

  retry(): void {
    this.loadReviews();
  }

  private mapToReview(item: any): Review {
    return {
      id: item.id,
      productId: item.productId ?? '',
      productName: item.productName ?? '',
      productImage: item.productImage ?? '',
      customerId: item.customerId ?? item.customerEmail ?? '',
      customerName: item.customerName ?? '',
      customerAvatar: item.customerAvatar ?? '',
      rating: item.rating ?? 0,
      title: item.title ?? '',
      content: item.content ?? '',
      status: (item.status ?? 'pending').toLowerCase() as ReviewStatus,
      isVerifiedPurchase: item.isVerifiedPurchase ?? false,
      helpfulCount: item.helpfulCount ?? 0,
      unhelpfulCount: item.unhelpfulCount ?? item.reportCount ?? 0,
      images: item.images ?? [],
      reply: item.reply ? {
        id: 'reply-' + item.id,
        authorId: item.repliedBy ?? 'admin',
        authorName: item.repliedBy ?? 'Admin',
        authorRole: 'admin' as const,
        content: typeof item.reply === 'string' ? item.reply : item.reply.content ?? '',
        createdAt: item.repliedAt ? new Date(item.repliedAt) : new Date()
      } : undefined,
      createdAt: item.createdAt ? new Date(item.createdAt) : new Date(),
      updatedAt: item.updatedAt ? new Date(item.updatedAt) : new Date()
    };
  }

  private buildParams(filters?: ReviewFilterParams): HttpParams {
    let params = new HttpParams();
    if (filters?.productId) params = params.set('productId', filters.productId);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.rating != null) params = params.set('rating', String(filters.rating));
    if (filters?.page != null) params = params.set('page', String(filters.page));
    if (filters?.size != null) params = params.set('size', String(filters.size));
    if (filters?.sort) params = params.set('sort', filters.direction
      ? `${filters.sort},${filters.direction}` : filters.sort);
    return params;
  }

  private handleError<T>(op = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[ReviewService] ${op}:`, error);
      this.error.set(error?.error?.message ?? error?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
