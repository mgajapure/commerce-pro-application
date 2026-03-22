import { Routes } from '@angular/router';
import { Finance } from './finance';
export const FINANCE_ROUTES: Routes = [{
  path: '', component: Finance, children: [
    { path: '', redirectTo: 'overview', pathMatch: 'full' },
    { path: 'overview',       loadComponent: () => import('./overview/overview').then(m => m.Overview) },
    { path: 'invoices',       loadComponent: () => import('./invoices/invoices').then(m => m.Invoices) },
    { path: 'ar',             loadComponent: () => import('./ar/ar').then(m => m.AR) },
    { path: 'vendors',        loadComponent: () => import('./vendors/vendors').then(m => m.Vendors) },
    { path: 'vendor-invoices',loadComponent: () => import('./vendor-invoices/vendor-invoices').then(m => m.VendorInvoices) },
    { path: 'ap',             loadComponent: () => import('./ap/ap').then(m => m.AP) },
    { path: 'tax',            loadComponent: () => import('./tax/tax').then(m => m.Tax) },
    { path: 'expenses',       loadComponent: () => import('./expenses/expenses').then(m => m.Expenses) },
    { path: 'journal',        loadComponent: () => import('./journal/journal').then(m => m.Journal) },
    { path: 'periods',        loadComponent: () => import('./periods/periods').then(m => m.Periods) },
    { path: 'budgets',        loadComponent: () => import('./budgets/budgets').then(m => m.Budgets) },
    { path: 'cashflow',       loadComponent: () => import('./cashflow/cashflow').then(m => m.Cashflow) },
    { path: 'exchange-rates', loadComponent: () => import('./exchange-rates/exchange-rates').then(m => m.ExchangeRates) },
  ]
}];
