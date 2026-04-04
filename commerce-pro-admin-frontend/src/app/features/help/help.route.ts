import { Routes } from '@angular/router';

export const HELP_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'documentation',
    pathMatch: 'full'
  },
  {
    path: 'documentation',
    loadComponent: () => import('./documentation/documentation').then(m => m.Documentation)
  },
  {
    path: 'api-docs',
    loadComponent: () => import('./api-docs/api-docs').then(m => m.ApiDocs)
  },
  {
    path: 'support',
    loadComponent: () => import('./support/support').then(m => m.ContactSupport)
  },
  {
    path: 'community',
    loadComponent: () => import('./community/community').then(m => m.Community)
  },
  {
    path: 'status',
    loadComponent: () => import('./status/status').then(m => m.SystemStatus)
  }
];
