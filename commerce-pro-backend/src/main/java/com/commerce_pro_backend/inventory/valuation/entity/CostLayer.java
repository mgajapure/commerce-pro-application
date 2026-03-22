package com.commerce_pro_backend.inventory.valuation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * CostLayer Entity
 * Represents a single receipt/purchase cost layer used in FIFO and LIFO
 * inventory cost-flow calculations.  Each layer captures the unit cost and
 * quantity for a specific stock receipt so that consumption can be matched
 * against the earliest (FIFO) or most-recent (LIFO) layer first.
 */
@Entity
@Table(name = "inventory_cost_layers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostLayer {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    // ==================== RELATIONSHIP ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valuation_id", nullable = false)
    private InventoryValuation valuation;

    // ==================== LAYER DATA ====================

    /** Date/time the stock was received (drives FIFO/LIFO ordering). */
    @Column(name = "receipt_date", nullable = false)
    private Instant receiptDate;

    /** Cost per unit at time of receipt. */
    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    /** Quantity received in this layer. */
    @Column(name = "original_quantity", nullable = false)
    private Integer originalQuantity;

    /** Quantity still available in this layer (decreases as stock is consumed). */
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    /** Total cost of the remaining quantity (remainingQuantity * unitCost). */
    @Column(name = "total_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCost;

    /** Purchase order number, receipt number, or other source reference. */
    @Column(name = "reference")
    private String reference;

    /** Optional expiry date for perishable or time-limited inventory. */
    @Column(name = "expiry_date")
    private Instant expiryDate;
}
