package com.commerce_pro_backend.catalog.review.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

public class ReviewDto {

    @Data
    @Builder
    public static class Request {
        @NotBlank
        private String productId;

        @Size(max = 200)
        private String productName;

        @NotBlank
        @Size(max = 100)
        private String customerName;

        @NotBlank
        @Email
        @Size(max = 200)
        private String customerEmail;

        @NotNull
        @Min(1)
        @Max(5)
        private Integer rating;

        @Size(max = 200)
        private String title;

        @Size(max = 5000)
        private String content;

        private Boolean isVerifiedPurchase;
    }

    @Data
    @Builder
    public static class Response {
        private String id;
        private String productId;
        private String productName;
        private String customerName;
        private String customerEmail;
        private Integer rating;
        private String title;
        private String content;
        private String status;
        private Boolean isVerifiedPurchase;
        private Integer helpfulCount;
        private Integer reportCount;
        private String reply;
        private String repliedBy;
        private Instant repliedAt;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class ListResponse {
        private String id;
        private String productId;
        private String productName;
        private String customerName;
        private Integer rating;
        private String title;
        private String status;
        private Boolean isVerifiedPurchase;
        private Integer helpfulCount;
        private Integer reportCount;
        private Instant createdAt;
    }

    @Data
    @Builder
    public static class ReplyRequest {
        @NotBlank
        @Size(max = 5000)
        private String reply;

        @NotBlank
        @Size(max = 100)
        private String repliedBy;
    }

    @Data
    @Builder
    public static class StatsResponse {
        private long totalReviews;
        private long pendingReviews;
        private long approvedReviews;
        private long rejectedReviews;
        private long flaggedReviews;
        private double averageRating;
        private Map<Integer, Long> ratingDistribution;
    }
}
