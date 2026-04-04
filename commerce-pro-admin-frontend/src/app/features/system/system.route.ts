import { Routes } from '@angular/router';

export const SYSTEM_ROUTES: Routes = [
  {
    path: 'integrations',
    loadComponent: () => import('./integrations/integrations').then(m => m.SystemIntegrations)
  },
  {
    path: 'api',
    loadComponent: () => import('./api-management/api-management').then(m => m.ApiManagement)
  },
  {
    path: 'webhooks',
    loadComponent: () => import('./webhooks/webhooks').then(m => m.SystemWebhooks)
  },
  {
    path: 'flags',
    loadComponent: () => import('./feature-flags/feature-flags').then(m => m.FeatureFlags)
  },
  {
    path: 'backup',
    loadComponent: () => import('./backup/backup').then(m => m.BackupRestore)
  }
];
