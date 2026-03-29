// Inventory models barrel export

// Warehouse models
export * from './warehouse/warehouse.model';

// Stock movement models - explicit exports to avoid conflicts
export type {
  MovementType,
  ReferenceType,
  StockMovement
} from './stock/stock-movement.model';

// Re-export stock request types from catalog to maintain compatibility
export type { StockUpdateRequest } from '../catalog/product-request.model';
export type { StockStatus } from '../catalog/product.model';

// Low stock models
export * from './stock/low-stock.model';

// Inventory models
export * from './inventory-item.model';
export * from './inventory-stats.model';
export * from './demand-forecast.model';

// Stock adjustment model
export * from './stock-adjustment.model';

// Stock transfer model
export * from './stock-transfer.model';

// Note: StockTransferRequest is defined in stock-movement.model.ts
export type { StockTransferRequest, BatchStockTransferRequest, BatchStockTransferLine } from './stock/stock-movement.model';
