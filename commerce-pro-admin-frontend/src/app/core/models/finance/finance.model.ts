// finance.model.ts — mirrors com.commerce_pro_backend.finance.dto.FinanceDTOs exactly

// ── Enums ─────────────────────────────────────────────────────────────────────

export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'VOID' | 'WRITTEN_OFF';
export type VendorStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'BLACKLISTED';
export type VendorInvoiceStatus = 'RECEIVED' | 'UNDER_REVIEW' | 'APPROVED' | 'SCHEDULED' | 'PARTIALLY_PAID' | 'PAID' | 'REJECTED' | 'DISPUTED' | 'VOID';
export type TaxType = 'SALES_TAX' | 'VAT' | 'GST' | 'HST' | 'PST' | 'EXCISE' | 'WITHHOLDING' | 'CUSTOM';
export type TaxApplicability = 'ALL' | 'PRODUCT_ONLY' | 'SERVICE_ONLY' | 'SHIPPING_ONLY' | 'DIGITAL_GOODS';
export type ExpenseStatus = 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'PAID' | 'CANCELLED';
export type JournalEntryType = 'ORDER_REVENUE' | 'ORDER_REFUND' | 'ORDER_TAX' | 'ORDER_SHIPPING' | 'COGS' | 'EXPENSE' | 'AP_PAYMENT' | 'AR_RECEIPT' | 'PAYMENT_FEE' | 'CHARGEBACK' | 'MANUAL_ADJUSTMENT' | 'DEPRECIATION' | 'EXCHANGE_GAIN_LOSS';
export type PeriodStatus = 'OPEN' | 'CLOSING' | 'CLOSED' | 'LOCKED';
export type BudgetStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'ACTIVE' | 'CLOSED';
export type AccountType = 'DEBIT' | 'CREDIT';

// ── Financial Period ──────────────────────────────────────────────────────────

export interface FinancialPeriodDTO {
  id: string;
  fiscalYear: number;
  periodNumber: number;
  periodName: string;
  periodType: string;
  startDate: string;
  endDate: string;
  status: PeriodStatus;
  closedAt?: string;
  closedBy?: string;
  lockedAt?: string;
  notes?: string;
  createdAt: string;
}

// ── Vendor ────────────────────────────────────────────────────────────────────

export interface VendorDTO {
  id: string;
  vendorCode: string;
  name: string;
  companyName?: string;
  email: string;
  phone?: string;
  website?: string;
  taxId?: string;
  status: VendorStatus;
  addressLine1?: string;
  city?: string;
  state?: string;
  country?: string;
  paymentTerms?: string;
  paymentTermsDays?: number;
  preferredCurrency: string;
  bankAccountLast4?: string;
  totalPayable: number;
  totalPaid: number;
  category?: string;
  notes?: string;
  tags?: string;
  createdAt: string;
  updatedAt: string;
}

export interface VendorSummaryDTO {
  id: string;
  vendorCode: string;
  name: string;
  email: string;
  status: VendorStatus;
  paymentTerms?: string;
  totalPayable: number;
  category?: string;
  createdAt: string;
}

// ── Customer Invoice ──────────────────────────────────────────────────────────

export interface CustomerInvoiceDTO {
  id: string;
  invoiceNumber: string;
  orderId?: string;
  orderNumber?: string;
  customerId?: string;
  customerName: string;
  customerEmail: string;
  status: InvoiceStatus;
  issuedAt?: string;
  dueDate: string;
  paidAt?: string;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  shippingAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
  currency: string;
  paymentTerms?: string;
  notes?: string;
  agingBucket: number;
  overdue: boolean;
  items: CustomerInvoiceItemDTO[];
  createdAt: string;
  updatedAt: string;
}

export interface CustomerInvoiceSummaryDTO {
  id: string;
  invoiceNumber: string;
  customerName: string;
  customerEmail: string;
  status: InvoiceStatus;
  dueDate: string;
  totalAmount: number;
  balanceDue: number;
  currency: string;
  overdue: boolean;
  agingBucket: number;
  issuedAt?: string;
  createdAt: string;
}

export interface CustomerInvoiceItemDTO {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  discountAmount: number;
  taxRate: number;
  taxAmount: number;
  lineTotal: number;
  sku?: string;
  sortOrder: number;
}

export interface ARSummaryDTO {
  totalOutstanding: number;
  current: number;
  days1To30: number;
  days31To60: number;
  days61To90: number;
  days90Plus: number;
  overdueInvoiceCount: number;
  overdueAmount: number;
  dsoDays: number;
  overdueInvoices: CustomerInvoiceSummaryDTO[];
}

// ── Vendor Invoice ────────────────────────────────────────────────────────────

export interface VendorInvoiceDTO {
  id: string;
  vendorInvoiceNumber: string;
  vendorReference?: string;
  vendorId: string;
  vendorName?: string;
  status: VendorInvoiceStatus;
  receivedAt: string;
  dueDate: string;
  scheduledPaymentDate?: string;
  approvedAt?: string;
  paidAt?: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
  currency: string;
  earlyPaymentDiscountPct?: number;
  earlyPaymentDiscountDays?: number;
  description?: string;
  notes?: string;
  overdue: boolean;
  agingBucket: number;
  items: VendorInvoiceItemDTO[];
  createdAt: string;
  updatedAt: string;
}

export interface VendorInvoiceSummaryDTO {
  id: string;
  vendorInvoiceNumber: string;
  vendorReference?: string;
  vendorName?: string;
  status: VendorInvoiceStatus;
  dueDate: string;
  totalAmount: number;
  balanceDue: number;
  currency: string;
  overdue: boolean;
  agingBucket: number;
  createdAt: string;
}

export interface VendorInvoiceItemDTO {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  taxAmount: number;
  lineTotal: number;
  sortOrder: number;
}

export interface APSummaryDTO {
  totalOutstanding: number;
  current: number;
  days1To30: number;
  days31To60: number;
  days61To90: number;
  days90Plus: number;
  overdueInvoiceCount: number;
  overdueAmount: number;
  dpoDays: number;
  upcomingPayments: VendorInvoiceSummaryDTO[];
  overdueInvoices: VendorInvoiceSummaryDTO[];
}

// ── Tax ───────────────────────────────────────────────────────────────────────

export interface TaxRateDTO {
  id: string;
  rateCode: string;
  name: string;
  description?: string;
  taxType: TaxType;
  rate: number;
  rateDisplay: string;
  jurisdictionId?: string;
  country?: string;
  stateProvince?: string;
  applicability: TaxApplicability;
  isCompound: boolean;
  isInclusive: boolean;
  isActive: boolean;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdAt: string;
}

export interface TaxJurisdictionDTO {
  id: string;
  jurisdictionCode: string;
  name: string;
  country?: string;
  stateProvince?: string;
  isActive: boolean;
  createdAt: string;
}

export interface TaxReportDTO {
  id: string;
  reportRef: string;
  periodId?: string;
  periodName?: string;
  reportType: string;
  jurisdictionName?: string;
  periodStart: string;
  periodEnd: string;
  status: string;
  taxableSales: number;
  exemptSales: number;
  totalSales: number;
  taxCollected: number;
  taxPaidToVendors: number;
  netTaxPayable: number;
  orderCount: number;
  generatedAt?: string;
  filedAt?: string;
  notes?: string;
  createdAt: string;
}

// ── Expense ───────────────────────────────────────────────────────────────────

export interface ExpenseCategoryDTO {
  id: string;
  categoryCode: string;
  name: string;
  description?: string;
  parentId?: string;
  glAccountCode?: string;
  isActive: boolean;
  budgetDefault?: number;
  createdAt: string;
}

export interface ExpenseDTO {
  id: string;
  expenseRef: string;
  title: string;
  description?: string;
  categoryId: string;
  categoryName?: string;
  status: ExpenseStatus;
  expenseDate: string;
  vendorId?: string;
  vendorName?: string;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  currency: string;
  paymentMethod?: string;
  receiptFilePath?: string;
  isRecurring: boolean;
  recurrenceInterval?: string;
  approvedBy?: string;
  approvedAt?: string;
  paidAt?: string;
  notes?: string;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ExpenseSummaryDTO {
  id: string;
  expenseRef: string;
  title: string;
  categoryName?: string;
  status: ExpenseStatus;
  expenseDate: string;
  totalAmount: number;
  currency: string;
  isRecurring: boolean;
  createdAt: string;
}

export interface ExpenseStatsDTO {
  totalMTD: number;
  totalYTD: number;
  pendingCount: number;
  pendingAmount: number;
  byCategory: Record<string, number>;
}

// ── Journal Entry ─────────────────────────────────────────────────────────────

export interface JournalEntryDTO {
  id: string;
  entryRef: string;
  periodId?: string;
  periodName?: string;
  entryType: JournalEntryType;
  status: string;
  entryDate: string;
  description: string;
  reference?: string;
  currency: string;
  sourceType?: string;
  sourceId?: string;
  postedBy?: string;
  reversalReason?: string;
  notes?: string;
  lines: JournalEntryLineDTO[];
  createdAt: string;
}

export interface JournalEntrySummaryDTO {
  id: string;
  entryRef: string;
  entryType: JournalEntryType;
  status: string;
  entryDate: string;
  description: string;
  reference?: string;
  totalDebits: number;
  createdAt: string;
}

export interface JournalEntryLineDTO {
  id: string;
  accountType: AccountType;
  accountCode: string;
  accountName: string;
  amount: number;
  currency: string;
  description?: string;
  sortOrder: number;
}

// ── Budget ────────────────────────────────────────────────────────────────────

export interface BudgetDTO {
  id: string;
  name: string;
  description?: string;
  fiscalYear: number;
  periodId?: string;
  periodName?: string;
  status: BudgetStatus;
  totalRevenueBudget: number;
  totalExpenseBudget: number;
  totalRevenueActual?: number;
  totalExpenseActual?: number;
  netBudget: number;
  netActual?: number;
  netVariance?: number;
  currency: string;
  approvedAt?: string;
  notes?: string;
  lines: BudgetLineDTO[];
  createdAt: string;
}

export interface BudgetLineDTO {
  id: string;
  lineName: string;
  categoryId?: string;
  categoryName?: string;
  lineType: string;
  budgetedAmount: number;
  actualAmount: number;
  variance: number;
  variancePct: number;
  notes?: string;
  sortOrder: number;
}

// ── Revenue & P&L ─────────────────────────────────────────────────────────────

export interface RevenueOverviewDTO {
  grossRevenueToday: number;
  grossRevenueMTD: number;
  grossRevenueYTD: number;
  netRevenueMTD: number;
  netRevenueYTD: number;
  totalRefundsMTD: number;
  totalTaxCollectedMTD: number;
  totalShippingRevenueMTD: number;
  ordersToday: number;
  ordersMTD: number;
  averageOrderValue: number;
  revenueGrowthPct: number;
  outstandingAR: number;
  overdueAR: number;
  outstandingAP: number;
  overdueAP: number;
  totalExpensesMTD: number;
  grossProfitMTD: number;
  grossMarginPct: number;
  trend: RevenueTrendPointDTO[];
}

export interface RevenueTrendPointDTO {
  period: string;
  grossRevenue: number;
  netRevenue: number;
  expenses: number;
}

export interface ProfitLossDTO {
  periodLabel: string;
  startDate: string;
  endDate: string;
  grossRevenue: number;
  salesReturns: number;
  netRevenue: number;
  cogs: number;
  grossProfit: number;
  grossMarginPct: number;
  totalOperatingExpenses: number;
  expenseByCategory: Record<string, number>;
  totalShipping: number;
  gatewayFees: number;
  chargebacks: number;
  ebitda: number;
  totalTax: number;
  netIncome: number;
  netMarginPct: number;
  previousPeriodNetIncome: number;
  netIncomeChangePct: number;
  budgetedNetIncome: number;
  budgetVariance: number;
}

// ── Cash Flow ─────────────────────────────────────────────────────────────────

export interface CashFlowForecastDTO {
  totalArDue30Days: number;
  totalArDue60Days: number;
  totalApDue30Days: number;
  totalApDue60Days: number;
  projectedNetCash30Days: number;
  projectedNetCash60Days: number;
  projectedNetCash90Days: number;
  projections: CashFlowProjectionDTO[];
}

export interface CashFlowProjectionDTO {
  period: string;
  expectedInflows: number;
  expectedOutflows: number;
  netCashFlow: number;
}

// ── Exchange Rate ─────────────────────────────────────────────────────────────

export interface ExchangeRateDTO {
  id: string;
  baseCurrency: string;
  quoteCurrency: string;
  rate: number;
  rateDate: string;
  source: string;
  createdAt: string;
}

export interface ConversionResultDTO {
  originalAmount: number;
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  convertedAmount: number;
  rateDate: string;
}

// ── Request Models ────────────────────────────────────────────────────────────

export interface CreatePeriodRequest {
  fiscalYear: number;
  periodNumber: number;
  periodName: string;
  startDate: string;
  endDate: string;
  periodType?: string;
  notes?: string;
}

export interface CreateVendorRequest {
  name: string;
  companyName?: string;
  email: string;
  phone?: string;
  taxId?: string;
  paymentTerms: string;
  paymentTermsDays?: number;
  preferredCurrency?: string;
  addressLine1?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  category?: string;
  notes?: string;
}

export interface CreateCustomerInvoiceRequest {
  customerId: string;
  orderId?: string;
  dueDate: string;
  paymentTerms: string;
  currency?: string;
  notes?: string;
  items: InvoiceItemRequest[];
}

export interface InvoiceItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  discountAmount?: number;
  taxRateId?: string;
}

export interface RecordPaymentRequest {
  amount: number;
  paymentDate: string;
  paymentMethod?: string;
  reference?: string;
  notes?: string;
}

export interface CreateVendorInvoiceRequest {
  vendorId: string;
  vendorReference: string;
  dueDate: string;
  description?: string;
  currency?: string;
  notes?: string;
  items: VendorInvoiceItemRequest[];
}

export interface VendorInvoiceItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate?: number;
  expenseCategoryId?: string;
}

export interface CreateTaxRateRequest {
  rateCode: string;
  name: string;
  taxType: TaxType;
  rate: number;
  country?: string;
  stateProvince?: string;
  applicability?: TaxApplicability;
  effectiveFrom?: string;
  effectiveTo?: string;
}

export interface CreateExpenseRequest {
  title: string;
  categoryId: string;
  vendorId?: string;
  expenseDate: string;
  amount: number;
  taxAmount?: number;
  currency?: string;
  paymentMethod?: string;
  notes?: string;
  isRecurring?: boolean;
  recurrenceInterval?: string;
}

export interface CreateJournalEntryRequest {
  entryType: JournalEntryType;
  entryDate: string;
  description: string;
  reference?: string;
  periodId?: string;
  lines: JournalLineRequest[];
}

export interface JournalLineRequest {
  accountType: AccountType;
  accountCode: string;
  accountName: string;
  amount: number;
  description?: string;
}

export interface CreateBudgetRequest {
  name: string;
  fiscalYear: number;
  periodId?: string;
  currency?: string;
  lines: BudgetLineRequest[];
}

export interface BudgetLineRequest {
  lineName: string;
  categoryId?: string;
  lineType?: string;
  budgetedAmount: number;
  notes?: string;
}

export interface AddExchangeRateRequest {
  baseCurrency: string;
  quoteCurrency: string;
  rate: number;
  rateDate: string;
  source?: string;
}

// ── Budget Variance ───────────────────────────────────────────────────────────

export interface BudgetVarianceLineDTO {
  lineId: string;
  category: string;
  budgetedAmount: number;
  actualAmount: number;
  varianceAmount: number;
  variancePct: number;
  overBudget: boolean;
}

export interface BudgetVarianceDTO {
  budgetId: string;
  budgetName: string;
  periodStart: string;
  periodEnd: string;
  totalBudgeted: number;
  totalActual: number;
  totalVariance: number;
  overallVariancePct: number;
  lines: BudgetVarianceLineDTO[];
}
