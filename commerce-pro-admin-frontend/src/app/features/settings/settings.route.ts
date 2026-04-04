import { Routes } from '@angular/router';

export const SETTINGS_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'general',
    pathMatch: 'full'
  },
  {
    path: 'general',
    loadComponent: () => import('./general/general').then(m => m.GeneralSettings)
  },
  {
    path: 'store',
    loadComponent: () => import('./store/store').then(m => m.StoreSettings)
  },
  {
    path: 'checkout',
    loadComponent: () => import('./checkout/checkout').then(m => m.CheckoutSettings)
  },
  {
    path: 'notifications',
    loadComponent: () => import('./notifications/notifications').then(m => m.NotificationSettings)
  },
  {
    path: 'billing',
    loadComponent: () => import('./billing/billing').then(m => m.BillingSettings)
  }
];
