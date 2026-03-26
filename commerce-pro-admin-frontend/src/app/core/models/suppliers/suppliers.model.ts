// src/app/core/models/suppliers/suppliers.model.ts
// Mirrors the Commerce Pro backend supplier module DTOs exactly

// ─── Supplier Directory ─────────────────────────────────────────────────────

export interface Supplier {
  id: string;
  name: string;
  code: string;
  contactPerson: string;
  email: string;
  phone: string;
  address: string;
  city: string;
  state: string;
  country: string;
  postalCode: string;
  website: string;
  description: string;
  paymentTerms: string;
  paymentTermsDays: number;
  leadTimeDays: number;
  rating: number;
  minOrderValue: number;
  preferredCurrency: string;
  certifications: string;
  notes: string;
  isActive: boolean;
  isPreferred: boolean;
  productCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierRequest {
  name: string;
  code: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
  website?: string;
  description?: string;
  paymentTerms?: string;
  paymentTermsDays?: number;
  leadTimeDays?: number;
  minOrderValue?: number;
  preferredCurrency?: string;
  certifications?: string;
  notes?: string;
  isActive?: boolean;
  isPreferred?: boolean;
}

export interface SupplierStats {
  total: number;
  active: number;
  preferred: number;
}

// ─── Supplier Product ───────────────────────────────────────────────────────

export interface SupplierProduct {
  id: string;
  supplierId: string;
  supplierName: string;
  productId: string;
  productName: string;
  sku: string;
  supplierSku: string;
  unitCost: number;
  currency: string;
  minOrderQuantity: number;
  leadTimeDays: number;
  isActive: boolean;
  isPrimary: boolean;
  notes: string;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierProductRequest {
  supplierId: string;
  supplierName: string;
  productId: string;
  productName: string;
  sku: string;
  supplierSku: string;
  unitCost: number;
  currency: string;
  minOrderQuantity: number;
  leadTimeDays: number;
  isActive: boolean;
  isPrimary: boolean;
  notes: string;
}

// ─── Purchase Order ─────────────────────────────────────────────────────────

export interface PurchaseOrder {
  id: string;
  poNumber: string;
  supplierId: string;
  supplierName: string;
  status: string;
  orderDate: string;
  expectedDeliveryDate: string;
  actualDeliveryDate: string;
  subtotal: number;
  taxAmount: number;
  shippingCost: number;
  totalAmount: number;
  currency: string;
  notes: string;
  createdBy: string;
  approvedBy: string;
  approvedAt: string;
  items: PurchaseOrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface PurchaseOrderItem {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  quantityOrdered: number;
  quantityReceived: number;
  unitCost: number;
  totalCost: number;
}

export interface PurchaseOrderRequest {
  supplierId: string;
  supplierName: string;
  status?: string;
  orderDate?: string;
  expectedDeliveryDate?: string;
  notes?: string;
  currency?: string;
  items: PurchaseOrderItemRequest[];
}

export interface PurchaseOrderItemRequest {
  productId: string;
  productName: string;
  sku: string;
  quantityOrdered: number;
  unitCost: number;
  notes?: string;
}

// ─── Procurement Request ────────────────────────────────────────────────────

export interface ProcurementRequest {
  id: string;
  requestNumber: string;
  title: string;
  description: string;
  priority: string;
  status: string;
  requestedBy: string;
  supplierId: string;
  supplierName: string;
  estimatedCost: number;
  actualCost: number;
  currency: string;
  requiredByDate: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProcurementRequestData {
  title: string;
  description: string;
  priority: string;
  supplierId?: string;
  supplierName?: string;
  estimatedCost?: number;
  currency?: string;
  requiredByDate?: string;
  notes?: string;
}

// ─── Supplier Evaluation ────────────────────────────────────────────────────

export interface SupplierEvaluation {
  id: string;
  supplierId: string;
  supplierName: string;
  evaluationDate: string;
  evaluatedBy: string;
  overallScore: number;
  qualityScore: number;
  deliveryScore: number;
  pricingScore: number;
  communicationScore: number;
  complianceScore: number;
  period: string;
  comments: string;
  recommendations: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierEvaluationRequest {
  supplierId: string;
  supplierName: string;
  evaluationDate: string;
  evaluatedBy: string;
  overallScore: number;
  qualityScore: number;
  deliveryScore: number;
  pricingScore: number;
  communicationScore: number;
  complianceScore: number;
  period: string;
  comments: string;
  recommendations: string;
  status: string;
}

// ─── Supplier Contract ──────────────────────────────────────────────────────

export interface SupplierContract {
  id: string;
  contractNumber: string;
  supplierId: string;
  supplierName: string;
  title: string;
  description: string;
  contractType: string;
  status: string;
  startDate: string;
  endDate: string;
  totalValue: number;
  currency: string;
  paymentTerms: string;
  autoRenew: boolean;
  renewalNoticeDays: number;
  documentUrl: string;
  terms: string;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierContractRequest {
  supplierId: string;
  supplierName?: string;
  title: string;
  description?: string;
  contractType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  totalValue?: number;
  currency?: string;
  paymentTerms?: string;
  autoRenew?: boolean;
  renewalNoticeDays?: number;
  documentUrl?: string;
  terms?: string;
}

// ─── Filter ─────────────────────────────────────────────────────────────────

export interface SupplierFilter {
  search?: string;
  status?: string;
  isPreferred?: boolean;
}
