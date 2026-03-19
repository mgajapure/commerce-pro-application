package com.commerce_pro_backend.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_saved_carts", indexes = {
        @Index(name = "idx_sc_customer", columnList = "customer_id"),
        @Index(name = "idx_sc_updated",  columnList = "updated_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SavedCart {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "cart_name", length = 100) @Builder.Default private String cartName = "Saved Cart";
    @Column(name = "coupon_code", length = 50)  private String couponCode;
    @Column(name = "estimated_total", precision = 19, scale = 4) @Builder.Default private BigDecimal estimatedTotal = BigDecimal.ZERO;
    @Column(name = "is_abandoned",  nullable = false) @Builder.Default private Boolean isAbandoned = false;
    @Column(name = "abandoned_at")                                       private LocalDateTime abandonedAt;
    @Column(name = "recovery_email_sent", nullable = false) @Builder.Default private Boolean recoveryEmailSent = false;

    @OneToMany(mappedBy = "savedCart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default private List<SavedCartItem> items = new ArrayList<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false, nullable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at", nullable = false)                    private LocalDateTime updatedAt;
}
