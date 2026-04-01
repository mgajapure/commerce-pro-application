import { API_HOST } from '../../config/api-config';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ApiResponse, PageResponse } from '../../models/common';
import {
  AuditLogEntry,
  CreateIdentityPermissionRequest,
  CreateIdentityRoleRequest,
  CreateIdentityUserRequest,
  IdentityPermission,
  IdentityRole,
  IdentityUser,
  IdentityUserDetail,
  ImpersonationToken,
  SuperAdminConfigView,
  UpdateIdentityUserRequest
} from '../../models/identity';
import { AuthService } from '../auth/auth.service';

@Injectable({
  providedIn: 'root'
})
export class IdentityService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  private readonly baseUrl = `${API_HOST}/api/v1/identity`;

  private adminHeaders(): HttpHeaders {
    const userId = this.authService.getCurrentUserId() ?? 'ui-admin';
    return new HttpHeaders({
      'X-Admin-Id': userId,
      'X-User-Id': userId
    });
  }

  getUsers(page = 0, size = 10, search = ''): Observable<PageResponse<IdentityUser>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) params = params.set('search', search.trim());

    return this.http
      .get<ApiResponse<PageResponse<IdentityUser>>>(`${this.baseUrl}/users`, { params })
      .pipe(
        map(res => res.data),
        catchError(() => of(this.emptyPage<IdentityUser>()))
      );
  }

  createUser(payload: CreateIdentityUserRequest): Observable<IdentityUser | null> {
    return this.http
      .post<ApiResponse<IdentityUser>>(`${this.baseUrl}/users`, payload, { headers: this.adminHeaders() })
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  getUserDetail(id: string): Observable<IdentityUserDetail | null> {
    return this.http
      .get<ApiResponse<IdentityUserDetail>>(`${this.baseUrl}/users/${id}`)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  updateUser(id: string, payload: UpdateIdentityUserRequest): Observable<IdentityUser | null> {
    return this.http
      .put<ApiResponse<IdentityUser>>(`${this.baseUrl}/users/${id}`, payload, { headers: this.adminHeaders() })
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  toggleUserActivation(id: string, active: boolean): Observable<boolean> {
    const endpoint = active ? 'activate' : 'deactivate';
    const params = active ? undefined : new HttpParams().set('reason', 'Deactivated via admin UI');

    return this.http
      .post<ApiResponse<string>>(`${this.baseUrl}/users/${id}/${endpoint}`, {}, { headers: this.adminHeaders(), params })
      .pipe(map(() => true), catchError(() => of(false)));
  }

  deleteUser(id: string, reason?: string): Observable<boolean> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http
      .delete<ApiResponse<string>>(`${this.baseUrl}/users/${id}`, { headers: this.adminHeaders(), params })
      .pipe(map(() => true), catchError(() => of(false)));
  }

  resetUserPassword(id: string, notifyUser = true): Observable<boolean> {
    const params = new HttpParams().set('notifyUser', notifyUser);
    return this.http
      .post<ApiResponse<string>>(`${this.baseUrl}/users/${id}/reset-password`, {}, { headers: this.adminHeaders(), params })
      .pipe(map(() => true), catchError(() => of(false)));
  }

  unlockUserAccount(id: string): Observable<boolean> {
    return this.http
      .post<ApiResponse<string>>(`${this.baseUrl}/users/${id}/unlock`, {}, { headers: this.adminHeaders() })
      .pipe(map(() => true), catchError(() => of(false)));
  }

  impersonateUser(id: string): Observable<ImpersonationToken | null> {
    return this.http
      .post<ApiResponse<ImpersonationToken>>(`${this.baseUrl}/users/${id}/impersonate`, {}, { headers: this.adminHeaders() })
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  getUserAudit(id: string, page = 0, size = 10): Observable<PageResponse<AuditLogEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<ApiResponse<PageResponse<AuditLogEntry>>>(`${this.baseUrl}/users/${id}/audit`, { params })
      .pipe(map(res => res.data), catchError(() => of(this.emptyPage<AuditLogEntry>())));
  }

  assignRoles(userId: string, roleCodes: string[]): Observable<boolean> {
    return this.http
      .post<ApiResponse<string>>(
        `${this.baseUrl}/users/${userId}/roles`,
        { roleCodes, reason: 'Assigned from admin UI' },
        { headers: this.adminHeaders() }
      )
      .pipe(map(() => true), catchError(() => of(false)));
  }

  getRoles(page = 0, size = 10): Observable<PageResponse<IdentityRole>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http
      .get<ApiResponse<PageResponse<IdentityRole>>>(`${this.baseUrl}/roles`, { params })
      .pipe(
        map(res => res.data),
        catchError(() => of(this.emptyPage<IdentityRole>()))
      );
  }

  createRole(payload: CreateIdentityRoleRequest): Observable<IdentityRole | null> {
    return this.http
      .post<ApiResponse<IdentityRole>>(`${this.baseUrl}/roles`, payload, { headers: this.adminHeaders() })
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  grantPermissions(roleId: string, permissionCodes: string[]): Observable<boolean> {
    return this.http
      .post<ApiResponse<string>>(`${this.baseUrl}/roles/${roleId}/permissions`, permissionCodes, {
        headers: this.adminHeaders()
      })
      .pipe(map(() => true), catchError(() => of(false)));
  }

  revokePermissions(roleId: string, permissionCodes: string[]): Observable<boolean> {
    return this.http
      .request<ApiResponse<string>>('delete', `${this.baseUrl}/roles/${roleId}/permissions`, {
        body: permissionCodes,
        headers: this.adminHeaders()
      })
      .pipe(map(() => true), catchError(() => of(false)));
  }

  getPermissions(page = 0, size = 25): Observable<PageResponse<IdentityPermission>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http
      .get<ApiResponse<PageResponse<IdentityPermission>>>(`${this.baseUrl}/permissions`, { params })
      .pipe(
        map(res => res.data),
        catchError(() => of(this.emptyPage<IdentityPermission>()))
      );
  }

  createPermission(payload: CreateIdentityPermissionRequest): Observable<IdentityPermission | null> {
    return this.http
      .post<ApiResponse<IdentityPermission>>(`${this.baseUrl}/permissions`, payload, {
        headers: this.adminHeaders()
      })
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  getAuditLogs(page = 0, size = 10): Observable<PageResponse<AuditLogEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http
      .get<ApiResponse<PageResponse<AuditLogEntry>>>(`${this.baseUrl}/audit/logs`, { params })
      .pipe(
        map(res => res.data),
        catchError(() => of(this.emptyPage<AuditLogEntry>()))
      );
  }

  getSuperAdminConfiguration(): Observable<SuperAdminConfigView | null> {
    return this.http
      .get<ApiResponse<SuperAdminConfigView>>(`${this.baseUrl}/config/superadmin`)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  reloadConfiguration(): Observable<boolean> {
    return this.http
      .post<ApiResponse<string>>(`${this.baseUrl}/config/reload`, {}, { headers: this.adminHeaders() })
      .pipe(map(() => true), catchError(() => of(false)));
  }

  private emptyPage<T>(): PageResponse<T> {
    return {
      content: [],
      page: 0,
      size: 0,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
      empty: true
    };
  }
}
