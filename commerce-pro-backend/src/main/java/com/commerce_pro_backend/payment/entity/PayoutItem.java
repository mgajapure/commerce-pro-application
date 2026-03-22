package com.commerce_pro_backend.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_items", indexes = {
    @Index(name = "idx_pi_payout",      columnList = "payout_id"),
    @Index(name = "idx_pi_transaction", columnList = "payment_transaction_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayoutItem {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id", nullable = false)
    private Payout payout;

    @Column(name = "payment_transaction_id", nullable = false, length = 36)
    private String paymentTransactionId;

    @Column(name = "transaction_ref", length = 30)
    private String transactionRef;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(name = "order_number", length = 30)
    private String orderNumber;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossAmount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
