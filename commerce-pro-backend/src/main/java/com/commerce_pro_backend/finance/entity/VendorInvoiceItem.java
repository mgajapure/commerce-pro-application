package com.commerce_pro_backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "vendor_invoice_items", indexes = {
    @Index(name = "idx_vii_invoice", columnList = "vendor_invoice_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VendorInvoiceItem {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_invoice_id", nullable = false)
    private VendorInvoice vendorInvoice;

    @Column(name = "description", nullable = false, length = 500) private String description;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4) private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 6)
    @Builder.Default private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    @Builder.Default private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "expense_category_id", length = 36) private String expenseCategoryId;
    @Column(name = "sort_order") @Builder.Default private Integer sortOrder = 0;

    @PrePersist @PreUpdate
    public void compute() {
        BigDecimal base = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.taxAmount = base.multiply(taxRate != null ? taxRate : BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        this.lineTotal = base.add(taxAmount).setScale(4, RoundingMode.HALF_UP);
    }
}
