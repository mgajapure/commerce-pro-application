import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { API_HOST } from '../../core/config/api-config';
import { ApiResponse } from '../../core/models/common/api-response.model';

const BASE = `${API_HOST}/api/v1/admin/settings`;

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private http = inject(HttpClient);

  // General Settings
  getGeneralSettings(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${BASE}/general`).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  updateGeneralSettings(data: any): Observable<any> {
    return this.http.put<ApiResponse<any>>(`${BASE}/general`, data).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  // Store Settings
  getStoreSettings(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${BASE}/store`).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  updateStoreSettings(data: any): Observable<any> {
    return this.http.put<ApiResponse<any>>(`${BASE}/store`, data).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  // Checkout Settings
  getCheckoutSettings(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${BASE}/checkout`).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  updateCheckoutSettings(data: any): Observable<any> {
    return this.http.put<ApiResponse<any>>(`${BASE}/checkout`, data).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  // Notification Settings
  getNotificationSettings(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${BASE}/notifications`).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  updateNotificationSettings(data: any): Observable<any> {
    return this.http.put<ApiResponse<any>>(`${BASE}/notifications`, data).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  // Billing Info
  getBillingInfo(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${BASE}/billing`).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }

  updateBillingInfo(data: any): Observable<any> {
    return this.http.put<ApiResponse<any>>(`${BASE}/billing`, data).pipe(
      map(res => res.data),
      catchError(() => of(null))
    );
  }
}
