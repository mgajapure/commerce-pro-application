import { Routes } from '@angular/router';
import { Orders } from './orders';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    component: Orders,
    children: [
      { path: '', redirectTo: 'all', pathMatch: 'full' },
      { path: 'all',        loadComponent: () => import('./order-list/order-list').then(m => m.OrderList) },
      { path: 'details/:id',loadComponent: () => import('./order-details/order-details').then(m => m.OrderDetails) },
      { path: 'add',        loadComponent: () => import('./order-form/order-form').then(m => m.OrderForm) },
      { path: 'edit/:id',   loadComponent: () => import('./order-form/order-form').then(m => m.OrderForm) },
      { path: 'pending',    loadComponent: () => import('./order-pending/order-pending').then(m => m.OrderPending) },
      { path: 'processing', loadComponent: () => import('./order-processing/order-processing').then(m => m.OrderProcessing) },
      { path: 'shipped',    loadComponent: () => import('./order-shipped/order-shipped').then(m => m.OrderShipped) },
      { path: 'delivered',  loadComponent: () => import('./order-delivered/order-delivered').then(m => m.OrderDelivered) },
      { path: 'cancelled',  loadComponent: () => import('./order-cancelled/order-cancelled').then(m => m.OrderCancelled) },
      { path: 'drafts',     loadComponent: () => import('./order-drafts/order-drafts').then(m => m.OrderDrafts) },
      { path: 'returns',    loadComponent: () => import('./order-return/order-return').then(m => m.OrderReturn) },
      { path: 'refunds',    loadComponent: () => import('./order-refund/order-refund').then(m => m.OrderRefund) },
      { path: 'bulk',       loadComponent: () => import('./order-bulk/order-bulk').then(m => m.OrderBulk) }
    ]
  }
];
