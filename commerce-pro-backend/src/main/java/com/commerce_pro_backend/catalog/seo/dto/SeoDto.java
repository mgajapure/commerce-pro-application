package com.commerce_pro_backend.catalog.seo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SeoDto {

    @Data
    @Builder
    public static class Request {
        @Size(max = 70)
        private String metaTitle;

        @Size(max = 160)
        private String metaDescription;

        @Size(max = 500)
        private String metaKeywords;

        @Size(max = 500)
        private String canonicalUrl;

        @Size(max = 70)
        private String ogTitle;

        @Size(max = 200)
        private String ogDescription;

        @Size(max = 500)
        private String ogImage;

        @Size(max = 100)
        private String robotsDirective;

        @Size(max = 250)
        private String slug;

        @Size(max = 5000)
        private String customHeadHtml;
    }

    @Data
    @Builder
    public static class Response {
        private String id;
        private String entityType;
        private String entityId;
        private String metaTitle;
        private String metaDescription;
        private String metaKeywords;
        private String canonicalUrl;
        private String ogTitle;
        private String ogDescription;
        private String ogImage;
        private String robotsDirective;
        private String slug;
        private String customHeadHtml;
        private Integer score;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class ListResponse {
        private String id;
        private String entityType;
        private String entityId;
        private String metaTitle;
        private String metaDescription;
        private Integer score;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class SeoAuditResult {
        private Integer score;
        private List<String> issues;
        private List<String> warnings;
        private List<String> passed;
        private Map<String, String> suggestions;
    }

    @Data
    @Builder
    public static class StatsResponse {
        private long totalEntries;
        private double averageScore;
        private long entriesWithScore80Plus;
        private long entriesWithScoreBelow50;
        private Map<String, Long> entriesByEntityType;
    }
}
