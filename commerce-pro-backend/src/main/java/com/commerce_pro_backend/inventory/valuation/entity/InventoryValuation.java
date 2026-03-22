package com.commerce_pro_backend.inventory.valuation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * InventoryValuation Entity
 * Tracks the valuation of inventory per product/warehouse combination,
 * supporting FIFO, LIFO, Weighted Average, and Specific Identification methods.
 */
@Entity
@Table(name = "inventory_valuations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryValuation {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    // ==================== PRODUCT / WAREHOUSE ====================

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "sku")
    private String sku;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    // ==================== VALUATION ====================

    /**
     * Supported values: fifo | lifo | weighted_average | specific_identification
     */
    @Column(name = "valuation_method", nullable = false)
    private String valuationMethod;

    /**
     * Supported values: active | archived
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "active";

    @Column(name = "total_quantity")
    @Builder.Default
    private Integer totalQuantity = 0;

    @Column(name = "average_unit_cost", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal averageUnitCost = BigDecimal.ZERO;

    @Column(name = "total_value", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    // ==================== AUDIT ====================

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    @Column(name = "calculated_by")
    private String calculatedBy;

    @Column(name = "last_adjustment_at")
    private Instant lastAdjustmentAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ==================== COST LAYERS ====================

    @OneToMany(mappedBy = "valuation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("receiptDate ASC")
    private List<CostLayer> costLayers = new ArrayList<>();

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = "active";
        }
        if (totalQuantity == null) {
            totalQuantity = 0;
        }
        if (averageUnitCost == null) {
            averageUnitCost = BigDecimal.ZERO;
        }
        if (totalValue == null) {
            totalValue = BigDecimal.ZERO;
        }
    }
}
