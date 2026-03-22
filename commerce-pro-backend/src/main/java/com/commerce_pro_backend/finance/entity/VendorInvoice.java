package com.commerce_pro_backend.finance.entity;

import com.commerce_pro_backend.finance.enums.VendorInvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vendor bill / AP invoice — received from a supplier.
 * Represents money owed to vendors. Reference: VINV-YYYYMMDD-NNNNNN.
 */
@Entity
@Table(name = "vendor_invoices", indexes = {
    @Index(name = "idx_vi_number",   columnList = "vendor_invoice_number", unique = true),
    @Index(name = "idx_vi_vendor",   columnList = "vendor_id"),
    @Index(name = "idx_vi_status",   columnList = "status"),
    @Index(name = "idx_vi_due",      columnList = "due_date"),
    @Index(name = "idx_vi_period",   columnList = "period_id"),
    @Index(name = "idx_vi_received", columnList = "received_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VendorInvoice {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "vendor_invoice_number", nullable = false, unique = true, length = 30)
    private String vendorInvoiceNumber;    // VINV-YYYYMMDD-NNNNNN (our reference)

    @Column(name = "vendor_reference", length = 100)
    private String vendorReference;        // Invoice number from the vendor

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private FinancialPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VendorInvoiceStatus status = VendorInvoiceStatus.RECEIVED;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "scheduled_payment_date")
    private LocalDate scheduledPaymentDate;

    // ── Financial (precision=19, scale=4) ─────────────────────────────────────
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_due", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal balanceDue = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default private String currency = "USD";

    @Column(name = "early_payment_discount_pct", precision = 5, scale = 4)
    private BigDecimal earlyPaymentDiscountPct;

    @Column(name = "early_payment_discount_days")
    private Integer earlyPaymentDiscountDays;

    // ── Metadata ──────────────────────────────────────────────────────────────
    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

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

    // ── Relationships ─────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "vendorInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VendorInvoiceItem> items = new ArrayList<>();

    // ── Business Methods ──────────────────────────────────────────────────────

    public boolean isOverdue() {
        return (status == VendorInvoiceStatus.APPROVED || status == VendorInvoiceStatus.PARTIALLY_PAID
             || status == VendorInvoiceStatus.SCHEDULED)
            && LocalDate.now().isAfter(dueDate);
    }

    public void recalculateTotals() {
        this.subtotal = items.stream()
            .map(VendorInvoiceItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.taxAmount = items.stream()
            .map(VendorInvoiceItem::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = subtotal.add(taxAmount);
        this.balanceDue = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);
    }

    /** AP aging bucket: 0=current, 1=1-30d, 2=31-60d, 3=61-90d, 4=90d+ */
    public int computeAgingBucket() {
        if (status == VendorInvoiceStatus.PAID || status == VendorInvoiceStatus.VOID) return 0;
        if (dueDate == null) return 0;
        long daysOverdue = LocalDate.now().toEpochDay() - dueDate.toEpochDay();
        if (daysOverdue <= 0)  return 0;
        if (daysOverdue <= 30) return 1;
        if (daysOverdue <= 60) return 2;
        if (daysOverdue <= 90) return 3;
        return 4;
    }
}
