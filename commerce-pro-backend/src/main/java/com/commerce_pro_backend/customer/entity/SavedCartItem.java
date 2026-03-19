package com.commerce_pro_backend.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_cart_items", indexes = {
        @Index(name = "idx_sci_cart",    columnList = "saved_cart_id"),
        @Index(name = "idx_sci_product", columnList = "product_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavedCartItem {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "saved_cart_id", nullable = false)
    private SavedCart savedCart;

    @Column(name = "product_id",        nullable = false, length = 36)  private String productId;
    @Column(name = "product_name",      length = 300)                   private String productName;
    @Column(name = "product_sku",       length = 100)                   private String productSku;
    @Column(name = "product_image_url", columnDefinition = "TEXT")      private String productImageUrl;
    @Column(name = "unit_price",        precision = 19, scale = 4)      private BigDecimal unitPrice;
    @Column(name = "quantity",          nullable = false) @Builder.Default private Integer quantity = 1;
    @Column(name = "variant_info",      length = 500)                   private String variantInfo;
    @Column(name = "added_at",          nullable = false)                private LocalDateTime addedAt;

    @PrePersist protected void onPersist() { if (addedAt == null) addedAt = LocalDateTime.now(); }
}
