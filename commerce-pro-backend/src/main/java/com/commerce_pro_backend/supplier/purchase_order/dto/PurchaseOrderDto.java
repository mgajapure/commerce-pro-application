package com.commerce_pro_backend.supplier.purchase_order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class PurchaseOrderDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank
        private String supplierId;

        private String supplierName;

        private String status;

        private Instant orderDate;

        private Instant expectedDeliveryDate;

        private String notes;

        private List<ItemRequest> items;

        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        private String productId;

        private String productName;

        private String sku;

        @NotNull
        @Min(1)
        private Integer quantityOrdered;

        @NotNull
        private BigDecimal unitCost;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String poNumber;
        private String supplierId;
        private String supplierName;
        private String status;
        private Instant orderDate;
        private Instant expectedDeliveryDate;
        private Instant actualDeliveryDate;
        private BigDecimal subtotal;
        private BigDecimal taxAmount;
        private BigDecimal shippingCost;
        private BigDecimal totalAmount;
        private String currency;
        private String notes;
        private String createdBy;
        private String approvedBy;
        private Instant approvedAt;
        private List<ItemResponse> items;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private String id;
        private String productId;
        private String productName;
        private String sku;
        private Integer quantityOrdered;
        private Integer quantityReceived;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private String id;
        private String poNumber;
        private String supplierName;
        private String status;
        private Instant orderDate;
        private Instant expectedDeliveryDate;
        private BigDecimal totalAmount;
        private String currency;
    }
}
