package com.commerce_pro_backend.finance.entity;

import com.commerce_pro_backend.finance.enums.PeriodStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Financial accounting period — the gatekeeper for all financial postings.
 * A LOCKED period cannot be reopened — permanent audit trail.
 */
@Entity
@Table(name = "financial_periods", indexes = {
    @Index(name = "idx_fp_year_period", columnList = "fiscal_year, period_number", unique = true),
    @Index(name = "idx_fp_status",      columnList = "status"),
    @Index(name = "idx_fp_dates",       columnList = "start_date, end_date")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialPeriod {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;           // 1-12 for monthly, 1-4 for quarterly

    @Column(name = "period_name", nullable = false, length = 50)
    private String periodName;             // "January 2024", "Q1 2024"

    @Column(name = "period_type", length = 20)
    @Builder.Default
    private String periodType = "MONTHLY"; // MONTHLY, QUARTERLY, YEARLY

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PeriodStatus status = PeriodStatus.OPEN;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 36)
    private String closedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", length = 36)
    private String lockedBy;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    // ── Business Methods ──────────────────────────────────────────────────────

    public boolean isPostingAllowed() {
        return status == PeriodStatus.OPEN || status == PeriodStatus.CLOSING;
    }

    public boolean isReopenable() {
        return status == PeriodStatus.CLOSED; // LOCKED cannot be reopened
    }
}
