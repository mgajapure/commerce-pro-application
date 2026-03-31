// src/app/core/models/analytics/analytics.model.ts
// Mirrors all backend analytics DTOs exactly

// ── Filter / Request ──────────────────────────────────────────────────────────

export type ReportType =
  | 'SALES_OVERVIEW' | 'SALES_BY_DATE' | 'SALES_BY_PRODUCT' | 'SALES_BY_CATEGORY' | 'SALES_BY_CHANNEL'
  | 'INVENTORY_STOCK_VALUE' | 'INVENTORY_MOVEMENT' | 'INVENTORY_AGEING'
  | 'ORDER_SUMMARY' | 'ORDER_FULFILLMENT_RATE' | 'ORDER_CANCELLATION_RATE'
  | 'CUSTOMER_LTV' | 'CUSTOMER_COHORT' | 'CUSTOMER_CHURN' | 'CUSTOMER_ACQUISITION'
  | 'FINANCIAL_REVENUE' | 'FINANCIAL_MARGIN' | 'FINANCIAL_TAX_SUMMARY'
  | 'SHIPPING_ON_TIME_DELIVERY' | 'SHIPPING_CARRIER_PERFORMANCE'
  | 'RETURN_RATE' | 'RETURN_REASON_ANALYSIS'
  | 'CUSTOM';

export type ExportFormat = 'CSV' | 'EXCEL' | 'JSON' | 'PDF';
export type ReportStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type ScheduleFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY';
export type GroupBy = 'DAY' | 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR';
export type DatePreset = 'TODAY' | 'YESTERDAY' | 'LAST_7_DAYS' | 'LAST_30_DAYS' | 'THIS_MONTH' | 'LAST_MONTH' | 'THIS_QUARTER' | 'THIS_YEAR' | 'LAST_YEAR' | 'CUSTOM';

export interface ReportFilter {
  reportType: ReportType;
  dateFrom?: string;         // yyyy-MM-dd
  dateTo?: string;           // yyyy-MM-dd
  datePreset?: DatePreset;
  productIds?: string[];
  categoryIds?: string[];
  brandIds?: string[];
  warehouseIds?: string[];
  carrierIds?: string[];
  customerIds?: string[];
  orderStatuses?: string[];
  channels?: string[];
  groupBy?: GroupBy;
  columns?: string[];
  customFilters?: Record<string, string>;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
  exportFormat?: ExportFormat;
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

export interface AnalyticsDashboard {
  revenueToday: number;
  revenueMtd: number;
  revenueYtd: number;
  previousMonthRevenue: number;
  revenueGrowthPct: number | null;
  ordersToday: number;
  ordersMtd: number;
  aovMtd: number;
  newCustomersMtd: number;
  totalActiveCustomers: number;
  totalInventoryValue: number;
  lowStockSkus: number;
  outOfStockSkus: number;
  fulfillmentRateMtd: number;
  onTimeDeliveryRateMtd: number;
  cancellationRateMtd: number;
  returnRateMtd: number;
  revenueLast30Days: SalesTrendPoint[];
}

export interface SalesTrendPoint {
  period: string;
  revenue: number;
  orders: number;
  aov?: number;
  units?: number;
}

// ── Sales ─────────────────────────────────────────────────────────────────────

export interface SalesOverview {
  totalRevenue: number;
  previousPeriodRevenue: number;
  revenueGrowthPct: number | null;
  totalOrders: number;
  previousPeriodOrders: number;
  orderGrowthPct: number | null;
  averageOrderValue: number;
  previousPeriodAov: number;
  aovGrowthPct: number | null;
  totalDiscount: number;
  totalTax: number;
  totalShipping: number;
  totalUnits: number;
  trend: SalesTrendPoint[];
}

export interface SalesReport {
  totalRevenue: number;
  previousRevenue: number;
  revenueChangePct: number | null;
  totalDiscount: number;
  totalTax: number;
  totalShipping: number;
  totalRefunded: number;
  netRevenue: number;
  totalOrders: number;
  previousOrders: number;
  ordersChangePct: number | null;
  averageOrderValue: number;
  totalUnits: number;
  rows: SalesRow[];
  topProducts: TopProduct[];
}

export interface SalesRow {
  dimension: string;
  dimensionId?: string;
  revenue: number;
  discount: number;
  tax: number;
  shipping: number;
  refunded: number;
  orders: number;
  units: number;
  aov: number;
  sharePct: number;
}

export interface TopProduct {
  productId: string;
  productName: string;
  sku: string;
  category: string;
  unitsSold: number;
  revenue: number;
}

// ── Inventory ─────────────────────────────────────────────────────────────────

export interface InventoryReport {
  totalStockValue: number;
  totalSkus: number;
  inStockSkus: number;
  lowStockSkus: number;
  outOfStockSkus: number;
  overstockSkus: number;
  averageUnitCost: number;
  stockValueRows: StockValueRow[];
  totalMovements: number;
  inboundMovements: number;
  outboundMovements: number;
  adjustmentMovements: number;
  returnMovements: number;
  movementRows: StockMovementRow[];
  ageingRows: StockAgeingRow[];
}

export interface StockValueRow {
  productId: string;
  productName: string;
  sku: string;
  category: string;
  warehouseName: string;
  quantityOnHand: number;
  reserved: number;
  available: number;
  unitCost: number;
  totalValue: number;
  stockStatus: string;
  binLocation: string;
}

export interface StockMovementRow {
  movementId: string;
  productName: string;
  sku: string;
  warehouseName: string;
  movementType: string;
  quantity: number;
  previousQuantity: number;
  newQuantity: number;
  reason: string;
  reference: string;
  createdAt: string;
  createdBy: string;
}

export interface StockAgeingRow {
  productId: string;
  productName: string;
  sku: string;
  warehouseName: string;
  quantityOnHand: number;
  totalValue: number;
  daysSinceLastMovement: number;
  ageingBucket: string;
  lastMovementDate: string;
}

// ── Orders ────────────────────────────────────────────────────────────────────

export interface OrderReport {
  totalOrders: number;
  fulfilledOrders: number;
  cancelledOrders: number;
  returnedOrders: number;
  pendingOrders: number;
  fulfillmentRatePct: number;
  cancellationRatePct: number;
  returnRatePct: number;
  averageFulfillmentHours: number;
  ordersByStatus: Record<string, number>;
  ordersBySource: Record<string, number>;
  trend: OrderTrendRow[];
  rows: OrderDetailRow[];
  totalRows: number;
}

export interface OrderTrendRow {
  period: string;
  totalOrders: number;
  fulfilledOrders: number;
  cancelledOrders: number;
  fulfillmentRatePct: number;
  cancellationRatePct: number;
  revenue: number;
}

export interface OrderDetailRow {
  orderNumber: string;
  customerId: string;
  customerName: string;
  customerEmail: string;
  status: string;
  fulfillmentStatus: string;
  paymentStatus: string;
  source: string;
  totalAmount: number;
  itemCount: number;
  createdAt: string;
  shippedAt: string;
  deliveredAt: string;
  cancellationReason: string;
  carrierName: string;
  trackingNumber: string;
}

// ── Customers ─────────────────────────────────────────────────────────────────

export interface CustomerReport {
  totalCustomers: number;
  activeCustomers: number;
  newCustomersInPeriod: number;
  churned: number;
  averageLtv: number;
  medianLtv: number;
  topDecileLtv: number;
  customersByTier: Record<string, number>;
  customersByAcquisitionSource: Record<string, number>;
  ltvRows: CustomerLtvRow[];
  cohortRows: CohortRow[];
  churnRows: ChurnRow[];
  trend: CustomerTrendRow[];
}

export interface CustomerLtvRow {
  customerId: string;
  customerName: string;
  email: string;
  tier: string;
  status: string;
  totalOrders: number;
  lifetimeSpend: number;
  averageOrderValue: number;
  firstOrderAt: string;
  lastOrderAt: string;
  daysSinceLastOrder: number;
  loyaltyPoints: number;
  acquisitionSource: string;
  segment: string;
}

export interface CohortRow {
  cohortMonth: string;
  cohortSize: number;
  retentionByMonth: number[];
}

export interface ChurnRow {
  customerId: string;
  customerName: string;
  email: string;
  tier: string;
  lifetimeSpend: number;
  totalOrders: number;
  lastOrderAt: string;
  daysSinceLastOrder: number;
  churnRisk: number;
}

export interface CustomerTrendRow {
  period: string;
  newCustomers: number;
  activeCustomers: number;
  churnedCustomers: number;
  avgOrderValue: number;
  totalRevenue: number;
}

// ── Financial ─────────────────────────────────────────────────────────────────

export interface FinancialReport {
  grossRevenue: number;
  totalDiscount: number;
  netRevenue: number;
  totalCogs: number;
  grossProfit: number;
  grossMarginPct: number;
  totalTax: number;
  totalShipping: number;
  totalRefunds: number;
  netProfit: number;
  previousGrossRevenue: number;
  revenueChangePct: number | null;
  previousGrossMarginPct: number;
  marginChangePct: number | null;
  totalTaxCollected: number;
  taxByRate: Record<string, number>;
  marginRows: MarginRow[];
  revenueTrend: RevenueTrendRow[];
  taxSummaryRows: TaxSummaryRow[];
}

export interface MarginRow {
  dimension: string;
  dimensionId: string;
  revenue: number;
  cogs: number;
  grossProfit: number;
  marginPct: number;
  unitsSold: number;
  avgSellingPrice: number;
  avgCostPrice: number;
}

export interface RevenueTrendRow {
  period: string;
  grossRevenue: number;
  netRevenue: number;
  cogs: number;
  grossProfit: number;
  grossMarginPct: number;
  tax: number;
  refunds: number;
}

export interface TaxSummaryRow {
  period: string;
  orders: number;
  taxableRevenue: number;
  taxCollected: number;
  effectiveTaxRate: number;
}

// ── Shipping ──────────────────────────────────────────────────────────────────

export interface ShippingReport {
  totalShipments: number;
  deliveredShipments: number;
  onTimeDeliveries: number;
  lateDeliveries: number;
  inTransit: number;
  lost: number;
  returnedToSender: number;
  onTimeRatePct: number;
  averageDeliveryDays: number;
  carrierPerformance: CarrierPerformance[];
  rows: ShipmentDetailRow[];
  totalRows: number;
  trend: ShippingTrendRow[];
}

export interface CarrierPerformance {
  carrierId: string;
  carrierName: string;
  totalShipments: number;
  delivered: number;
  onTime: number;
  late: number;
  lost: number;
  returnedToSender: number;
  onTimeRatePct: number;
  avgDeliveryDays: number;
  totalShippingCost: number;
  avgShippingCost: number;
  avgWeightGrams: number;
}

export interface ShipmentDetailRow {
  shipmentNumber: string;
  orderNumber: string;
  carrierName: string;
  trackingNumber: string;
  status: string;
  shippedAt: string;
  estimatedDeliveryDate: string;
  actualDeliveryDate: string;
  deliveryDays: number;
  onTime: boolean;
  shippingCost: number;
  recipientCity: string;
  recipientCountry: string;
}

export interface ShippingTrendRow {
  period: string;
  shipments: number;
  onTimeRatePct: number;
  avgDeliveryDays: number;
  totalCost: number;
}

// ── Returns ───────────────────────────────────────────────────────────────────

export interface ReturnReport {
  totalOrdersInPeriod: number;
  ordersWithReturns: number;
  totalReturnedUnits: number;
  returnRatePct: number;
  returnedValueAmount: number;
  refundedAmount: number;
  ordersByReason: Record<string, number>;
  productReturnRates: ProductReturnRate[];
  trend: ReturnTrendRow[];
  rows: ReturnDetailRow[];
  totalRows: number;
}

export interface ProductReturnRate {
  productId: string;
  productName: string;
  sku: string;
  category: string;
  unitsSold: number;
  unitsReturned: number;
  returnRatePct: number;
  returnedValue: number;
}

export interface ReturnTrendRow {
  period: string;
  returnsInitiated: number;
  unitsReturned: number;
  returnRatePct: number;
  refundedAmount: number;
}

export interface ReturnDetailRow {
  orderNumber: string;
  customerName: string;
  productName: string;
  sku: string;
  returnedQuantity: number;
  refundedAmount: number;
  reason: string;
  orderStatus: string;
  createdAt: string;
}

// ── Saved / Scheduled Reports ─────────────────────────────────────────────────

export interface SavedReportResponse {
  id: string;
  name: string;
  description: string;
  reportType: ReportType;
  filterParams: ReportFilter | null;
  defaultFormat: ExportFormat;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface SavedReportRequest {
  name: string;
  description?: string;
  reportType: ReportType;
  filterParams?: ReportFilter;
  defaultFormat?: ExportFormat;
  isPublic?: boolean;
}

export interface ScheduledReportResponse {
  id: string;
  savedReportId: string;
  savedReportName: string;
  name: string;
  frequency: ScheduleFrequency;
  dayOfPeriod?: number;
  runAtTime?: string;
  recipientEmails: string[];
  format: ExportFormat;
  emailSubject: string;
  isActive: boolean;
  lastRunAt?: string;
  nextRunAt?: string;
  runCount: number;
  lastRunStatus?: string;
  createdAt: string;
  createdBy: string;
}

export interface ScheduledReportRequest {
  savedReportId: string;
  name: string;
  frequency: ScheduleFrequency;
  dayOfPeriod?: number;
  runAtTime?: string;
  recipientEmails: string;
  format?: ExportFormat;
  emailSubject?: string;
  emailBody?: string;
  isActive?: boolean;
}

export interface ReportExecution {
  id: string;
  reportType: ReportType;
  status: ReportStatus;
  format: ExportFormat;
  fileName?: string;
  fileSizeBytes?: number;
  rowCount?: number;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  downloadUrl?: string;
  savedReportId?: string;
  scheduledReportId?: string;
  requestedBy: string;
  createdAt: string;
}
