import { Routes } from '@angular/router';
import { Fulfillment } from './fulfillment';

export const FULFILLMENT_ROUTES: Routes = [
  {
    path: '',
    component: Fulfillment,
    children: [
      { path: '', redirectTo: 'queue', pathMatch: 'full' },
      { path: 'queue',          loadComponent: () => import('./fulfillment-queue/fulfillment-queue').then(m => m.FulfillmentQueue) },
      { path: 'pick-pack',      loadComponent: () => import('./pick-pack/pick-pack').then(m => m.PickPack) },
      { path: 'shipments',      loadComponent: () => import('./shipments/shipments').then(m => m.Shipments) },
      { path: 'tracking',       loadComponent: () => import('./tracking/tracking').then(m => m.Tracking) },
      { path: 'labels',         loadComponent: () => import('./labels/labels').then(m => m.Labels) },
      { path: 'shipping-rules', loadComponent: () => import('./shipping-rules/shipping-rules').then(m => m.ShippingRules) },
    ]
  }
];