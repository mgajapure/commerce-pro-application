package com.commerce_pro_backend.supplier.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

public class ProcurementRequestDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank
        private String title;

        private String description;

        private String priority;

        private String status;

        private String requestedBy;

        private String supplierId;

        private String supplierName;

        private BigDecimal estimatedCost;

        private String currency;

        private Instant requiredByDate;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String requestNumber;
        private String title;
        private String description;
        private String priority;
        private String status;
        private String requestedBy;
        private String supplierId;
        private String supplierName;
        private BigDecimal estimatedCost;
        private BigDecimal actualCost;
        private String currency;
        private Instant requiredByDate;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private String id;
        private String requestNumber;
        private String title;
        private String priority;
        private String status;
        private String requestedBy;
        private BigDecimal estimatedCost;
        private BigDecimal actualCost;
        private Instant requiredByDate;
        private Instant createdAt;
    }
}
