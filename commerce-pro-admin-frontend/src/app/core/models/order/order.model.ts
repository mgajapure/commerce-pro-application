// ─── Enums — exactly match backend Java enums ────────────────────────────────

export type OrderStatus =
  | 'DRAFT' | 'PENDING_PAYMENT' | 'PAYMENT_FAILED' | 'CONFIRMED'
  | 'ON_HOLD' | 'PROCESSING' | 'PARTIALLY_FULFILLED' | 'FULFILLED'
  | 'SHIPPED' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED'
  | 'RETURN_INITIATED' | 'RETURN_IN_TRANSIT' | 'RETURN_RECEIVED'
  | 'REFUNDED' | 'PARTIALLY_REFUNDED' | 'CLOSED';

export type PaymentStatus =
  | 'PENDING' | 'AUTHORIZED' | 'CAPTURED' | 'PARTIALLY_CAPTURED'
  | 'FAILED' | 'VOIDED' | 'REFUND_PENDING' | 'REFUNDED'
  | 'PARTIALLY_REFUNDED' | 'CHARGEBACK' | 'DISPUTED';

export type FulfillmentStatus =
  | 'UNFULFILLED' | 'PARTIALLY_FULFILLED' | 'FULFILLED' | 'CANCELLED';

export type OrderSource =
  | 'STOREFRONT' | 'MANUAL' | 'API' | 'IMPORT' | 'MOBILE_APP';

// ─── Sub-models — match backend embedded/response objects ────────────────────

export interface OrderAddress {
  fullName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  phone?: string;
}

export interface OrderItemResponse {
  id: string;
  productId: string;
  sku: string;
  productName: string;
  productImageUrl?: string;
  variantInfo?: string;
  unitPrice: number;
  costPrice?: number;
  itemDiscount: number;
  taxRate: number;
  taxAmount: number;
  lineTotal: number;
  quantity: number;
  fulfilledQuantity: number;
  returnedQuantity: number;
  pendingFulfillmentQuantity: number;
  createdAt: string;
}

export interface OrderStatusHistory {
  id: string;
  previousStatus?: string;
  newStatus: string;
  reason?: string;
  changedBy?: string;
  createdAt: string;
}

// ─── Main response models — match backend *ResponseDTO ───────────────────────

/** Full order — returned by GET /api/v1/orders/{id} */
export interface OrderResponse {
  id: string;
  orderNumber: string;
  customerId?: string;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  fulfillmentStatus: FulfillmentStatus;
  source: OrderSource;
  subtotal: number;
  discountAmount: number;
  shippingCost: number;
  taxAmount: number;
  totalAmount: number;
  refundedAmount: number;
  couponCode?: string;
  discountPercentage?: number;
  currency: string;
  shippingAddress?: OrderAddress;
  billingAddress?: OrderAddress;
  shippingMethod?: string;
  trackingNumber?: string;
  carrier?: string;
  riskScore: number;
  isFlagged: boolean;
  holdReason?: string;
  cancellationReason?: string;
  internalNotes?: string;
  customerNotes?: string;
  confirmedAt?: string;
  shippedAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
  items: OrderItemResponse[];
  statusHistory: OrderStatusHistory[];
  totalQuantity: number;
  isEditable: boolean;
  isCancellable: boolean;
}

/** Light summary — returned in paginated list GET /api/v1/orders */
export interface OrderSummaryResponse {
  id: string;
  orderNumber: string;
  customerId?: string;
  customerName: string;
  customerEmail: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  fulfillmentStatus: FulfillmentStatus;
  source: OrderSource;
  totalAmount: number;
  currency: string;
  totalQuantity: number;
  itemCount: number;
  isFlagged: boolean;
  riskScore: number;
  createdAt: string;
  updatedAt: string;
}

/** Stats — returned by GET /api/v1/orders/stats */
export interface OrderStatsResponse {
  totalOrders: number;
  ordersToday: number;
  pendingOrders: number;
  processingOrders: number;
  confirmedOrders: number;
  shippedOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  flaggedOrders: number;
  onHoldOrders: number;
  totalRevenue: number;
  revenueToday: number;
  averageOrderValue: number;
  statusBreakdown: Record<string, number>;
}

// ─── Request models — match backend *RequestDTO ───────────────────────────────

export interface OrderItemRequest {
  productId: string;
  variantInfo?: string;
  quantity: number;
  unitPrice?: number;
  itemDiscount?: number;
  taxRate?: number;
}

export interface CreateOrderRequest {
  customerId?: string;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  items: OrderItemRequest[];
  shippingAddress?: OrderAddress;
  billingAddress?: OrderAddress;
  source?: OrderSource;
  couponCode?: string;
  shippingCost?: number;
  discountAmount?: number;
  shippingMethod?: string;
  customerNotes?: string;
  internalNotes?: string;
  currency?: string;
}

export interface UpdateOrderRequest {
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
  shippingAddress?: OrderAddress;
  billingAddress?: OrderAddress;
  items?: OrderItemRequest[];
  shippingCost?: number;
  discountAmount?: number;
  couponCode?: string;
  shippingMethod?: string;
  internalNotes?: string;
  customerNotes?: string;
}

export interface HoldOrderRequest {
  reason: string;
}

export interface CancelOrderRequest {
  reason: string;
}

export interface TrackingUpdateRequest {
  trackingNumber: string;
  carrier: string;
}

export interface BulkOrderActionRequest {
  orderIds: string[];
  targetStatus: OrderStatus;
  reason?: string;
}
