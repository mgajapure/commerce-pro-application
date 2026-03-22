package com.commerce_pro_backend.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_entries", indexes = {
    @Index(name = "idx_re_report",      columnList = "report_id"),
    @Index(name = "idx_re_transaction", columnList = "payment_transaction_id"),
    @Index(name = "idx_re_status",      columnList = "match_status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReconciliationEntry {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private ReconciliationReport report;

    @Column(name = "payment_transaction_id", nullable = false, length = 36)
    private String paymentTransactionId;

    @Column(name = "transaction_ref", length = 30)
    private String transactionRef;

    @Column(name = "internal_amount", precision = 19, scale = 4)
    private BigDecimal internalAmount;

    @Column(name = "gateway_amount", precision = 19, scale = 4)
    private BigDecimal gatewayAmount;

    @Column(name = "variance", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal variance = BigDecimal.ZERO;

    /** MATCHED / UNMATCHED / VARIANCE */
    @Column(name = "match_status", length = 30)
    private String matchStatus;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
