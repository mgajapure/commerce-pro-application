package com.commerce_pro_backend.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"wishlist_id", "product_id"}),
       indexes = {
               @Index(name = "idx_wli_wishlist", columnList = "wishlist_id"),
               @Index(name = "idx_wli_product",  columnList = "product_id")
       })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WishlistItem {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "wishlist_id", nullable = false)
    private Wishlist wishlist;

    @Column(name = "product_id",       nullable = false, length = 36)  private String productId;
    @Column(name = "product_name",     length = 300)                   private String productName;
    @Column(name = "product_sku",      length = 100)                   private String productSku;
    @Column(name = "product_image_url",columnDefinition = "TEXT")      private String productImageUrl;
    @Column(name = "price_at_add",     precision = 19, scale = 4)      private BigDecimal priceAtAdd;
    @Column(name = "variant_info",     length = 500)                   private String variantInfo;
    @Column(name = "note",             length = 500)                   private String note;
    @Column(name = "added_at",         nullable = false)                private LocalDateTime addedAt;

    @PrePersist protected void onPersist() { if (addedAt == null) addedAt = LocalDateTime.now(); }
}
