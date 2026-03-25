import { API_HOST } from '../../config/api-config';
import { Injectable, inject, signal, computed, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, Subscription, interval } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  Notification,
  NotificationStats,
  NotificationType,
  CreateNotificationRequest
} from '../../models/notification/notification.model';
import { ApiResponse, PageResponse } from '../../models/common';

const BASE = `${API_HOST}/api/v1/notifications`;
const POLL_INTERVAL_MS = 30_000; // 30 seconds

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly http = inject(HttpClient);

  private notificationList = signal<Notification[]>([]);
  private stats = signal<NotificationStats | null>(null);
  private loading = signal<boolean>(false);
  private error = signal<string | null>(null);
  private pollSub: Subscription | null = null;
  private initialized = false;
  private notificationSound: HTMLAudioElement | null = null;

  readonly notifications = computed(() => this.notificationList());
  readonly isLoading = computed(() => this.loading());
  readonly currentError = computed(() => this.error());
  readonly notificationStats = computed(() => this.stats());

  readonly unreadCount = computed(() =>
    this.notificationList().filter(n => !n.isRead).length
  );

  readonly unreadNotifications = computed(() =>
    this.notificationList().filter(n => !n.isRead)
  );

  constructor() {
    this.initSound();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  // ── Lifecycle — called by AuthService after login ─────────────────────────

  startAfterLogin(): void {
    if (this.initialized) return;
    this.initialized = true;
    this.loadNotifications();
    this.loadStats();
    this.startPolling();
  }

  stopOnLogout(): void {
    this.initialized = false;
    this.stopPolling();
    this.notificationList.set([]);
    this.stats.set(null);
  }

  // ── Polling ───────────────────────────────────────────────────────────────

  private startPolling(): void {
    this.stopPolling();
    this.pollSub = interval(POLL_INTERVAL_MS).subscribe(() => {
      this.pollForNewNotifications();
    });
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }

  private pollForNewNotifications(): void {
    const previousUnreadCount = this.unreadCount();

    this.http.get<ApiResponse<any[]>>(`${BASE}/unread`).pipe(
      map(r => (r.data ?? []).map((item: any) => this.mapToNotification(item))),
      catchError(() => of([] as Notification[]))
    ).subscribe(unread => {
      if (unread.length > previousUnreadCount) {
        this.playSound();
      }
      // Refresh full list + stats
      this.loadNotifications();
      this.loadStats();
    });
  }

  // ── Sound ─────────────────────────────────────────────────────────────────

  private initSound(): void {
    try {
      this.notificationSound = new Audio('assets/sounds/notification.wav');
      this.notificationSound.volume = 0.5;
    } catch {
      this.notificationSound = null;
    }
  }

  private playSound(): void {
    try {
      if (this.notificationSound) {
        this.notificationSound.currentTime = 0;
        this.notificationSound.play().catch(() => {
          // Browser may block autoplay — ignore silently
        });
      }
    } catch {
      // Ignore sound errors
    }
  }

  // ── Load ──────────────────────────────────────────────────────────────────

  loadNotifications(page = 0, size = 50): void {
    this.loading.set(true);
    this.error.set(null);

    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));

    this.http.get<ApiResponse<PageResponse<any>>>(BASE, { params }).pipe(
      map(r => (r.data?.content ?? []).map((item: any) => this.mapToNotification(item))),
      catchError(this.handleError('loadNotifications', [] as Notification[]))
    ).subscribe({
      next: (items) => {
        this.notificationList.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message);
        this.loading.set(false);
      }
    });
  }

  loadStats(): void {
    this.http.get<ApiResponse<NotificationStats>>(`${BASE}/stats`).pipe(
      map(r => r.data),
      catchError(this.handleError('loadStats', null))
    ).subscribe(data => {
      if (data) this.stats.set(data);
    });
  }

  // ── Queries ───────────────────────────────────────────────────────────────

  getNotifications(page = 0, size = 20): Observable<PageResponse<Notification>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));

    return this.http.get<ApiResponse<PageResponse<any>>>(BASE, { params }).pipe(
      map(r => ({
        ...r.data,
        content: (r.data?.content ?? []).map((item: any) => this.mapToNotification(item))
      })),
      catchError(this.handleError('getNotifications',
        { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true, empty: true }))
    );
  }

  getUnreadNotifications(): Observable<Notification[]> {
    return this.http.get<ApiResponse<any[]>>(`${BASE}/unread`).pipe(
      map(r => (r.data ?? []).map((item: any) => this.mapToNotification(item))),
      catchError(this.handleError('getUnreadNotifications', []))
    );
  }

  getStats(): Observable<NotificationStats> {
    return this.http.get<ApiResponse<NotificationStats>>(`${BASE}/stats`).pipe(
      map(r => r.data),
      catchError(this.handleError('getStats', { total: 0, unread: 0, byType: {}, byPriority: {} }))
    );
  }

  // ── Mutations ─────────────────────────────────────────────────────────────

  createNotification(request: CreateNotificationRequest): Observable<Notification> {
    return this.http.post<ApiResponse<any>>(BASE, request).pipe(
      map(r => this.mapToNotification(r.data)),
      tap(() => this.loadNotifications()),
      catchError(this.handleError<Notification>('createNotification'))
    );
  }

  markAsRead(id: string): Observable<Notification> {
    return this.http.put<ApiResponse<any>>(`${BASE}/${id}/read`, {}).pipe(
      map(r => this.mapToNotification(r.data)),
      tap(() => {
        this.notificationList.update(list =>
          list.map(n => n.id === id ? { ...n, isRead: true, readAt: new Date() } : n)
        );
        this.loadStats();
      }),
      catchError(this.handleError<Notification>('markAsRead'))
    );
  }

  markAllAsRead(): Observable<number> {
    return this.http.put<ApiResponse<number>>(`${BASE}/read-all`, {}).pipe(
      map(r => r.data),
      tap(() => {
        this.notificationList.update(list =>
          list.map(n => ({ ...n, isRead: true, readAt: new Date() }))
        );
        this.loadStats();
      }),
      catchError(this.handleError('markAllAsRead', 0))
    );
  }

  deleteNotification(id: string): Observable<void> {
    return this.http.delete<ApiResponse<any>>(`${BASE}/${id}`).pipe(
      map(() => void 0),
      tap(() => {
        this.notificationList.update(list => list.filter(n => n.id !== id));
        this.loadStats();
      }),
      catchError(this.handleError<void>('deleteNotification'))
    );
  }

  retry(): void {
    this.loadNotifications();
    this.loadStats();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private mapToNotification(item: any): Notification {
    return {
      id: item.id,
      userId: item.userId ?? '',
      title: item.title ?? '',
      message: item.message ?? '',
      type: item.type ?? 'SYSTEM',
      channel: item.channel ?? 'IN_APP',
      priority: item.priority ?? 'NORMAL',
      isRead: item.isRead ?? false,
      readAt: item.readAt ? new Date(item.readAt) : undefined,
      link: item.link,
      metadata: item.metadata,
      createdAt: item.createdAt ? new Date(item.createdAt) : new Date()
    };
  }

  private handleError<T>(op = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`[NotificationService] ${op}:`, error);
      this.error.set(error?.error?.message ?? error?.message ?? 'An error occurred');
      return of(result as T);
    };
  }
}
