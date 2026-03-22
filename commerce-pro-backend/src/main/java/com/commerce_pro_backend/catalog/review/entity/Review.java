package com.commerce_pro_backend.catalog.review.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
    name = "reviews",
    indexes = {
        @Index(name = "idx_review_product", columnList = "product_id"),
        @Index(name = "idx_review_status", columnList = "status"),
        @Index(name = "idx_review_rating", columnList = "rating"),
        @Index(name = "idx_review_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Size(max = 200)
    @Column(name = "product_name", length = 200)
    private String productName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @NotBlank
    @Email
    @Size(max = 200)
    @Column(name = "customer_email", nullable = false, length = 200)
    private String customerEmail;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Size(max = 200)
    @Column(length = 200)
    private String title;

    @Size(max = 5000)
    @Column(length = 5000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "is_verified_purchase", nullable = false)
    @Builder.Default
    private Boolean isVerifiedPurchase = false;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @Size(max = 5000)
    @Column(length = 5000)
    private String reply;

    @Size(max = 100)
    @Column(name = "replied_by", length = 100)
    private String repliedBy;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum ReviewStatus {
        PENDING, APPROVED, REJECTED, FLAGGED
    }
}
