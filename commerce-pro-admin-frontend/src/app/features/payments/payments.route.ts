import { Routes } from '@angular/router';
import { Payments } from './payments';

export const PAYMENTS_ROUTES: Routes = [
  {
    path: '',
    component: Payments,
    children: [
      { path: '', redirectTo: 'transactions', pathMatch: 'full' },
      { path: 'transactions',    loadComponent: () => import('./transactions/transactions').then(m => m.Transactions) },
      { path: 'pending',         loadComponent: () => import('./pending/pending').then(m => m.Pending) },
      { path: 'refunds',         loadComponent: () => import('./refunds/refunds').then(m => m.Refunds) },
      { path: 'chargebacks',     loadComponent: () => import('./chargebacks/chargebacks').then(m => m.Chargebacks) },
      { path: 'reconciliation',  loadComponent: () => import('./reconciliation/reconciliation').then(m => m.Reconciliation) },
      { path: 'payouts',         loadComponent: () => import('./payouts/payouts').then(m => m.Payouts) },
      { path: 'links',           loadComponent: () => import('./links/links').then(m => m.Links) },
      { path: 'gateways',        loadComponent: () => import('./gateways/gateways').then(m => m.Gateways) },
      { path: 'methods',         loadComponent: () => import('./methods/methods').then(m => m.Methods) },
    ]
  }
];
