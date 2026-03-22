// payment.model.ts — matches backend payment module DTOs exactly

// ── Enums ─────────────────────────────────────────────────────────────────────

export type TransactionType = 'CHARGE' | 'AUTHORIZE' | 'CAPTURE' | 'VOID' | 'REFUND' | 'CHARGEBACK' | 'ADJUSTMENT';
export type TransactionStatus = 'PENDING' | 'AUTHORIZED' | 'CAPTURED' | 'SETTLED' | 'FAILED' | 'VOIDED' | 'REFUNDED' | 'PARTIALLY_REFUNDED' | 'CHARGEBACK' | 'CANCELLED';
export type RefundStatus = 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'PROCESSING' | 'COMPLETED' | 'REJECTED' | 'FAILED';
export type RefundReason = 'CUSTOMER_REQUEST' | 'DAMAGED_ITEM' | 'WRONG_ITEM_SHIPPED' | 'ITEM_NOT_RECEIVED' | 'QUALITY_ISSUE' | 'DUPLICATE_CHARGE' | 'ORDER_CANCELLED' | 'PRICE_ADJUSTMENT' | 'FRAUD' | 'OTHER';
export type ChargebackStatus = 'RECEIVED' | 'UNDER_REVIEW' | 'EVIDENCE_REQUIRED' | 'EVIDENCE_SUBMITTED' | 'WON' | 'LOST' | 'ACCEPTED' | 'WITHDRAWN';
export type ChargebackReason = 'FRAUDULENT' | 'DUPLICATE' | 'PRODUCT_NOT_RECEIVED' | 'PRODUCT_UNACCEPTABLE' | 'CREDIT_NOT_PROCESSED' | 'GENERAL' | 'SUBSCRIPTION_CANCELLED' | 'UNRECOGNIZED';
export type ReconciliationStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'COMPLETED_WITH_VARIANCE' | 'FAILED';
export type PayoutStatus = 'PENDING' | 'APPROVED' | 'PROCESSING' | 'IN_TRANSIT' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type PaymentLinkStatus = 'ACTIVE' | 'PAID' | 'EXPIRED' | 'CANCELLED';
export type PaymentMethodType = 'CREDIT_CARD' | 'DEBIT_CARD' | 'PAYPAL' | 'APPLE_PAY' | 'GOOGLE_PAY' | 'BANK_TRANSFER' | 'CRYPTO' | 'STORE_CREDIT' | 'MANUAL';
export type GatewayProvider = 'STRIPE' | 'PAYPAL' | 'SQUARE' | 'BRAINTREE' | 'RAZORPAY' | 'MANUAL';

// ── Response Models ────────────────────────────────────────────────────────────

export interface PaymentTransactionDTO {
  id: string;
  transactionRef: string;
  orderId: string;
  orderNumber: string;
  customerId?: string;
  customerName?: string;
  customerEmail?: string;
  transactionType: TransactionType;
  status: TransactionStatus;
  gatewayProvider?: GatewayProvider;
  gatewayTransactionId?: string;
  gatewayChargeId?: string;
  amount: number;
  currency: string;
  gatewayFee: number;
  netAmount: number;
  paymentMethodType?: PaymentMethodType;
  cardLast4?: string;
  cardBrand?: string;
  cardExpMonth?: string;
  cardExpYear?: string;
  authorizationCode?: string;
  expiresAt?: string;
  capturedAt?: string;
  settledAt?: string;
  riskScore: number;
  isFlagged: boolean;
  flagReason?: string;
  failureReason?: string;
  internalNotes?: string;
  parentTransactionId?: string;
  paymentLinkId?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
}

export interface PaymentTransactionSummaryDTO {
  id: string;
  transactionRef: string;
  orderNumber: string;
  customerName?: string;
  transactionType: TransactionType;
  status: TransactionStatus;
  gatewayProvider?: GatewayProvider;
  amount: number;
  currency: string;
  cardLast4?: string;
  cardBrand?: string;
  isFlagged: boolean;
  expiresAt?: string;
  createdAt: string;
}

export interface PaymentStatsDTO {
  totalTransactions: number;
  transactionsToday: number;
  totalRevenue: number;
  revenueToday: number;
  pendingCaptureAmount: number;
  pendingRefunds: number;
  pendingRefundAmount: number;
  activeDisputes: number;
  disputedAmount: number;
  activePaymentLinks: number;
  nextPayoutAmount: number;
}

export interface RefundRequestDTO {
  id: string;
  refundRef: string;
  paymentTransactionId: string;
  orderId: string;
  orderNumber: string;
  customerId?: string;
  customerName?: string;
  status: RefundStatus;
  reason: RefundReason;
  reasonDetail?: string;
  refundAmount: number;
  currency: string;
  refundShipping: boolean;
  requestedBy?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewNotes?: string;
  gatewayRefundId?: string;
  processedAt?: string;
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RefundRequestSummaryDTO {
  id: string;
  refundRef: string;
  orderNumber: string;
  customerName?: string;
  status: RefundStatus;
  reason: RefundReason;
  refundAmount: number;
  currency: string;
  createdAt: string;
}

export interface ChargebackDisputeDTO {
  id: string;
  disputeRef: string;
  paymentTransactionId?: string;
  orderId: string;
  orderNumber?: string;
  customerId?: string;
  customerName?: string;
  status: ChargebackStatus;
  reason: ChargebackReason;
  reasonCode?: string;
  disputedAmount: number;
  currency: string;
  gatewayDisputeId?: string;
  responseDeadline?: string;
  evidenceSubmittedAt?: string;
  evidenceNotes?: string;
  resolvedAt?: string;
  resolutionNotes?: string;
  isWon?: boolean;
  deadlineApproaching: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ChargebackSummaryDTO {
  id: string;
  disputeRef: string;
  orderNumber?: string;
  customerName?: string;
  status: ChargebackStatus;
  reason: ChargebackReason;
  disputedAmount: number;
  responseDeadline?: string;
  deadlineApproaching: boolean;
  createdAt: string;
}

export interface ReconciliationReportDTO {
  id: string;
  reportRef: string;
  periodStart: string;
  periodEnd: string;
  status: ReconciliationStatus;
  totalTransactions: number;
  totalRevenue: number;
  totalRefunds: number;
  totalChargebacks: number;
  totalFees: number;
  netRevenue: number;
  varianceAmount: number;
  matchedCount: number;
  unmatchedCount: number;
  generatedAt?: string;
  notes?: string;
  createdAt: string;
}

export interface ReconciliationSummaryDTO {
  id: string;
  reportRef: string;
  periodStart: string;
  periodEnd: string;
  status: ReconciliationStatus;
  totalTransactions: number;
  netRevenue: number;
  unmatchedCount: number;
  generatedAt?: string;
}

export interface PayoutDTO {
  id: string;
  payoutRef: string;
  status: PayoutStatus;
  periodStart: string;
  periodEnd: string;
  totalAmount: number;
  totalFees: number;
  netAmount: number;
  currency: string;
  bankAccountName?: string;
  bankAccountLast4?: string;
  initiatedAt?: string;
  estimatedArrival?: string;
  completedAt?: string;
  failureReason?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PayoutSummaryDTO {
  id: string;
  payoutRef: string;
  status: PayoutStatus;
  netAmount: number;
  currency: string;
  periodStart: string;
  periodEnd: string;
  createdAt: string;
}

export interface PaymentLinkDTO {
  id: string;
  slug: string;
  payUrl: string;
  title: string;
  orderId?: string;
  orderNumber?: string;
  customerEmail?: string;
  status: PaymentLinkStatus;
  amount: number;
  currency: string;
  isOneTime: boolean;
  maxUses?: number;
  usesCount: number;
  expiresAt?: string;
  description?: string;
  paidAt?: string;
  payable: boolean;
  createdAt: string;
}

export interface PaymentLinkSummaryDTO {
  id: string;
  slug: string;
  title: string;
  status: PaymentLinkStatus;
  amount: number;
  currency: string;
  usesCount: number;
  maxUses?: number;
  expiresAt?: string;
  createdAt: string;
}

export interface PaymentMethodDTO {
  id: string;
  customerId: string;
  methodType: PaymentMethodType;
  gatewayProvider?: GatewayProvider;
  cardLast4?: string;
  cardBrand?: string;
  cardExpMonth?: string;
  cardExpYear?: string;
  cardHolderName?: string;
  accountLabel?: string;
  isDefault: boolean;
  isActive: boolean;
  lastUsedAt?: string;
  createdAt: string;
}

export interface PaymentGatewayConfigDTO {
  id: string;
  gatewayProvider: GatewayProvider;
  displayName: string;
  isTestMode: boolean;
  isActive: boolean;
  isDefault: boolean;
  supportedCurrencies?: string;
  webhookUrl?: string;
  createdAt: string;
  updatedAt: string;
}

// ── Request Models ─────────────────────────────────────────────────────────────

export interface CreatePaymentRequest {
  orderId: string;
  transactionType: TransactionType;
  amount: number;
  currency: string;
  gatewayProvider: GatewayProvider;
  paymentMethodType?: PaymentMethodType;
  internalNotes?: string;
}

export interface CreateRefundRequest {
  paymentTransactionId: string;
  refundAmount: number;
  reason: RefundReason;
  reasonDetail?: string;
  refundShipping?: boolean;
}

export interface ReviewRefundRequest {
  notes?: string;
}

export interface CreateChargebackRequest {
  orderId: string;
  paymentTransactionId?: string;
  reason: ChargebackReason;
  reasonCode?: string;
  disputedAmount: number;
  currency?: string;
  gatewayDisputeId?: string;
  responseDeadline: string;
  internalNotes?: string;
}

export interface SubmitEvidenceRequest {
  evidenceNotes: string;
  evidenceFilePaths?: string[];
}

export interface ResolveChargebackRequest {
  won: boolean;
  notes?: string;
}

export interface GenerateReconciliationRequest {
  periodStart: string;
  periodEnd: string;
  notes?: string;
}

export interface CreatePayoutRequest {
  periodStart: string;
  periodEnd: string;
  bankAccountName?: string;
  bankAccountLast4?: string;
  notes?: string;
}

export interface CreatePaymentLinkRequest {
  title: string;
  orderId?: string;
  customerEmail?: string;
  amount: number;
  currency?: string;
  isOneTime?: boolean;
  maxUses?: number;
  expiresAt?: string;
  description?: string;
}

export interface CreateGatewayConfigRequest {
  gatewayProvider: GatewayProvider;
  displayName: string;
  apiKey?: string;
  apiSecret?: string;
  webhookSecret?: string;
  isTestMode?: boolean;
  supportedCurrencies?: string;
  webhookUrl?: string;
}
