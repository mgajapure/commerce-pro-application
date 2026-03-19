package com.commerce_pro_backend.customer.entity;

import com.commerce_pro_backend.customer.enums.CustomerStatus;
import com.commerce_pro_backend.customer.enums.CustomerTier;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Customer — aggregate root of the customer module.
 *
 * Cross-module note: Orders are linked via Order.customerId (plain String on the
 * Order side). We never hold a @ManyToOne to Order here. Cross-module reads go
 * through CustomerOrderQueryService which only injects OrderRepository.
 * Product IDs on WishlistItem / SavedCartItem are plain String FKs resolved
 * through CustomerProductQueryService.
 */
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_cust_email",     columnList = "email",         unique = true),
        @Index(name = "idx_cust_phone",     columnList = "phone"),
        @Index(name = "idx_cust_status",    columnList = "status"),
        @Index(name = "idx_cust_tier",      columnList = "tier"),
        @Index(name = "idx_cust_group",     columnList = "group_id"),
        @Index(name = "idx_cust_blacklist", columnList = "is_blacklisted"),
        @Index(name = "idx_cust_created",   columnList = "created_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Customer {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    // ── Identity ──────────────────────────────────────────────────────────
    @NotBlank @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank @Email @Size(max = 255)
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @Size(max = 30)
    @Column(name = "secondary_phone", length = 30)
    private String secondaryPhone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 20)
    @Column(length = 20)
    private String gender;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    // ── Status & Tier ─────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CustomerTier tier = CustomerTier.REGULAR;

    // ── Group ─────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private CustomerGroup group;

    // ── Lifetime stats (maintained by CustomerStatsService) ───────────────
    @Column(name = "lifetime_spend", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lifetimeSpend = BigDecimal.ZERO;

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_returns", nullable = false)
    @Builder.Default
    private Integer totalReturns = 0;

    @Column(name = "average_order_value", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal averageOrderValue = BigDecimal.ZERO;

    @Column(name = "last_order_at")
    private LocalDateTime lastOrderAt;

    @Column(name = "last_order_id", length = 36)
    private String lastOrderId;

    // ── Loyalty points (running balance) ─────────────────────────────────
    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    // ── Business ─────────────────────────────────────────────────────────
    @Size(max = 50)  @Column(name = "tax_id", length = 50)        private String taxId;
    @Size(max = 100) @Column(name = "company_name", length = 100) private String companyName;

    // ── Preferences ───────────────────────────────────────────────────────
    @Column(name = "preferred_currency", length = 3) @Builder.Default private String preferredCurrency = "USD";
    @Column(name = "preferred_language", length = 10) @Builder.Default private String preferredLanguage = "en";
    @Column(name = "marketing_opt_in", nullable = false) @Builder.Default private Boolean marketingOptIn = false;
    @Column(name = "sms_opt_in", nullable = false)       @Builder.Default private Boolean smsOptIn = false;
    @Column(name = "email_verified", nullable = false)   @Builder.Default private Boolean emailVerified = false;
    @Column(name = "phone_verified", nullable = false)   @Builder.Default private Boolean phoneVerified = false;

    // ── Acquisition / referral ────────────────────────────────────────────
    @Size(max = 100) @Column(name = "acquisition_source", length = 100) private String acquisitionSource;
    @Size(max = 50)  @Column(name = "referral_code", length = 50, unique = true) private String referralCode;
    @Column(name = "referred_by_customer_id", length = 36) private String referredByCustomerId;

    // ── Linked identity account (soft-link, no FK to user_identity) ───────
    @Column(name = "linked_user_id", length = 36) private String linkedUserId;

    // ── Tags (ad-hoc labels beyond groups) ────────────────────────────────
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_tags", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "tag", length = 50)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    // ── Internal notes ────────────────────────────────────────────────────
    @Column(name = "internal_notes", length = 2000) private String internalNotes;

    // ── Blacklist & fraud (managed directly on this entity) ───────────────
    @Column(name = "is_blacklisted", nullable = false) @Builder.Default private Boolean isBlacklisted = false;
    @Column(name = "blacklist_reason",  length = 500) private String blacklistReason;
    @Column(name = "blacklisted_at")                  private LocalDateTime blacklistedAt;
    @Column(name = "blacklisted_by",    length = 36)  private String blacklistedBy;

    @Column(name = "is_fraud_flagged",  nullable = false) @Builder.Default private Boolean isFraudFlagged = false;
    @Column(name = "fraud_reason",      length = 500) private String fraudReason;
    @Column(name = "fraud_evidence",    columnDefinition = "TEXT") private String fraudEvidence;
    @Column(name = "fraud_flagged_at")                private LocalDateTime fraudFlaggedAt;
    @Column(name = "fraud_flagged_by",  length = 36)  private String fraudFlaggedBy;
    @Column(name = "fraud_resolved",    nullable = false) @Builder.Default private Boolean fraudResolved = false;
    @Column(name = "fraud_resolved_at")               private LocalDateTime fraudResolvedAt;
    @Column(name = "fraud_resolved_by", length = 36)  private String fraudResolvedBy;
    @Column(name = "fraud_resolution",  length = 1000) private String fraudResolution;

    @Column(name = "risk_score", nullable = false) @Builder.Default private Integer riskScore = 0;

    // ── Relationships ──────────────────────────────────────────────────────
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("isDefault DESC, createdAt ASC")
    @Builder.Default private List<CustomerAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("loggedAt DESC")
    @Builder.Default private List<CustomerCommunicationLog> communicationLogs = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default private List<Wishlist> wishlists = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("updatedAt DESC")
    @Builder.Default private List<SavedCart> savedCarts = new ArrayList<>();

    // ── Audit ──────────────────────────────────────────────────────────────
    @CreationTimestamp @Column(name = "created_at", updatable = false, nullable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at", nullable = false)                    private LocalDateTime updatedAt;
    @Column(name = "created_by", updatable = false, length = 36) private String createdBy;
    @Column(name = "updated_by", length = 36)                    private String updatedBy;

    // ── Business methods ───────────────────────────────────────────────────
    public String getFullName() { return firstName + " " + lastName; }

    public void recalculateAverageOrderValue() {
        this.averageOrderValue = (totalOrders == null || totalOrders == 0 || lifetimeSpend == null)
                ? BigDecimal.ZERO
                : lifetimeSpend.divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP);
    }

    public CustomerTier evaluateTier() {
        if (lifetimeSpend == null) return CustomerTier.REGULAR;
        if (lifetimeSpend.compareTo(new BigDecimal("10000")) >= 0 || totalOrders >= 50) return CustomerTier.PLATINUM;
        if (lifetimeSpend.compareTo(new BigDecimal("2000"))  >= 0 || totalOrders >= 20) return CustomerTier.GOLD;
        if (lifetimeSpend.compareTo(new BigDecimal("500"))   >= 0 || totalOrders >= 5)  return CustomerTier.SILVER;
        return CustomerTier.REGULAR;
    }

    public boolean canPlaceOrders() {
        return status == CustomerStatus.ACTIVE && !Boolean.TRUE.equals(isBlacklisted);
    }
}
