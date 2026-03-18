package com.commerce_pro_backend.fulfillment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "pick_list_items", indexes = {
        @Index(name = "idx_pli_picklist", columnList = "pick_list_id"),
        @Index(name = "idx_pli_order",    columnList = "order_id"),
        @Index(name = "idx_pli_sku",      columnList = "sku")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickListItem {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pick_list_id", nullable = false)
    private PickList pickList;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "order_item_id", nullable = false, length = 36)
    private String orderItemId;

    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;

    @Column(name = "variant_info", length = 500)
    private String variantInfo;

    @Column(name = "bin_location", length = 100)
    private String binLocation;

    @Column(name = "quantity_required", nullable = false)
    private Integer quantityRequired;

    @Column(name = "quantity_picked", nullable = false)
    @Builder.Default
    private Integer quantityPicked = 0;

    @Column(name = "is_picked", nullable = false)
    @Builder.Default
    private Boolean isPicked = false;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // ── Business logic ────────────────────────────────────────────────────────

    public void markPicked(int qty) {
        this.quantityPicked = Math.min(qty, this.quantityRequired);
        this.isPicked = this.quantityPicked >= this.quantityRequired;
    }
}
