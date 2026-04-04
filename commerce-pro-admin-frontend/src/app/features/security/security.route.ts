import { Routes } from '@angular/router';

export const SECURITY_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'settings',
    pathMatch: 'full'
  },
  {
    path: 'settings',
    loadComponent: () => import('./settings/settings').then(m => m.SecuritySettings)
  },
  {
    path: '2fa',
    loadComponent: () => import('./two-factor/two-factor').then(m => m.TwoFactorAdmin)
  },
  {
    path: 'sso',
    loadComponent: () => import('./sso/sso').then(m => m.SsoConfig)
  },
  {
    path: 'gdpr',
    loadComponent: () => import('./gdpr/gdpr').then(m => m.GdprPrivacy)
  },
  {
    path: 'pci',
    loadComponent: () => import('./pci/pci').then(m => m.PciCompliance)
  },
  {
    path: 'encryption',
    loadComponent: () => import('./encryption/encryption').then(m => m.EncryptionKeys)
  }
];
