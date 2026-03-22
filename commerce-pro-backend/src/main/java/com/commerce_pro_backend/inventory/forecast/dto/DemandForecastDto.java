package com.commerce_pro_backend.inventory.forecast.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTOs for DemandForecast: Request, Response, and GenerateRequest inner classes.
 */
public class DemandForecastDto {

    /**
     * Request DTO for creating or updating a demand forecast record manually.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "Product ID is required")
        private String productId;

        private String productName;

        private String sku;

        private String warehouseId;

        private String warehouseName;

        @NotBlank(message = "Period is required")
        @Pattern(
            regexp = "daily|weekly|monthly|quarterly|yearly",
            message = "Period must be one of: daily, weekly, monthly, quarterly, yearly"
        )
        private String period;

        private String algorithm;

        @Pattern(
            regexp = "active|archived|obsolete",
            message = "Status must be one of: active, archived, obsolete"
        )
        private String status;

        private Instant startDate;
        private Instant endDate;

        private Integer totalPredictedDemand;
        private Double averageDailyDemand;
        private Integer peakDemandQuantity;
        private Integer safetyStockRecommendation;
        private Integer reorderPointRecommendation;
        private String generatedBy;
    }

    /**
     * Response DTO — full projection of a DemandForecast record.
     */
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
        private String period;
        private String algorithm;
        private String status;
        private Instant startDate;
        private Instant endDate;
        private Integer totalPredictedDemand;
        private Double averageDailyDemand;
        private Integer peakDemandQuantity;
        private Integer safetyStockRecommendation;
        private Integer reorderPointRecommendation;
        private String generatedBy;
        private Instant generatedAt;
        private Instant lastUpdatedAt;
    }

    /**
     * Request DTO for triggering automatic forecast generation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateRequest {

        @NotBlank(message = "Product ID is required")
        private String productId;

        private String productName;

        private String sku;

        private String warehouseId;

        private String warehouseName;

        @NotBlank(message = "Period is required")
        @Pattern(
            regexp = "daily|weekly|monthly|quarterly|yearly",
            message = "Period must be one of: daily, weekly, monthly, quarterly, yearly"
        )
        private String period;

        /**
         * Optional algorithm override. Defaults to MOVING_AVERAGE if not supplied.
         */
        @Pattern(
            regexp = "MOVING_AVERAGE|WEIGHTED_MOVING_AVERAGE|EXPONENTIAL_SMOOTHING|LINEAR_REGRESSION",
            message = "Algorithm must be one of: MOVING_AVERAGE, WEIGHTED_MOVING_AVERAGE, EXPONENTIAL_SMOOTHING, LINEAR_REGRESSION"
        )
        private String algorithm;

        @NotNull(message = "Start date is required")
        private Instant startDate;

        @NotNull(message = "End date is required")
        private Instant endDate;

        /**
         * Historical baseline demand used by the algorithm (units per day).
         */
        private Double baselineDailyDemand;

        /**
         * Optional demand variability factor (0.0 – 1.0) used to compute safety stock.
         */
        private Double demandVariabilityFactor;

        private String generatedBy;
    }
}
