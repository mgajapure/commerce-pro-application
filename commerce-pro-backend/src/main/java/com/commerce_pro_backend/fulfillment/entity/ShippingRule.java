package com.commerce_pro_backend.fulfillment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_rules", indexes = {
        @Index(name = "idx_rule_active",   columnList = "is_active"),
        @Index(name = "idx_rule_priority", columnList = "priority")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRule {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Optional FK to Carrier — denormalized for flexibility */
    @Column(name = "carrier_id", length = 36)
    private String carrierId;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    /** WEIGHT | PRICE | COUNTRY | ALWAYS */
    @Column(name = "condition_type", nullable = false, length = 50)
    @Builder.Default
    private String conditionType = "ALWAYS";

    @Column(name = "condition_min", precision = 19, scale = 4)
    private BigDecimal conditionMin;

    @Column(name = "condition_max", precision = 19, scale = 4)
    private BigDecimal conditionMax;

    /** Free-text value — e.g. comma-separated country codes */
    @Column(name = "condition_value", length = 200)
    private String conditionValue;

    @Column(name = "shipping_method", nullable = false, length = 100)
    private String shippingMethod;

    @Column(name = "service_level", length = 100)
    private String serviceLevel;

    /** Lower priority number = evaluated first */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
