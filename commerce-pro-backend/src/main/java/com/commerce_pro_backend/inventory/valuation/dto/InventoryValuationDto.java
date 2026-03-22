package com.commerce_pro_backend.inventory.valuation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTOs for the Inventory Valuation module.
 *
 * <ul>
 *   <li>{@link Request}         – create / update payload</li>
 *   <li>{@link Response}        – full detail response</li>
 *   <li>{@link SummaryResponse} – aggregate summary / report</li>
 *   <li>{@link CostLayerResponse} – individual cost-layer detail</li>
 *   <li>{@link RecalculateRequest} – trigger recalculation</li>
 *   <li>{@link ChangeMethodRequest} – change valuation method</li>
 * </ul>
 */
public class InventoryValuationDto {

    // ==================== REQUEST ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotBlank(message = "Product name is required")
        private String productName;

        private String sku;

        private String warehouseId;

        private String warehouseName;

        /**
         * Must be one of: fifo | lifo | weighted_average | specific_identification
         */
        @NotBlank(message = "Valuation method is required")
        @Pattern(
            regexp = "fifo|lifo|weighted_average|specific_identification",
            message = "Valuation method must be one of: fifo, lifo, weighted_average, specific_identification"
        )
        private String valuationMethod;

        /**
         * Must be one of: active | archived
         */
        @Pattern(regexp = "active|archived", message = "Status must be 'active' or 'archived'")
        private String status;

        @Min(value = 0, message = "Total quantity must be >= 0")
        private Integer totalQuantity;

        @DecimalMin(value = "0.0", message = "Average unit cost must be >= 0")
        private BigDecimal averageUnitCost;

        @DecimalMin(value = "0.0", message = "Total value must be >= 0")
        private BigDecimal totalValue;

        private String calculatedBy;

        /** Optional seed cost layers supplied at creation time. */
        private List<CostLayerRequest> costLayers;
    }

    // ==================== COST LAYER REQUEST ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostLayerRequest {

        private Instant receiptDate;

        @DecimalMin(value = "0.0", inclusive = false, message = "Unit cost must be > 0")
        private BigDecimal unitCost;

        @Min(value = 1, message = "Original quantity must be >= 1")
        private Integer originalQuantity;

        private String reference;

        private Instant expiryDate;
    }

    // ==================== RESPONSE ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {

        private String id;
        private String productId;
        private String productName;
        private String sku;
        private String warehouseId;
        private String warehouseName;
        private String valuationMethod;
        private String status;

        private Integer totalQuantity;
        private BigDecimal averageUnitCost;
        private BigDecimal totalValue;

        private Instant calculatedAt;
        private String calculatedBy;
        private Instant lastAdjustmentAt;
        private Instant createdAt;

        private List<CostLayerResponse> costLayers;
    }

    // ==================== COST LAYER RESPONSE ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CostLayerResponse {

        private String id;
        private String valuationId;
        private Instant receiptDate;
        private BigDecimal unitCost;
        private Integer originalQuantity;
        private Integer remainingQuantity;
        private BigDecimal totalCost;
        private String reference;
        private Instant expiryDate;
    }

    // ==================== SUMMARY RESPONSE ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SummaryResponse {

        /** Total number of active valuation records. */
        private long totalValuations;

        /** Number of distinct products with active valuations. */
        private long totalProducts;

        /** Number of distinct warehouses with active valuations. */
        private long totalWarehouses;

        /** Sum of totalValue across all active valuations. */
        private BigDecimal totalInventoryValue;

        /** Breakdown of count and value by valuation method. */
        private Map<String, MethodBreakdown> byMethod;

        /** Breakdown of count and value by warehouse. */
        private Map<String, WarehouseBreakdown> byWarehouse;

        /** Timestamp when this summary was generated. */
        private Instant generatedAt;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MethodBreakdown {
            private String method;
            private long count;
            private BigDecimal totalValue;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class WarehouseBreakdown {
            private String warehouseId;
            private String warehouseName;
            private long count;
            private BigDecimal totalValue;
        }
    }

    // ==================== RECALCULATE REQUEST ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecalculateRequest {

        /** When null, all active valuations are recalculated. */
        private List<String> valuationIds;

        /** Optional override method applied during recalculation. */
        @Pattern(
            regexp = "fifo|lifo|weighted_average|specific_identification",
            message = "Valuation method must be one of: fifo, lifo, weighted_average, specific_identification"
        )
        private String valuationMethod;

        private String recalculatedBy;
    }

    // ==================== CHANGE METHOD REQUEST ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeMethodRequest {

        @NotBlank(message = "New valuation method is required")
        @Pattern(
            regexp = "fifo|lifo|weighted_average|specific_identification",
            message = "Valuation method must be one of: fifo, lifo, weighted_average, specific_identification"
        )
        private String newMethod;

        private String changedBy;
    }
}
