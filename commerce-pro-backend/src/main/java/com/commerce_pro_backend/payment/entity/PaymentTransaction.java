package com.commerce_pro_backend.payment.entity;

import com.commerce_pro_backend.payment.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_ptxn_ref",      columnList = "transaction_ref",    unique = true),
    @Index(name = "idx_ptxn_order",    columnList = "order_id"),
    @Index(name = "idx_ptxn_customer", columnList = "customer_id"),
    @Index(name = "idx_ptxn_status",   columnList = "status"),
    @Index(name = "idx_ptxn_type",     columnList = "transaction_type"),
    @Index(name = "idx_ptxn_gateway",  columnList = "gateway_provider"),
    @Index(name = "idx_ptxn_created",  columnList = "created_at"),
    @Index(name = "idx_ptxn_settled",  columnList = "settled_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentTransaction {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 30)
    private String transactionRef;

    // ── Cross-module links (plain String FKs — no @ManyToOne across modules) ──
    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    // ── Type & Status ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    // ── Gateway ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_provider", length = 30)
    private GatewayProvider gatewayProvider;

    @Column(name = "gateway_transaction_id", length = 200)
    private String gatewayTransactionId;

    @Column(name = "gateway_charge_id", length = 200)
    private String gatewayChargeId;

    @Column(name = "gateway_response_code", length = 10)
    private String gatewayResponseCode;

    @Column(name = "gateway_response_message", length = 500)
    private String gatewayResponseMessage;

    // ── Financial (precision=19, scale=4) ────────────────────────────────────
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "gateway_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal netAmount = BigDecimal.ZERO;

    // ── Payment method snapshot (denormalised — captured at transaction time) ─
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", length = 30)
    private PaymentMethodType paymentMethodType;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_exp_month", length = 2)
    private String cardExpMonth;

    @Column(name = "card_exp_year", length = 4)
    private String cardExpYear;

    @Column(name = "wallet_type", length = 30)
    private String walletType;

    // ── Authorization ─────────────────────────────────────────────────────────
    @Column(name = "authorization_code", length = 50)
    private String authorizationCode;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    // ── Risk & Fraud ──────────────────────────────────────────────────────────
    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "is_flagged")
    @Builder.Default
    private Boolean isFlagged = false;

    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    // ── Metadata ──────────────────────────────────────────────────────────────
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "internal_notes", length = 2000)
    private String internalNotes;

    @Column(name = "parent_transaction_id", length = 36)
    private String parentTransactionId;

    @Column(name = "payment_link_id", length = 36)
    private String paymentLinkId;

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

    public void computeNetAmount() {
        this.netAmount = this.amount.subtract(
            this.gatewayFee != null ? this.gatewayFee : BigDecimal.ZERO);
    }

    public boolean isCaptureable() {
        return this.transactionType == TransactionType.AUTHORIZE
            && this.status == TransactionStatus.AUTHORIZED
            && (this.expiresAt == null || this.expiresAt.isAfter(LocalDateTime.now()));
    }

    public boolean isVoidable() {
        return this.status == TransactionStatus.AUTHORIZED
            || this.status == TransactionStatus.PENDING;
    }

    public boolean isRefundable() {
        return this.status == TransactionStatus.SETTLED
            || this.status == TransactionStatus.CAPTURED;
    }
}
