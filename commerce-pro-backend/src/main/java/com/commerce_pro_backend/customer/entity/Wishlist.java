package com.commerce_pro_backend.customer.entity;

import com.commerce_pro_backend.customer.enums.WishlistVisibility;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_wishlists", indexes = {
        @Index(name = "idx_wl_customer",    columnList = "customer_id"),
        @Index(name = "idx_wl_share_token", columnList = "share_token", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Wishlist {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100) @Builder.Default private String name = "My Wishlist";

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) @Builder.Default
    private WishlistVisibility visibility = WishlistVisibility.PRIVATE;

    @Column(name = "share_token", length = 36, unique = true) private String shareToken;
    @Column(name = "is_default", nullable = false) @Builder.Default private Boolean isDefault = false;

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("addedAt DESC") @Builder.Default private List<WishlistItem> items = new ArrayList<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false, nullable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at", nullable = false)                    private LocalDateTime updatedAt;
}
