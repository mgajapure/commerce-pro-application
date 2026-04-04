import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { API_HOST } from '../../core/config/api-config';
import { ApiResponse } from '../../core/models/common/api-response.model';

// ── Interfaces ──

export interface Integration {
  id: string;
  name: string;
  type: 'Payment' | 'Shipping' | 'CRM' | 'ERP' | 'Email' | 'Analytics';
  status: 'active' | 'inactive' | 'error';
  lastSyncedAt: string | null;
  configuration: Record<string, any>;
  credentials: Record<string, string>;
  createdAt: string;
  updatedAt: string;
}

export interface ApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  fullKey?: string;
  scopes: string[];
  createdAt: string;
  lastUsedAt: string | null;
  status: 'active' | 'revoked';
}

export interface ApiUsageStats {
  totalRequests: number;
  requestsToday: number;
  errorRate: number;
  activeKeys: number;
}

export interface Webhook {
  id: string;
  url: string;
  events: string[];
  secret: string;
  active: boolean;
  lastTriggeredAt: string | null;
  failureCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookDelivery {
  id: string;
  webhookId: string;
  event: string;
  statusCode: number;
  success: boolean;
  timestamp: string;
  responseTime: number;
}

export interface FeatureFlag {
  key: string;
  description: string;
  enabled: boolean;
  category: 'Features' | 'Experiments' | 'Ops';
  rolloutPercentage: number;
  targetGroups: string[];
  updatedAt: string;
}

export interface BackupStatus {
  lastBackupAt: string | null;
  lastBackupSize: string;
  status: 'idle' | 'running' | 'completed' | 'failed';
  progress: number;
}

export interface BackupEntry {
  id: string;
  date: string;
  size: string;
  type: 'auto' | 'manual';
  status: 'completed' | 'failed' | 'in_progress';
}

export interface BackupSchedule {
  frequency: 'daily' | 'weekly' | 'monthly';
  time: string;
  retentionCount: number;
}

// ── Service ──

@Injectable({ providedIn: 'root' })
export class SystemService {
  private http = inject(HttpClient);
  private base = `${API_HOST}/api/v1/admin/system`;

  // ── Integrations ──

  getIntegrations(): Observable<Integration[]> {
    return this.http.get<ApiResponse<Integration[]>>(`${this.base}/integrations`)
      .pipe(map(res => res.data), catchError(() => of([])));
  }

  createIntegration(data: Partial<Integration>): Observable<Integration | null> {
    return this.http.post<ApiResponse<Integration>>(`${this.base}/integrations`, data)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  updateIntegration(id: string, data: Partial<Integration>): Observable<Integration | null> {
    return this.http.put<ApiResponse<Integration>>(`${this.base}/integrations/${id}`, data)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  deleteIntegration(id: string): Observable<boolean> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/integrations/${id}`)
      .pipe(map(() => true), catchError(() => of(false)));
  }

  testIntegration(id: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<ApiResponse<{ success: boolean; message: string }>>(`${this.base}/integrations/${id}/test`, {})
      .pipe(map(res => res.data), catchError(() => of({ success: false, message: 'Connection test failed' })));
  }

  // ── API Keys ──

  getApiKeys(): Observable<ApiKey[]> {
    return this.http.get<ApiResponse<ApiKey[]>>(`${this.base}/api-keys`)
      .pipe(map(res => res.data), catchError(() => of([])));
  }

  getApiUsageStats(): Observable<ApiUsageStats> {
    return this.http.get<ApiResponse<ApiUsageStats>>(`${this.base}/api-keys/stats`)
      .pipe(map(res => res.data), catchError(() => of({ totalRequests: 0, requestsToday: 0, errorRate: 0, activeKeys: 0 })));
  }

  createApiKey(data: { name: string; scopes: string[] }): Observable<ApiKey | null> {
    return this.http.post<ApiResponse<ApiKey>>(`${this.base}/api-keys`, data)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  revokeApiKey(id: string): Observable<boolean> {
    return this.http.post<ApiResponse<void>>(`${this.base}/api-keys/${id}/revoke`, {})
      .pipe(map(() => true), catchError(() => of(false)));
  }

  // ── Webhooks ──

  getWebhooks(): Observable<Webhook[]> {
    return this.http.get<ApiResponse<Webhook[]>>(`${this.base}/webhooks`)
      .pipe(map(res => res.data), catchError(() => of([])));
  }

  getWebhookDeliveries(webhookId: string): Observable<WebhookDelivery[]> {
    return this.http.get<ApiResponse<WebhookDelivery[]>>(`${this.base}/webhooks/${webhookId}/deliveries`)
      .pipe(map(res => res.data), catchError(() => of([])));
  }

  createWebhook(data: Partial<Webhook>): Observable<Webhook | null> {
    return this.http.post<ApiResponse<Webhook>>(`${this.base}/webhooks`, data)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  updateWebhook(id: string, data: Partial<Webhook>): Observable<Webhook | null> {
    return this.http.put<ApiResponse<Webhook>>(`${this.base}/webhooks/${id}`, data)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  deleteWebhook(id: string): Observable<boolean> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/webhooks/${id}`)
      .pipe(map(() => true), catchError(() => of(false)));
  }

  testWebhook(id: string): Observable<{ success: boolean; statusCode: number; message: string }> {
    return this.http.post<ApiResponse<{ success: boolean; statusCode: number; message: string }>>(`${this.base}/webhooks/${id}/test`, {})
      .pipe(map(res => res.data), catchError(() => of({ success: false, statusCode: 0, message: 'Test failed' })));
  }

  // ── Feature Flags ──

  getFeatureFlags(): Observable<FeatureFlag[]> {
    return this.http.get<ApiResponse<FeatureFlag[]>>(`${this.base}/feature-flags`)
      .pipe(map(res => res.data), catchError(() => of([])));
  }

  updateFeatureFlag(key: string, data: Partial<FeatureFlag>): Observable<FeatureFlag | null> {
    return this.http.put<ApiResponse<FeatureFlag>>(`${this.base}/feature-flags/${key}`, data)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  // ── Backup ──

  getBackupStatus(): Observable<BackupStatus> {
    return this.http.get<ApiResponse<BackupStatus>>(`${this.base}/backup/status`)
      .pipe(map(res => res.data), catchError(() => of({ lastBackupAt: null, lastBackupSize: '0 MB', status: 'idle' as const, progress: 0 })));
  }

  getBackupHistory(): Observable<BackupEntry[]> {
    return this.http.get<ApiResponse<BackupEntry[]>>(`${this.base}/backup/history`)
      .pipe(map(res => res.data), catchError(() => of([])));
  }

  getBackupSchedule(): Observable<BackupSchedule> {
    return this.http.get<ApiResponse<BackupSchedule>>(`${this.base}/backup/schedule`)
      .pipe(map(res => res.data), catchError(() => of({ frequency: 'daily' as const, time: '02:00', retentionCount: 30 })));
  }

  updateBackupSchedule(data: BackupSchedule): Observable<BackupSchedule | null> {
    return this.http.put<ApiResponse<BackupSchedule>>(`${this.base}/backup/schedule`, data)
      .pipe(map(res => res.data), catchError(() => of(null)));
  }

  triggerBackup(): Observable<boolean> {
    return this.http.post<ApiResponse<void>>(`${this.base}/backup/trigger`, {})
      .pipe(map(() => true), catchError(() => of(false)));
  }

  triggerRestore(confirmCode: string): Observable<boolean> {
    return this.http.post<ApiResponse<void>>(`${this.base}/backup/restore`, { confirmCode })
      .pipe(map(() => true), catchError(() => of(false)));
  }

  downloadBackup(id: string): Observable<Blob> {
    return this.http.get(`${this.base}/backup/${id}/download`, { responseType: 'blob' });
  }
}
