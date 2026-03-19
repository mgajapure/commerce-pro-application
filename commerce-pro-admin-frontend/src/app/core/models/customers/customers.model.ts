// src/app/core/models/customer/customer.model.ts
// Mirrors the Commerce Pro backend customer module DTOs exactly

// ─── Enums ───────────────────────────────────────────────────────────────────

export type CustomerStatus =
  | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION' | 'BLACKLISTED';

export type CustomerTier = 'REGULAR' | 'SILVER' | 'GOLD' | 'PLATINUM';

export type AddressType = 'SHIPPING' | 'BILLING' | 'BOTH';

export type CommunicationChannel =
  | 'EMAIL' | 'PHONE' | 'SMS' | 'CHAT' | 'IN_PERSON' | 'SOCIAL_MEDIA' | 'PORTAL' | 'OTHER';

export type CommunicationDirection = 'INBOUND' | 'OUTBOUND';

export type WishlistVisibility = 'PRIVATE' | 'PUBLIC' | 'SHARED';

// ─── Core Customer Models ────────────────────────────────────────────────────

/** Matches CustomerSummaryDTO — used in paginated list */
export interface CustomerSummary {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  status: CustomerStatus;
  tier: CustomerTier;
  groupName?: string;
  lifetimeSpend: number;
  totalOrders: number;
  loyaltyPoints: number;
  riskScore: number;
  isBlacklisted: boolean;
  isFraudFlagged: boolean;
  tags: string[];
  lastOrderAt?: string;
  createdAt: string;
}

/** Matches CustomerResponseDTO — full detail view */
export interface CustomerResponse {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  secondaryPhone?: string;
  dateOfBirth?: string;
  gender?: string;
  avatarUrl?: string;
  status: CustomerStatus;
  tier: CustomerTier;
  groupId?: string;
  groupName?: string;
  // Lifetime stats
  lifetimeSpend: number;
  totalOrders: number;
  totalReturns: number;
  averageOrderValue: number;
  lastOrderAt?: string;
  lastOrderId?: string;
  loyaltyPoints: number;
  // Business
  taxId?: string;
  companyName?: string;
  // Preferences
  preferredCurrency: string;
  preferredLanguage: string;
  marketingOptIn: boolean;
  smsOptIn: boolean;
  emailVerified: boolean;
  phoneVerified: boolean;
  // Acquisition
  acquisitionSource?: string;
  referralCode?: string;
  referredByCustomerId?: string;
  linkedUserId?: string;
  // Meta
  tags: string[];
  internalNotes?: string;
  // Blacklist & Fraud
  isBlacklisted: boolean;
  blacklistReason?: string;
  blacklistedAt?: string;
  isFraudFlagged: boolean;
  fraudReason?: string;
  fraudEvidence?: string;
  fraudFlaggedAt?: string;
  fraudResolved: boolean;
  fraudResolution?: string;
  riskScore: number;
  // Counts
  addressCount: number;
  wishlistCount: number;
  communicationLogCount: number;
  // Audit
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

/** Matches CustomerStatsDTO */
export interface CustomerStats {
  totalCustomers: number;
  activeCustomers: number;
  newCustomersToday: number;
  newCustomersThisMonth: number;
  blacklistedCustomers: number;
  fraudFlaggedCustomers: number;
  byTier: Record<string, number>;
  byStatus: Record<string, number>;
  totalLifetimeRevenue: number;
  averageLifetimeValue: number;
  loyaltyPointsOutstanding: number;
}

// ─── Address ─────────────────────────────────────────────────────────────────

export interface CustomerAddress {
  id: string;
  addressLabel?: string;
  addressType: AddressType;
  fullName: string;
  phone?: string;
  company?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
  isVerified: boolean;
  createdAt: string;
}

export interface AddressRequest {
  addressLabel?: string;
  addressType?: AddressType;
  fullName: string;
  phone?: string;
  company?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  isDefault?: boolean;
}

// ─── Groups ──────────────────────────────────────────────────────────────────

export interface CustomerGroup {
  id: string;
  name: string;
  description?: string;
  colorHex: string;
  isActive: boolean;
  discountPercentage: number;
  internalNotes?: string;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface GroupRequest {
  name: string;
  description?: string;
  colorHex?: string;
  isActive?: boolean;
  discountPercentage?: number;
  internalNotes?: string;
}

// ─── Communication Logs ───────────────────────────────────────────────────────

export interface CommunicationLog {
  id: string;
  channel: CommunicationChannel;
  direction: CommunicationDirection;
  subject: string;
  body?: string;
  orderId?: string;
  orderNumber?: string;
  loggedBy?: string;
  loggedByName?: string;
  loggedAt: string;
  requiresFollowUp: boolean;
  followUpAt?: string;
  isResolved: boolean;
  createdAt: string;
}

export interface CommunicationLogRequest {
  channel: string;
  direction: string;
  subject: string;
  body?: string;
  orderId?: string;
  orderNumber?: string;
  requiresFollowUp?: boolean;
  followUpAt?: string;
}

// ─── Wishlists ────────────────────────────────────────────────────────────────

export interface WishlistItem {
  id: string;
  productId: string;
  productName?: string;
  productSku?: string;
  productImageUrl?: string;
  priceAtAdd?: number;
  variantInfo?: string;
  note?: string;
  addedAt: string;
}

export interface Wishlist {
  id: string;
  name: string;
  visibility: WishlistVisibility;
  shareToken?: string;
  isDefault: boolean;
  itemCount: number;
  items: WishlistItem[];
  createdAt: string;
  updatedAt: string;
}

// ─── Saved Carts ──────────────────────────────────────────────────────────────

export interface SavedCartItem {
  id: string;
  productId: string;
  productName?: string;
  productSku?: string;
  productImageUrl?: string;
  unitPrice?: number;
  quantity: number;
  variantInfo?: string;
  addedAt: string;
}

export interface SavedCart {
  id: string;
  cartName: string;
  couponCode?: string;
  estimatedTotal: number;
  isAbandoned: boolean;
  abandonedAt?: string;
  recoveryEmailSent: boolean;
  items: SavedCartItem[];
  createdAt: string;
  updatedAt: string;
}

// ─── Order History ────────────────────────────────────────────────────────────

export interface CustomerOrderHistory {
  id: string;
  orderNumber: string;
  status: string;
  paymentStatus: string;
  fulfillmentStatus: string;
  totalAmount: number;
  currency: string;
  itemCount: number;
  createdAt: string;
  deliveredAt?: string;
}

// ─── Request Models ───────────────────────────────────────────────────────────

export interface CustomerRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  secondaryPhone?: string;
  dateOfBirth?: string;
  gender?: string;
  avatarUrl?: string;
  companyName?: string;
  taxId?: string;
  preferredCurrency?: string;
  preferredLanguage?: string;
  marketingOptIn?: boolean;
  smsOptIn?: boolean;
  acquisitionSource?: string;
  groupId?: string;
  internalNotes?: string;
  linkedUserId?: string;
}

export interface BlacklistRequest {
  reason: string;
}

export interface FraudFlagRequest {
  reason: string;
  evidence?: string;
  relatedOrderId?: string;
}

export interface FraudResolveRequest {
  resolution: string;
}

export interface LoyaltyAdjustRequest {
  points: number;
  description: string;
}

export interface TagsUpdateRequest {
  tags: string[];
}

// ─── Filter ───────────────────────────────────────────────────────────────────

export interface CustomerFilter {
  search?: string;
  status?: string;
  tier?: string;
  groupId?: string;
  isBlacklisted?: boolean;
  isFraudFlagged?: boolean;
  marketingOptIn?: boolean;
  tag?: string;
  acquisitionSource?: string;
  minLifetimeSpend?: number;
  maxLifetimeSpend?: number;
  minTotalOrders?: number;
  createdFrom?: string;
  createdTo?: string;
  lastOrderFrom?: string;
  lastOrderTo?: string;
}
