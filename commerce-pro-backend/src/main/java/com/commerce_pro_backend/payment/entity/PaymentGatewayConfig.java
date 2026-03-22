package com.commerce_pro_backend.payment.entity;

import com.commerce_pro_backend.payment.config.PaymentCredentialEncryptor;
import com.commerce_pro_backend.payment.enums.GatewayProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_gateway_configs", indexes = {
    @Index(name = "idx_pgc_provider", columnList = "gateway_provider", unique = true),
    @Index(name = "idx_pgc_active",   columnList = "is_active")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentGatewayConfig {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_provider", nullable = false, unique = true, length = 30)
    private GatewayProvider gatewayProvider;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** Encrypted with PaymentCredentialEncryptor (AES-256-GCM) */
    @Convert(converter = PaymentCredentialEncryptor.class)
    @Column(name = "api_key", length = 1000)
    private String apiKey;

    @Convert(converter = PaymentCredentialEncryptor.class)
    @Column(name = "api_secret", length = 1000)
    private String apiSecret;

    @Convert(converter = PaymentCredentialEncryptor.class)
    @Column(name = "webhook_secret", length = 1000)
    private String webhookSecret;

    @Column(name = "is_test_mode")
    @Builder.Default
    private Boolean isTestMode = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /** JSON array: ["USD","EUR","GBP"] */
    @Column(name = "supported_currencies", length = 500)
    private String supportedCurrencies;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /** JSON for gateway-specific extra fields */
    @Column(name = "config_metadata", columnDefinition = "TEXT")
    private String configMetadata;

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
}
