import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { API_HOST } from '../../core/config/api-config';
import { ApiResponse } from '../../core/models/common/api-response.model';

const BASE = `${API_HOST}/api/v1/admin/security`;

// ── Interfaces ──────────────────────────────────────────────────────────────

export interface SecuritySettings {
  passwordPolicy: {
    minLength: number;
    requireUppercase: boolean;
    requireNumbers: boolean;
    requireSpecialChars: boolean;
    expiryDays: number;
  };
  sessionConfig: {
    timeoutMinutes: number;
    maxConcurrentSessions: number;
  };
  loginConfig: {
    maxAttempts: number;
    lockoutDurationMinutes: number;
  };
  ipWhitelist: string[];
}

export interface MfaPolicy {
  requireForAllAdmins: boolean;
  gracePeriodDays: number;
  allowedMethods: {
    totp: boolean;
    sms: boolean;
    email: boolean;
  };
  stats: {
    totalUsers: number;
    mfaEnabledUsers: number;
  };
}

export interface SsoConfiguration {
  enabled: boolean;
  provider: 'SAML' | 'OAuth2' | 'OIDC';
  clientId: string;
  clientSecret: string;
  issuerUrl: string;
  callbackUrl: string;
  allowedDomains: string[];
  autoProvisionUsers: boolean;
}

export interface GdprSettings {
  dataRetention: {
    retentionDays: number;
    autoAnonymize: boolean;
  };
  consentManagement: {
    cookieConsentRequired: boolean;
    marketingConsent: boolean;
  };
  dataSubjectRights: {
    allowExport: boolean;
    allowDeletion: boolean;
    processingDays: number;
  };
  cookieSettings: {
    analytics: boolean;
    marketing: boolean;
    functional: boolean;
  };
}

export interface PciComplianceItem {
  id: string;
  name: string;
  status: 'compliant' | 'non-compliant' | 'in-progress';
  lastAuditDate: string;
  notes: string;
}

export interface EncryptionKey {
  id: string;
  name: string;
  algorithm: string;
  status: 'active' | 'expired' | 'rotating';
  createdDate: string;
  expiryDate: string;
}

// ── Service ─────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class SecurityService {

  private http = inject(HttpClient);

  // ── Security Settings ───────────────────────────────────────────────────

  getSecuritySettings(): Observable<SecuritySettings> {
    return this.http.get<ApiResponse<SecuritySettings>>(`${BASE}/settings`)
      .pipe(map(r => r.data), catchError(this.err('getSecuritySettings', {
        passwordPolicy: { minLength: 8, requireUppercase: true, requireNumbers: true, requireSpecialChars: false, expiryDays: 90 },
        sessionConfig: { timeoutMinutes: 30, maxConcurrentSessions: 3 },
        loginConfig: { maxAttempts: 5, lockoutDurationMinutes: 15 },
        ipWhitelist: []
      })));
  }

  updateSecuritySettings(settings: SecuritySettings): Observable<SecuritySettings> {
    return this.http.put<ApiResponse<SecuritySettings>>(`${BASE}/settings`, settings)
      .pipe(map(r => r.data), catchError(this.err('updateSecuritySettings', null as any)));
  }

  // ── MFA Policy ──────────────────────────────────────────────────────────

  getMfaPolicy(): Observable<MfaPolicy> {
    return this.http.get<ApiResponse<MfaPolicy>>(`${BASE}/mfa`)
      .pipe(map(r => r.data), catchError(this.err('getMfaPolicy', {
        requireForAllAdmins: false, gracePeriodDays: 14,
        allowedMethods: { totp: true, sms: true, email: true },
        stats: { totalUsers: 0, mfaEnabledUsers: 0 }
      })));
  }

  updateMfaPolicy(policy: MfaPolicy): Observable<MfaPolicy> {
    return this.http.put<ApiResponse<MfaPolicy>>(`${BASE}/mfa`, policy)
      .pipe(map(r => r.data), catchError(this.err('updateMfaPolicy', null as any)));
  }

  // ── SSO Config ──────────────────────────────────────────────────────────

  getSsoConfig(): Observable<SsoConfiguration> {
    return this.http.get<ApiResponse<SsoConfiguration>>(`${BASE}/sso`)
      .pipe(map(r => r.data), catchError(this.err('getSsoConfig', {
        enabled: false, provider: 'SAML' as const, clientId: '', clientSecret: '',
        issuerUrl: '', callbackUrl: '', allowedDomains: [], autoProvisionUsers: false
      })));
  }

  updateSsoConfig(config: SsoConfiguration): Observable<SsoConfiguration> {
    return this.http.put<ApiResponse<SsoConfiguration>>(`${BASE}/sso`, config)
      .pipe(map(r => r.data), catchError(this.err('updateSsoConfig', null as any)));
  }

  testSsoConnection(config: SsoConfiguration): Observable<{ success: boolean; message: string }> {
    return this.http.post<ApiResponse<{ success: boolean; message: string }>>(`${BASE}/sso/test`, config)
      .pipe(map(r => r.data), catchError(this.err('testSsoConnection', { success: false, message: 'Connection test failed' })));
  }

  // ── GDPR Settings ──────────────────────────────────────────────────────

  getGdprSettings(): Observable<GdprSettings> {
    return this.http.get<ApiResponse<GdprSettings>>(`${BASE}/gdpr`)
      .pipe(map(r => r.data), catchError(this.err('getGdprSettings', {
        dataRetention: { retentionDays: 365, autoAnonymize: false },
        consentManagement: { cookieConsentRequired: true, marketingConsent: false },
        dataSubjectRights: { allowExport: true, allowDeletion: true, processingDays: 30 },
        cookieSettings: { analytics: true, marketing: false, functional: true }
      })));
  }

  updateGdprSettings(settings: GdprSettings): Observable<GdprSettings> {
    return this.http.put<ApiResponse<GdprSettings>>(`${BASE}/gdpr`, settings)
      .pipe(map(r => r.data), catchError(this.err('updateGdprSettings', null as any)));
  }

  // ── PCI Compliance ─────────────────────────────────────────────────────

  getPciStatus(): Observable<PciComplianceItem[]> {
    return this.http.get<ApiResponse<PciComplianceItem[]>>(`${BASE}/pci`)
      .pipe(map(r => r.data ?? []), catchError(this.err('getPciStatus', [])));
  }

  updatePciStatus(items: PciComplianceItem[]): Observable<PciComplianceItem[]> {
    return this.http.put<ApiResponse<PciComplianceItem[]>>(`${BASE}/pci`, items)
      .pipe(map(r => r.data ?? []), catchError(this.err('updatePciStatus', null as any)));
  }

  // ── Encryption Keys ────────────────────────────────────────────────────

  getEncryptionKeys(): Observable<EncryptionKey[]> {
    return this.http.get<ApiResponse<EncryptionKey[]>>(`${BASE}/encryption/keys`)
      .pipe(map(r => r.data ?? []), catchError(this.err('getEncryptionKeys', [])));
  }

  rotateEncryptionKey(keyId: string): Observable<EncryptionKey> {
    return this.http.post<ApiResponse<EncryptionKey>>(`${BASE}/encryption/keys/${keyId}/rotate`, {})
      .pipe(map(r => r.data), catchError(this.err('rotateEncryptionKey', null as any)));
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  private err<T>(op: string, fallback: T) {
    return (e: any): Observable<T> => {
      console.error(`[SecurityService.${op}]`, e?.error?.message ?? e?.message ?? e);
      return of(fallback);
    };
  }
}
