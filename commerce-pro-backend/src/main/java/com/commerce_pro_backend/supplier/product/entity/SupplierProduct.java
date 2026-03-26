package com.commerce_pro_backend.supplier.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "supplier_products",
    indexes = {
        @Index(name = "idx_sp_supplier", columnList = "supplier_id"),
        @Index(name = "idx_sp_product", columnList = "product_id"),
        @Index(name = "idx_sp_sku", columnList = "supplier_sku")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SupplierProduct {

    @Id
    @Column(length = 36)
    private String id;

    @NotBlank
    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Size(max = 100)
    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @NotBlank
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Size(max = 255)
    @Column(name = "product_name", length = 255)
    private String productName;

    @Size(max = 100)
    @Column(length = 100)
    private String sku;

    @Size(max = 100)
    @Column(name = "supplier_sku", length = 100)
    private String supplierSku;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Size(max = 10)
    @Column(length = 10, nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "min_order_quantity", nullable = false)
    @Builder.Default
    private Integer minOrderQuantity = 1;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplierProduct)) return false;
        SupplierProduct that = (SupplierProduct) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
