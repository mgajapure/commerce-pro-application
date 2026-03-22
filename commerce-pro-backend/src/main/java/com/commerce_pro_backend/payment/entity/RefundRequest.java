package com.commerce_pro_backend.payment.entity;

import com.commerce_pro_backend.payment.enums.RefundReason;
import com.commerce_pro_backend.payment.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests", indexes = {
    @Index(name = "idx_refund_ref",         columnList = "refund_ref",             unique = true),
    @Index(name = "idx_refund_order",       columnList = "order_id"),
    @Index(name = "idx_refund_transaction", columnList = "payment_transaction_id"),
    @Index(name = "idx_refund_status",      columnList = "status"),
    @Index(name = "idx_refund_created",     columnList = "created_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RefundRequest {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "refund_ref", nullable = false, unique = true, length = 30)
    private String refundRef;

    @Column(name = "payment_transaction_id", nullable = false, length = 36)
    private String paymentTransactionId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RefundReason reason;

    @Column(name = "reason_detail", length = 1000)
    private String reasonDetail;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal refundAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    /** JSON array: [{orderItemId, qty, amount}] */
    @Column(name = "refund_items", columnDefinition = "TEXT")
    private String refundItems;

    @Column(name = "refund_shipping")
    @Builder.Default
    private Boolean refundShipping = false;

    // ── Approval workflow ─────────────────────────────────────────────────────
    @Column(name = "requested_by", length = 36)
    private String requestedBy;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    // ── Gateway execution ─────────────────────────────────────────────────────
    @Column(name = "gateway_refund_id", length = 200)
    private String gatewayRefundId;

    @Column(name = "gateway_status", length = 50)
    private String gatewayStatus;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

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

    public boolean isApprovable() {
        return this.status == RefundStatus.PENDING || this.status == RefundStatus.UNDER_REVIEW;
    }

    public boolean isRejectable() {
        return this.status == RefundStatus.PENDING || this.status == RefundStatus.UNDER_REVIEW;
    }

    public boolean isProcessable() {
        return this.status == RefundStatus.APPROVED;
    }
}
