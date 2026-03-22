package com.commerce_pro_backend.finance.entity;

import com.commerce_pro_backend.finance.enums.ExpenseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Operating expense record — tracks OPEX by category with approval workflow.
 * Reference number: EXP-YYYYMMDD-NNNNNN
 */
@Entity
@Table(name = "expenses", indexes = {
    @Index(name = "idx_exp_ref",      columnList = "expense_ref",   unique = true),
    @Index(name = "idx_exp_category", columnList = "category_id"),
    @Index(name = "idx_exp_status",   columnList = "status"),
    @Index(name = "idx_exp_date",     columnList = "expense_date"),
    @Index(name = "idx_exp_period",   columnList = "period_id"),
    @Index(name = "idx_exp_vendor",   columnList = "vendor_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Expense {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "expense_ref", unique = true, nullable = false, length = 30)
    private String expenseRef;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private FinancialPeriod period;

    @Column(name = "vendor_id", length = 36)
    private String vendorId;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExpenseStatus status = ExpenseStatus.PENDING;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ── Financial (precision=19, scale=4) ─────────────────────────────────────
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default private String currency = "USD";

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;          // CREDIT_CARD, BANK_TRANSFER, CASH, etc.

    @Column(name = "receipt_file_path", length = 500)
    private String receiptFilePath;

    @Column(name = "is_recurring")
    @Builder.Default private Boolean isRecurring = false;

    @Column(name = "recurrence_interval", length = 20)
    private String recurrenceInterval;    // MONTHLY, QUARTERLY, ANNUALLY

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

    @PrePersist
    @PreUpdate
    public void computeTotal() {
        this.totalAmount = this.amount.add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO);
    }

    public boolean isApprovable() {
        return status == ExpenseStatus.PENDING || status == ExpenseStatus.UNDER_REVIEW;
    }
}
