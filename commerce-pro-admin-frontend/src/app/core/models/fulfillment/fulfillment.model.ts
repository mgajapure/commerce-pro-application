// src/app/core/models/fulfillment/fulfillment.model.ts
// All fulfillment models — exactly match backend DTOs

// ── Enums ─────────────────────────────────────────────────────────────────────

export type PickListStatus = 'GENERATED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type PickListType   = 'SINGLE' | 'BATCH' | 'ZONE' | 'WAVE';
export type ShipmentStatus =
  | 'LABEL_CREATED' | 'PICKED_UP' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY'
  | 'DELIVERED' | 'ATTEMPTED_DELIVERY' | 'RETURNED_TO_SENDER' | 'LOST' | 'EXCEPTION';
export type CarrierStatus = 'ACTIVE' | 'INACTIVE';

// ── Fulfillment Queue ─────────────────────────────────────────────────────────

export interface FulfillmentQueueItem {
  orderId: string;
  orderNumber: string;
  customerName: string;
  customerEmail: string;
  status: string;
  fulfillmentStatus: string;
  totalAmount: number;
  currency: string;
  totalItems: number;
  pendingItems: number;
  confirmedAt?: string;
  createdAt: string;
  isFlagged: boolean;
  riskScore: number;
  shippingMethod?: string;
}

// ── Pick List ─────────────────────────────────────────────────────────────────

export interface PickListItem {
  id: string;
  orderId: string;
  orderNumber: string;
  orderItemId: string;
  productId?: string;
  sku: string;
  productName: string;
  variantInfo?: string;
  binLocation?: string;
  quantityRequired: number;
  quantityPicked: number;
  isPicked: boolean;
  notes?: string;
  createdAt: string;
}

export interface PickList {
  id: string;
  pickListNumber: string;
  type: PickListType;
  status: PickListStatus;
  warehouseId?: string;
  warehouseName?: string;
  assignedToUserId?: string;
  assignedToName?: string;
  totalOrders: number;
  totalItems: number;
  completedItems: number;
  pendingItems: number;
  completionPercentage: number;
  notes?: string;
  generatedAt: string;
  assignedAt?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  items: PickListItem[];
}

export interface PickListSummary {
  id: string;
  pickListNumber: string;
  type: PickListType;
  status: PickListStatus;
  warehouseName?: string;
  assignedToName?: string;
  totalOrders: number;
  totalItems: number;
  completedItems: number;
  completionPercentage: number;
  generatedAt: string;
  completedAt?: string;
  createdAt: string;
}

// ── Shipment ──────────────────────────────────────────────────────────────────

export interface TrackingEvent {
  id: string;
  status: ShipmentStatus;
  location?: string;
  description: string;
  eventTime: string;
  createdAt: string;
  createdBy?: string;
}

export interface ShipmentDetail {
  id: string;
  shipmentNumber: string;
  orderId: string;
  orderNumber: string;
  carrierId?: string;
  carrierName?: string;
  trackingNumber?: string;
  trackingUrl?: string;
  status: ShipmentStatus;
  shippingMethod?: string;
  serviceLevel?: string;
  estimatedDeliveryDate?: string;
  actualDeliveryDate?: string;
  recipientName: string;
  recipientPhone?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  weightGrams?: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  shippingCost?: number;
  labelUrl?: string;
  notes?: string;
  exceptionReason?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  trackingEvents: TrackingEvent[];
}

export interface ShipmentSummary {
  id: string;
  shipmentNumber: string;
  orderId: string;
  orderNumber: string;
  carrierName?: string;
  trackingNumber?: string;
  status: ShipmentStatus;
  shippingMethod?: string;
  estimatedDeliveryDate?: string;
  actualDeliveryDate?: string;
  recipientName: string;
  city: string;
  country: string;
  shippingCost?: number;
  createdAt: string;
  updatedAt: string;
}

// ── Carrier ───────────────────────────────────────────────────────────────────

export interface Carrier {
  id: string;
  name: string;
  code: string;
  status: CarrierStatus;
  trackingUrlTemplate?: string;
  logoUrl?: string;
  isDefault: boolean;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

// ── Shipping Rule ─────────────────────────────────────────────────────────────

export interface ShippingRule {
  id: string;
  name: string;
  description?: string;
  carrierId?: string;
  carrierName?: string;
  conditionType: string;
  conditionMin?: number;
  conditionMax?: number;
  conditionValue?: string;
  shippingMethod: string;
  serviceLevel?: string;
  priority: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

// ── Stats ─────────────────────────────────────────────────────────────────────

export interface FulfillmentStats {
  queueSize: number;
  generatedPickLists: number;
  inProgressPickLists: number;
  completedPickListsToday: number;
  totalShipments: number;
  inTransitShipments: number;
  deliveredShipments: number;
  overdueShipments: number;
  exceptionShipments: number;
  pickListStatusBreakdown: Record<string, number>;
  shipmentStatusBreakdown: Record<string, number>;
}

export interface ShipmentStats {
  totalShipments: number;
  shipmentsToday: number;
  inTransit: number;
  outForDelivery: number;
  delivered: number;
  exceptions: number;
  overdue: number;
  statusBreakdown: Record<string, number>;
}

// ── Request models ────────────────────────────────────────────────────────────

export interface CreatePickListRequest {
  orderIds: string[];
  type?: PickListType;
  warehouseId?: string;
  warehouseName?: string;
  notes?: string;
}

export interface AssignPickListRequest {
  userId: string;
  userName?: string;
}

export interface UpdatePickItemRequest {
  quantityPicked: number;
  notes?: string;
}

export interface CreateShipmentRequest {
  orderId: string;
  carrierId?: string;
  trackingNumber?: string;
  shippingMethod?: string;
  serviceLevel?: string;
  estimatedDeliveryDate?: string;
  recipientName?: string;
  recipientPhone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  weightGrams?: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  shippingCost?: number;
  notes?: string;
}

export interface AddTrackingEventRequest {
  status: string;
  description: string;
  location?: string;
  eventTime?: string;
}

export interface CarrierRequest {
  name: string;
  code: string;
  trackingUrlTemplate?: string;
  logoUrl?: string;
  isDefault?: boolean;
  notes?: string;
}

export interface ShippingRuleRequest {
  name: string;
  description?: string;
  carrierId?: string;
  conditionType: string;
  conditionMin?: number;
  conditionMax?: number;
  conditionValue?: string;
  shippingMethod: string;
  serviceLevel?: string;
  priority?: number;
  isActive?: boolean;
}

// ── Update Shipment Request — matches backend UpdateShipmentRequest ───────────

export interface UpdateShipmentRequest {
  carrierId?: string;
  trackingNumber?: string;
  shippingMethod?: string;
  serviceLevel?: string;
  estimatedDeliveryDate?: string;  // ISO date: YYYY-MM-DD
  weightGrams?: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  shippingCost?: number;
  notes?: string;
  exceptionReason?: string;
}
