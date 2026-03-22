// src/app/core/services/index.ts
// Central export for all services

// Catalog
export * from './catalog/product.service';
export * from './catalog/category.service';
export * from './catalog/attribute.service';
export * from './catalog/brand.service';
export * from './catalog/collection.service';
export * from './catalog/review.service';
export * from './catalog/bulk-operation.service';
export * from './catalog/seo.service';

// Domain
export * from './customers/customers.service';
export * from './order/order.service';
export * from './dashboard/dashboard.service';
export * from './notification/notification.service';
export * from './identity/identity.service';
export * from './auth/auth.service';
export * from './payment/payment.service';
export * from './finance/finance.service';
