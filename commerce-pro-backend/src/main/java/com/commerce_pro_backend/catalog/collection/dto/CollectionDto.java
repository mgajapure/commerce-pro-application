package com.commerce_pro_backend.catalog.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class CollectionDto {

    @Data
    @Builder
    public static class Request {
        @NotBlank
        @Size(max = 200)
        private String name;

        @NotBlank
        @Size(max = 250)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
        private String slug;

        @Size(max = 2000)
        private String description;

        private String type;
        private String status;

        @Size(max = 500)
        private String imageUrl;

        @Size(max = 5000)
        private String conditions;

        private List<String> productIds;
        private Integer sortOrder;
        private Boolean isFeatured;
        private Instant startDate;
        private Instant endDate;
    }

    @Data
    @Builder
    public static class Response {
        private String id;
        private String name;
        private String slug;
        private String description;
        private String type;
        private String status;
        private String imageUrl;
        private String conditions;
        private List<String> productIds;
        private Integer sortOrder;
        private Boolean isFeatured;
        private Instant startDate;
        private Instant endDate;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class ListResponse {
        private String id;
        private String name;
        private String slug;
        private String type;
        private String status;
        private String imageUrl;
        private Boolean isFeatured;
        private Integer sortOrder;
        private int productCount;
        private Instant createdAt;
    }

    @Data
    @Builder
    public static class ProductIdsRequest {
        private List<String> productIds;
    }

    @Data
    @Builder
    public static class StatsResponse {
        private long totalCollections;
        private long activeCollections;
        private long draftCollections;
        private long archivedCollections;
        private long manualCollections;
        private long automatedCollections;
        private long featuredCollections;
    }
}
