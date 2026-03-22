package com.commerce_pro_backend.payment.entity;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.PaymentMethodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_pm_customer", columnList = "customer_id"),
    @Index(name = "idx_pm_type",     columnList = "method_type"),
    @Index(name = "idx_pm_active",   columnList = "is_active")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentMethod {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false, length = 30)
    private PaymentMethodType methodType;

    /** Gateway's tokenised reference — NOT raw PAN */
    @Column(name = "gateway_token", length = 500)
    private String gatewayToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_provider", length = 30)
    private GatewayProvider gatewayProvider;

    // ── Card display info (last4 only — raw PAN is never stored) ─────────────
    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_exp_month", length = 2)
    private String cardExpMonth;

    @Column(name = "card_exp_year", length = 4)
    private String cardExpYear;

    @Column(name = "card_holder_name", length = 200)
    private String cardHolderName;

    /** e.g. "PayPal – john@example.com" */
    @Column(name = "account_label", length = 200)
    private String accountLabel;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;
}
