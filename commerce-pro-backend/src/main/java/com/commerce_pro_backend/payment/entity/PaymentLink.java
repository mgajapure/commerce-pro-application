package com.commerce_pro_backend.payment.entity;

import com.commerce_pro_backend.payment.enums.PaymentLinkStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_links", indexes = {
    @Index(name = "idx_pl_slug",    columnList = "slug",    unique = true),
    @Index(name = "idx_pl_status",  columnList = "status"),
    @Index(name = "idx_pl_order",   columnList = "order_id"),
    @Index(name = "idx_pl_expires", columnList = "expires_at"),
    @Index(name = "idx_pl_created", columnList = "created_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentLink {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(name = "order_number", length = 30)
    private String orderNumber;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentLinkStatus status = PaymentLinkStatus.ACTIVE;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "is_one_time")
    @Builder.Default
    private Boolean isOneTime = true;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "uses_count")
    @Builder.Default
    private Integer usesCount = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "description", length = 2000)
    private String description;

    /** JSON key/value for custom fields */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_transaction_id", length = 36)
    private String paidTransactionId;

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

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsageLimitReached() {
        return maxUses != null && usesCount >= maxUses;
    }

    public boolean isPayable() {
        return status == PaymentLinkStatus.ACTIVE && !isExpired() && !isUsageLimitReached();
    }
}
