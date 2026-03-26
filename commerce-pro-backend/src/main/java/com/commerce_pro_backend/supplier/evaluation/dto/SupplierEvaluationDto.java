package com.commerce_pro_backend.supplier.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class SupplierEvaluationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank
        private String supplierId;

        private String supplierName;

        private Instant evaluationDate;

        private String evaluatedBy;

        @Min(0)
        @Max(5)
        private Double overallScore;

        @Min(0)
        @Max(5)
        private Double qualityScore;

        @Min(0)
        @Max(5)
        private Double deliveryScore;

        @Min(0)
        @Max(5)
        private Double pricingScore;

        @Min(0)
        @Max(5)
        private Double communicationScore;

        @Min(0)
        @Max(5)
        private Double complianceScore;

        private String period;

        private String comments;

        private String recommendations;

        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String supplierId;
        private String supplierName;
        private Instant evaluationDate;
        private String evaluatedBy;
        private Double overallScore;
        private Double qualityScore;
        private Double deliveryScore;
        private Double pricingScore;
        private Double communicationScore;
        private Double complianceScore;
        private String period;
        private String comments;
        private String recommendations;
        private String status;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private String id;
        private String supplierName;
        private Instant evaluationDate;
        private Double overallScore;
        private Double qualityScore;
        private Double deliveryScore;
        private Double pricingScore;
        private Double communicationScore;
        private Double complianceScore;
        private String period;
        private String status;
    }
}
