package com.commerce_pro_backend.supplier.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

public class SupplierProductDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank
        private String supplierId;

        private String supplierName;

        @NotBlank
        private String productId;

        private String productName;

        private String sku;

        private String supplierSku;

        @NotNull
        private BigDecimal unitCost;

        private String currency;

        private Integer minOrderQuantity;

        private Integer leadTimeDays;

        private Boolean isActive;

        private Boolean isPrimary;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String supplierId;
        private String supplierName;
        private String productId;
        private String productName;
        private String sku;
        private String supplierSku;
        private BigDecimal unitCost;
        private String currency;
        private Integer minOrderQuantity;
        private Integer leadTimeDays;
        private Boolean isActive;
        private Boolean isPrimary;
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
        private String supplierName;
        private String productName;
        private String sku;
        private String supplierSku;
        private BigDecimal unitCost;
        private Boolean isActive;
        private Boolean isPrimary;
    }
}
