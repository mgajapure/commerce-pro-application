package com.commerce_pro_backend.catalog.collection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "collections",
    indexes = {
        @Index(name = "idx_collection_slug", columnList = "slug", unique = true),
        @Index(name = "idx_collection_status", columnList = "status"),
        @Index(name = "idx_collection_type", columnList = "type"),
        @Index(name = "idx_collection_featured", columnList = "is_featured"),
        @Index(name = "idx_collection_sort", columnList = "sort_order")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = 250)
    @Column(nullable = false, length = 250, unique = true)
    private String slug;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CollectionType type = CollectionType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CollectionStatus status = CollectionStatus.DRAFT;

    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "conditions", length = 5000)
    private String conditions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "collection_products", joinColumns = @JoinColumn(name = "collection_id"))
    @Column(name = "product_id")
    @Builder.Default
    private List<String> productIds = new ArrayList<>();

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum CollectionType {
        MANUAL, AUTOMATED
    }

    public enum CollectionStatus {
        ACTIVE, DRAFT, ARCHIVED
    }

    public void addProductIds(List<String> ids) {
        if (ids != null) {
            ids.forEach(id -> {
                if (!productIds.contains(id)) {
                    productIds.add(id);
                }
            });
        }
    }

    public void removeProductIds(List<String> ids) {
        if (ids != null) {
            productIds.removeAll(ids);
        }
    }
}
