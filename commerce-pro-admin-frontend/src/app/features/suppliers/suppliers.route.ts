import { Routes } from '@angular/router';
import { Suppliers } from './suppliers';

export const SUPPLIERS_ROUTES: Routes = [
  {
    path: '',
    component: Suppliers,
    children: [
      { path: '', redirectTo: 'directory', pathMatch: 'full' },
      {
        path: 'directory',
        loadComponent: () => import('./supplier-directory/supplier-directory').then(m => m.SupplierDirectory)
      },
      {
        path: 'products',
        loadComponent: () => import('./supplier-products/supplier-products').then(m => m.SupplierProducts)
      },
      {
        path: 'purchase-orders',
        loadComponent: () => import('./purchase-orders/purchase-orders').then(m => m.PurchaseOrders)
      },
      {
        path: 'procurement',
        loadComponent: () => import('./procurement/procurement').then(m => m.Procurement)
      },
      {
        path: 'evaluation',
        loadComponent: () => import('./evaluation/evaluation').then(m => m.Evaluation)
      },
      {
        path: 'contracts',
        loadComponent: () => import('./contracts/contracts').then(m => m.Contracts)
      }
    ]
  }
];
