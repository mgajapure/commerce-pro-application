import { Routes } from '@angular/router';
import { Analytics } from './analytics';

export const ANALYTICS_ROUTES: Routes = [
  {
    path: '',
    component: Analytics,
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview',          loadComponent: () => import('./overview/overview').then(m => m.AnalyticsOverview) },
      { path: 'sales',             loadComponent: () => import('./sales/sales').then(m => m.Sales) },
      { path: 'inventory-report',  loadComponent: () => import('./inventory/inventory').then(m => m.Inventory) },
      { path: 'orders-report',     loadComponent: () => import('./orders-report/orders-report').then(m => m.OrdersReportPage) },
      { path: 'customers-report',  loadComponent: () => import('./customer/customer').then(m => m.Customer) },
      { path: 'financial',         loadComponent: () => import('./financial/financial').then(m => m.Financial) },
      { path: 'shipping',          loadComponent: () => import('./shipping/shipping').then(m => m.ShippingReportPage) },
      { path: 'returns',           loadComponent: () => import('./returns/returns').then(m => m.ReturnsReportPage) },
      { path: 'saved-reports',     loadComponent: () => import('./saved-reports/saved-reports').then(m => m.SavedReportsPage) },
      { path: 'scheduled-reports', loadComponent: () => import('./scheduled-reports/scheduled-reports').then(m => m.ScheduledReportsPage) },
      { path: 'execution-history', loadComponent: () => import('./execution-history/execution-history').then(m => m.ExecutionHistoryPage) },
      { path: 'custom-reports',    loadComponent: () => import('./custom-reports/custom-reports').then(m => m.CustomReports) },
    ]
  }
];
