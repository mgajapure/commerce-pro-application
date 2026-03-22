package com.commerce_pro_backend.catalog.seo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
    name = "seo_metadata",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_seo_entity", columnNames = {"entity_type", "entity_id"})
    },
    indexes = {
        @Index(name = "idx_seo_entity_type", columnList = "entity_type"),
        @Index(name = "idx_seo_entity_id", columnList = "entity_id"),
        @Index(name = "idx_seo_slug", columnList = "slug"),
        @Index(name = "idx_seo_score", columnList = "score")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeoMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @NotBlank
    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    @Size(max = 70)
    @Column(name = "meta_title", length = 70)
    private String metaTitle;

    @Size(max = 160)
    @Column(name = "meta_description", length = 160)
    private String metaDescription;

    @Size(max = 500)
    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

    @Size(max = 500)
    @Column(name = "canonical_url", length = 500)
    private String canonicalUrl;

    @Size(max = 70)
    @Column(name = "og_title", length = 70)
    private String ogTitle;

    @Size(max = 200)
    @Column(name = "og_description", length = 200)
    private String ogDescription;

    @Size(max = 500)
    @Column(name = "og_image", length = 500)
    private String ogImage;

    @Size(max = 100)
    @Column(name = "robots_directive", length = 100)
    private String robotsDirective;

    @Size(max = 250)
    @Column(length = 250)
    private String slug;

    @Size(max = 5000)
    @Column(name = "custom_head_html", length = 5000)
    private String customHeadHtml;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum EntityType {
        PRODUCT, CATEGORY, COLLECTION, PAGE
    }
}
