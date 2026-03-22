package com.commerce_pro_backend.payment.entity;

import com.commerce_pro_backend.payment.enums.PayoutStatus;
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
@Table(name = "payouts", indexes = {
    @Index(name = "idx_payout_ref",     columnList = "payout_ref",            unique = true),
    @Index(name = "idx_payout_status",  columnList = "status"),
    @Index(name = "idx_payout_created", columnList = "created_at"),
    @Index(name = "idx_payout_period",  columnList = "period_start, period_end")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Payout {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "payout_ref", nullable = false, unique = true, length = 30)
    private String payoutRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "total_fees", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalFees = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    // ── Bank account (display only — never store routing in plain text in prod) ─
    @Column(name = "bank_account_name", length = 200)
    private String bankAccountName;

    @Column(name = "bank_account_last4", length = 4)
    private String bankAccountLast4;

    @Column(name = "bank_routing_number", length = 20)
    private String bankRoutingNumber;

    // ── Gateway ───────────────────────────────────────────────────────────────
    @Column(name = "gateway_payout_id", length = 200)
    private String gatewayPayoutId;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "estimated_arrival")
    private LocalDateTime estimatedArrival;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "notes", length = 2000)
    private String notes;

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
    @OneToMany(mappedBy = "payout", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PayoutItem> items = new ArrayList<>();
}
