package com.commerce_pro_backend.supplier.contract.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

public class SupplierContractDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank
        private String supplierId;

        private String supplierName;

        @NotBlank
        private String title;

        private String description;

        private String contractType;

        private String status;

        private Instant startDate;

        private Instant endDate;

        private BigDecimal totalValue;

        private String currency;

        private String paymentTerms;

        private Boolean autoRenew;

        private Integer renewalNoticeDays;

        private String documentUrl;

        private String terms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String contractNumber;
        private String supplierId;
        private String supplierName;
        private String title;
        private String description;
        private String contractType;
        private String status;
        private Instant startDate;
        private Instant endDate;
        private BigDecimal totalValue;
        private String currency;
        private String paymentTerms;
        private Boolean autoRenew;
        private Integer renewalNoticeDays;
        private String documentUrl;
        private String terms;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private String id;
        private String contractNumber;
        private String supplierName;
        private String title;
        private String contractType;
        private String status;
        private Instant startDate;
        private Instant endDate;
        private BigDecimal totalValue;
    }
}
