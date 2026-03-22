package com.commerce_pro_backend.payment.entity;

import com.commerce_pro_backend.payment.enums.ChargebackReason;
import com.commerce_pro_backend.payment.enums.ChargebackStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chargeback_disputes", indexes = {
    @Index(name = "idx_cb_ref",         columnList = "dispute_ref",          unique = true),
    @Index(name = "idx_cb_order",       columnList = "order_id"),
    @Index(name = "idx_cb_transaction", columnList = "payment_transaction_id"),
    @Index(name = "idx_cb_status",      columnList = "status"),
    @Index(name = "idx_cb_deadline",    columnList = "response_deadline"),
    @Index(name = "idx_cb_created",     columnList = "created_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargebackDispute {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "dispute_ref", nullable = false, unique = true, length = 30)
    private String disputeRef;

    @Column(name = "payment_transaction_id", length = 36)
    private String paymentTransactionId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "order_number", length = 30)
    private String orderNumber;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ChargebackStatus status = ChargebackStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChargebackReason reason;

    @Column(name = "reason_code", length = 20)
    private String reasonCode;

    @Column(name = "disputed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal disputedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "gateway_dispute_id", length = 200)
    private String gatewayDisputeId;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    // ── Evidence ──────────────────────────────────────────────────────────────
    @Column(name = "evidence_submitted_at")
    private LocalDateTime evidenceSubmittedAt;

    @Column(name = "evidence_notes", columnDefinition = "TEXT")
    private String evidenceNotes;

    /** JSON array of file paths in the chargebacks/ subdirectory */
    @Column(name = "evidence_file_paths", columnDefinition = "TEXT")
    private String evidenceFilePaths;

    // ── Resolution ────────────────────────────────────────────────────────────
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    /** true = merchant won; false = customer won; null = not yet resolved */
    @Column(name = "is_won")
    private Boolean isWon;

    @Column(name = "internal_notes", length = 2000)
    private String internalNotes;

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

    // ── Business Methods ──────────────────────────────────────────────────────

    public boolean isDeadlineApproaching() {
        return responseDeadline != null
            && LocalDateTime.now().isAfter(responseDeadline.minusDays(3))
            && status == ChargebackStatus.EVIDENCE_REQUIRED;
    }
}
