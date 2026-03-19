// src/app/features/customers/customers.route.ts
import { Routes } from '@angular/router';
import { Customers } from './customers';

export const CUSTOMERS_ROUTES: Routes = [
  {
    path: '',
    component: Customers,
    children: [
      { path: '', redirectTo: 'all', pathMatch: 'full' },
      {
        path: 'all',
        loadComponent: () => import('./customer-list/customer-list').then(m => m.CustomerList)
      },
      {
        path: 'add',
        loadComponent: () => import('./customer-form/customer-form').then(m => m.CustomerForm)
      },
      {
        path: 'edit/:id',
        loadComponent: () => import('./customer-form/customer-form').then(m => m.CustomerForm)
      },
      {
        path: 'detail/:id',
        loadComponent: () => import('./customer-detail/customer-detail').then(m => m.CustomerDetail)
      },
      {
        path: 'groups',
        loadComponent: () => import('./customer-groups/customer-groups').then(m => m.CustomerGroups)
      }
    ]
  }
];
