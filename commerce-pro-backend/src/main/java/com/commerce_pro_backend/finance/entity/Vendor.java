package com.commerce_pro_backend.finance.entity;

import com.commerce_pro_backend.finance.enums.VendorStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vendor master data — the supplier entity for Accounts Payable.
 * Vendor code is auto-generated (VEN-000001) via InvoiceNumberService.
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_code",   columnList = "vendor_code",  unique = true),
    @Index(name = "idx_vendor_name",   columnList = "name"),
    @Index(name = "idx_vendor_status", columnList = "status"),
    @Index(name = "idx_vendor_email",  columnList = "email")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Vendor {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "vendor_code", nullable = false, unique = true, length = 20)
    private String vendorCode;             // VEN-000001

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "tax_id", length = 50)
    private String taxId;                  // VAT/EIN/GST number

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VendorStatus status = VendorStatus.ACTIVE;

    // ── Address ───────────────────────────────────────────────────────────────
    @Column(name = "address_line1", length = 300) private String addressLine1;
    @Column(name = "address_line2", length = 300) private String addressLine2;
    @Column(name = "city", length = 100)          private String city;
    @Column(name = "state", length = 100)         private String state;
    @Column(name = "postal_code", length = 20)    private String postalCode;
    @Column(name = "country", length = 100)       private String country;

    // ── Payment Terms ─────────────────────────────────────────────────────────
    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;           // "Net 30", "Net 60", "Due on Receipt"

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;      // 30, 60, 0

    @Column(name = "preferred_currency", length = 3)
    @Builder.Default
    private String preferredCurrency = "USD";

    @Column(name = "bank_account_name", length = 200)
    private String bankAccountName;

    @Column(name = "bank_account_last4", length = 4)
    private String bankAccountLast4;

    @Column(name = "bank_routing_number", length = 20)
    private String bankRoutingNumber;

    // ── Financial Summary (denormalised — updated by AP service) ──────────────
    @Column(name = "total_payable", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalPayable = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    // ── Metadata ──────────────────────────────────────────────────────────────
    @Column(name = "category", length = 100)
    private String category;              // SUPPLIER, SERVICE_PROVIDER, UTILITY, etc.

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "tags", length = 500)
    private String tags;                  // comma-separated

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
