// src/app/core/models/index.ts
// Central export for all models

// Product models
export * from './catalog/product.model';

// Category models
export * from './catalog/category.model';

// Attribute models
export * from './catalog/attribute.model';

// Brand models
export * from './catalog/brand.model';

// Collection models
export * from './catalog/collection.model';

// Review models
export * from './catalog/review.model';

// SEO models
export * from './catalog/seo.model';

// Catalog shared models (bulk operations, etc.)
export * from './catalog/catalog-shared.model';

// Common/Shared API models (ApiResponse, Pagination, etc.)
export * from './common';

// Backend API DTOs - Catalog specific (Product requests/responses)
export * from './catalog/product-request.model';
export * from './catalog/product-response.model';

// Dashboard models
export * from './dashboard/dashboard.model';

// Inventory models
export * from './inventory';

// Identity models
export * from './identity';

// Auth models
export * from './auth';

// Payment models
export * from './payment/payment.model';

// Finance models
export * from './finance/finance.model';

// Legacy exports for backward compatibility
export type { Product as ProductModel } from './catalog/product.model';
export type { Category as CategoryModel } from './catalog/category.model';
