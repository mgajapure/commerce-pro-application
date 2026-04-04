import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.Login)
  },
  {
    path: 'auth/change-password',
    canActivate: [authGuard],
    loadComponent: () => import('./features/auth/change-password/change-password').then(m => m.ChangePassword)
  },
  {
    path: 'account/security',
    canActivate: [authGuard],
    loadComponent: () => import('./features/account/security/security').then(m => m.AccountSecurity)
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    path: 'analytics',
    canActivate: [authGuard],
    loadChildren: () => import('./features/analytics/analytics.route').then(m => m.ANALYTICS_ROUTES)
  },
  {
    path: 'catalog',
    canActivate: [authGuard],
    loadChildren: () => import('./features/catalog/catalog.route').then(m => m.PRODUCTS_ROUTES)
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadChildren: () => import('./features/orders/orders.route').then(m => m.ORDERS_ROUTES)
  },
  {
    path: 'inventory',
    canActivate: [authGuard],
    loadChildren: () => import('./features/inventory/inventory.route').then(m => m.INVENTORY_ROUTES)
  },
  {
    path: 'customers',
    canActivate: [authGuard],
    loadChildren: () => import('./features/customers/customers.route').then(m => m.CUSTOMERS_ROUTES)
  },
  {
    path: 'fulfillment',
    canActivate: [authGuard],
    loadChildren: () => import('./features/fulfillment/fulfillment.route').then(m => m.FULFILLMENT_ROUTES)
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadChildren: () => import('./features/notifications/notifications.route').then(m => m.NOTIFICATIONS_ROUTES)
  },
  {
    path: 'identity',
    canActivate: [authGuard],
    loadChildren: () => import('./features/identity/identity.route').then(m => m.IDENTITY_ROUTES)
  },
  {
    path: 'payments',
    canActivate: [authGuard],
    loadChildren: () => import('./features/payments/payments.route').then(m => m.PAYMENTS_ROUTES)
  },
  {
      path: 'finance',
      canActivate: [authGuard],
      loadChildren: () => import('./features/finance/finance.route').then(m => m.FINANCE_ROUTES)
    },
  {
    path: 'suppliers',
    canActivate: [authGuard],
    loadChildren: () => import('./features/suppliers/suppliers.route').then(m => m.SUPPLIERS_ROUTES)
  },
  {
    path: 'ai',
    canActivate: [authGuard],
    loadChildren: () => import('./features/ai/ai.route').then(m => m.AI_ROUTES)
  },
  {
    path: 'security',
    canActivate: [authGuard],
    loadChildren: () => import('./features/security/security.route').then(m => m.SECURITY_ROUTES)
  },
  {
    path: 'system',
    canActivate: [authGuard],
    loadChildren: () => import('./features/system/system.route').then(m => m.SYSTEM_ROUTES)
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadChildren: () => import('./features/settings/settings.route').then(m => m.SETTINGS_ROUTES)
  },
  {
    path: 'help',
    canActivate: [authGuard],
    loadChildren: () => import('./features/help/help.route').then(m => m.HELP_ROUTES)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
