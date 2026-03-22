package com.commerce_pro_backend.payment.entity;

import com.commerce_pro_backend.payment.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reconciliation_reports", indexes = {
    @Index(name = "idx_recon_period",  columnList = "period_start, period_end"),
    @Index(name = "idx_recon_status",  columnList = "status"),
    @Index(name = "idx_recon_created", columnList = "created_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReconciliationReport {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "report_ref", nullable = false, unique = true, length = 30)
    private String reportRef;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.PENDING;

    // ── Aggregated Totals ─────────────────────────────────────────────────────
    @Column(name = "total_transactions", nullable = false)
    @Builder.Default
    private Long totalTransactions = 0L;

    @Column(name = "total_revenue", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_refunds", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    @Column(name = "total_chargebacks", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalChargebacks = BigDecimal.ZERO;

    @Column(name = "total_fees", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalFees = BigDecimal.ZERO;

    /** revenue - refunds - chargebacks - fees */
    @Column(name = "net_revenue", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal netRevenue = BigDecimal.ZERO;

    /** Difference between internal ledger and gateway totals */
    @Column(name = "variance_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal varianceAmount = BigDecimal.ZERO;

    @Column(name = "matched_count", nullable = false)
    @Builder.Default
    private Long matchedCount = 0L;

    @Column(name = "unmatched_count", nullable = false)
    @Builder.Default
    private Long unmatchedCount = 0L;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", length = 36)
    private String generatedBy;

    @Column(name = "notes", length = 2000)
    private String notes;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relationships ─────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReconciliationEntry> entries = new ArrayList<>();
}
