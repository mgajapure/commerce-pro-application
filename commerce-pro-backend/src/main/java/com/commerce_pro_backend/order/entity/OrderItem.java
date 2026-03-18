package com.commerce_pro_backend.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single line item inside an Order.
 * Product details are snapshotted at order-placement time — product edits
 * must not retroactively alter historical orders.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_item_order",   columnList = "order_id"),
        @Index(name = "idx_order_item_product", columnList = "product_id"),
        @Index(name = "idx_order_item_sku",     columnList = "sku")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ── Product snapshot ─────────────────────────────────────────────────────
    /** FK to catalog — nullable so deleted products don't break order history. */
    @Column(name = "product_id")
    private String productId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;

    @Column(name = "product_image_url", columnDefinition = "TEXT")
    private String productImageUrl;

    @Column(name = "variant_info", length = 500)
    private String variantInfo;

    // ── Pricing snapshot (precision=19 scale=4) ───────────────────────────────
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "cost_price", precision = 19, scale = 4)
    private BigDecimal costPrice;

    @Column(name = "item_discount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal itemDiscount = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    // ── Quantity ──────────────────────────────────────────────────────────────
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "fulfilled_quantity", nullable = false)
    @Builder.Default
    private Integer fulfilledQuantity = 0;

    @Column(name = "returned_quantity", nullable = false)
    @Builder.Default
    private Integer returnedQuantity = 0;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // ── Business logic ────────────────────────────────────────────────────────

    /**
     * Recompute lineTotal = (unitPrice - itemDiscount) * quantity + taxAmount.
     * Call whenever unitPrice, quantity, itemDiscount, or taxRate changes.
     */
    public void recalculate() {
        BigDecimal effectivePrice = unitPrice.subtract(itemDiscount);
        if (effectivePrice.compareTo(BigDecimal.ZERO) < 0) {
            effectivePrice = BigDecimal.ZERO;
        }
        BigDecimal baseTotal = effectivePrice.multiply(BigDecimal.valueOf(quantity));
        this.taxAmount = baseTotal.multiply(taxRate);
        this.lineTotal = baseTotal.add(taxAmount);
    }

    public int getPendingFulfillmentQuantity() {
        return Math.max(0, quantity - fulfilledQuantity);
    }

    public int getPendingReturnQuantity() {
        return Math.max(0, fulfilledQuantity - returnedQuantity);
    }
}
