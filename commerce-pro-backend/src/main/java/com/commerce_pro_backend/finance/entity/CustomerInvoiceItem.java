package com.commerce_pro_backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "customer_invoice_items", indexes = {
    @Index(name = "idx_cii_invoice", columnList = "invoice_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerInvoiceItem {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private CustomerInvoice invoice;

    @Column(name = "order_item_id", length = 36)
    private String orderItemId;            // Reference to order_items (no FK — cross-module)

    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 6)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "tax_rate_id", length = 36)
    private String taxRateId;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @PrePersist
    @PreUpdate
    public void compute() {
        BigDecimal effective = unitPrice.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        if (effective.compareTo(BigDecimal.ZERO) < 0) effective = BigDecimal.ZERO;
        BigDecimal base = effective.multiply(BigDecimal.valueOf(quantity));
        this.taxAmount = base.multiply(taxRate != null ? taxRate : BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        this.lineTotal = base.add(taxAmount).setScale(4, RoundingMode.HALF_UP);
    }
}
