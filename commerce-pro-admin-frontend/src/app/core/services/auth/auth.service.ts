import { API_HOST } from '../../config/api-config';
import { Injectable, Injector, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';

import { ApiResponse } from '../../models/common';
import { AuthRequest, AuthResponse, AuthSession, ChangePasswordRequest, MfaDisableRequest, MfaEnableRequest, MfaSetupResponse } from '../../models/auth';
import { NotificationService } from '../notification/notification.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiBase = `${API_HOST}/api`;
  private readonly authBase = `${this.apiBase}/v1/auth`;
  private readonly storageKey = 'commerce-pro-admin-auth';
  private readonly injector = inject(Injector);

  /** Lazy-resolved to break circular dependency: AuthInterceptor -> AuthService -> HttpClient -> AuthInterceptor */
  private get http(): HttpClient {
    return this.injector.get(HttpClient);
  }

  private get notificationService(): NotificationService {
    return this.injector.get(NotificationService);
  }

  private readonly sessionSignal = signal<AuthSession | null>(this.readPersistedSession());

  readonly session = computed(() => this.sessionSignal());
  readonly isAuthenticated = computed(() => {
    const current = this.sessionSignal();
    if (!current?.accessToken) return false;
    if (!current.expiresAtMs) return true;
    return Date.now() < current.expiresAtMs;
  });

  constructor() {
    // If session is restored from localStorage, start notifications
    if (this.isAuthenticated()) {
      this.notificationService.startAfterLogin();
    }
  }

  login(payload: AuthRequest): Observable<boolean> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.authBase}/login`, payload)
      .pipe(
        map(response => response.data),
        tap(response => this.setSession(response)),
        map(() => true),
        catchError(() => of(false))
      );
  }

  refreshToken(): Observable<boolean> {
    const refreshToken = this.sessionSignal()?.refreshToken;
    if (!refreshToken) return of(false);

    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.authBase}/refresh`, { refreshToken })
      .pipe(
        map(response => response.data),
        tap(response => this.setSession(response)),
        map(() => true),
        catchError(() => {
          this.clearSession();
          return of(false);
        })
      );
  }

  logout(): Observable<boolean> {
    const token = this.sessionSignal()?.accessToken;
    if (!token) {
      this.clearSession();
      return of(true);
    }

    return this.http
      .post<ApiResponse<string>>(`${this.authBase}/logout`, {}, {
        headers: new HttpHeaders({ Authorization: `Bearer ${token}` })
      })
      .pipe(
        map(() => true),
        catchError(() => of(false)),
        tap(() => this.clearSession())
      );
  }

  changePassword(payload: ChangePasswordRequest): Observable<boolean> {
    return this.http
      .post<ApiResponse<string>>(`${this.authBase}/change-password`, payload)
      .pipe(
        map(() => true),
        catchError(() => of(false))
      );
  }

  setupMfa(): Observable<MfaSetupResponse | null> {
    return this.http
      .post<ApiResponse<MfaSetupResponse>>(`${this.authBase}/mfa/setup`, {})
      .pipe(
        map(response => response.data),
        catchError(() => of(null))
      );
  }

  enableMfa(payload: MfaEnableRequest): Observable<boolean> {
    return this.http
      .post<ApiResponse<string>>(`${this.authBase}/mfa/enable`, payload)
      .pipe(
        map(() => true),
        catchError(() => of(false))
      );
  }

  disableMfa(payload: MfaDisableRequest): Observable<boolean> {
    return this.http
      .post<ApiResponse<string>>(`${this.authBase}/mfa/disable`, payload)
      .pipe(
        map(() => true),
        catchError(() => of(false))
      );
  }

  isMfaEnabled(): boolean {
    return this.sessionSignal()?.mustChangePassword === true;
  }

  getMustChangePassword(): boolean {
    return this.sessionSignal()?.mustChangePassword ?? false;
  }

  hasAuthority(authority: string): boolean {
    return this.sessionSignal()?.authorities?.includes(authority) ?? false;
  }

  hasAnyAuthority(authorities: string[]): boolean {
    if (!authorities.length) return true;
    return authorities.some(authority => this.hasAuthority(authority));
  }

  getAccessToken(): string | null {
    return this.sessionSignal()?.accessToken ?? null;
  }

  getCurrentUserId(): string | null {
    return this.sessionSignal()?.userId ?? null;
  }

  private setSession(response: AuthResponse): void {
    const tokenClaims = this.decodeJwtPayload(response.accessToken);
    const authorities = Array.isArray(tokenClaims?.['roles'])
      ? (tokenClaims?.['roles'] as unknown[]).filter((value: unknown): value is string => typeof value === 'string')
      : [];

    const expiryMs = typeof tokenClaims?.['exp'] === 'number' ? Number(tokenClaims['exp']) * 1000 : undefined;

    const session: AuthSession = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: response.userId,
      username: response.username,
      superAdmin: response.superAdmin,
      authorities,
      expiresAtMs: expiryMs,
      mustChangePassword: response.mustChangePassword ?? false
    };

    this.sessionSignal.set(session);
    localStorage.setItem(this.storageKey, JSON.stringify(session));
    this.notificationService.startAfterLogin();
  }

  private clearSession(): void {
    this.notificationService.stopOnLogout();
    this.sessionSignal.set(null);
    localStorage.removeItem(this.storageKey);
  }

  private readPersistedSession(): AuthSession | null {
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (!raw) return null;
      return JSON.parse(raw) as AuthSession;
    } catch {
      return null;
    }
  }

  private decodeJwtPayload(token: string): Record<string, unknown> | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;
      return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    } catch {
      return null;
    }
  }
}
