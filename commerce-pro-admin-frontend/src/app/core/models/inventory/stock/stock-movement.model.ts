// Stock Movement model - matches backend StockMovementDTO

export type MovementType = 'IN' | 'OUT' | 'ADJUSTMENT' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'RETURN' | 'DAMAGED';

export type ReferenceType = 'PURCHASE_ORDER' | 'SALES_ORDER' | 'TRANSFER' | 'ADJUSTMENT' | 'RETURN' | 'COUNT';

export interface StockMovement {
  id: string;
  inventoryId: string;
  productId: string;
  warehouseId: string;
  productName?: string;
  warehouseName?: string;

  type: MovementType;
  quantity: number;
  previousQuantity: number;
  newQuantity: number;

  reason?: string;
  notes?: string;
  reference?: string;
  referenceType?: ReferenceType;

  createdBy?: string;
  createdAt: Date;
}

export interface StockUpdateRequest {
  quantity: number;
  adjust?: boolean;
  reason: string;
  notes?: string;
  reference?: string;
  referenceType?: string;
}

/**
 * Matches backend StockTransferRequestDTO.
 * For multi-product transfers use BatchStockTransferRequest.
 */
export interface StockTransferRequest {
  fromWarehouseId: string;
  toWarehouseId: string;
  productId: string;  // single product — use BatchStockTransferRequest for multi-product
  quantity: number;
  notes?: string;
  reference?: string;
}

/** Matches backend BatchStockTransferRequestDTO */
export interface BatchStockTransferLine {
  productId: string;
  quantity: number;
  notes?: string;
}

export interface BatchStockTransferRequest {
  fromWarehouseId: string;
  toWarehouseId: string;
  lines: BatchStockTransferLine[];
  notes?: string;
  reference?: string;
}
