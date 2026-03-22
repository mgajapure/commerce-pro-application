package com.commerce_pro_backend.inventory.forecast.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;

/**
 * DemandForecast Entity - Represents a generated demand forecast for a product/warehouse combination.
 */
@Entity
@Table(name = "demand_forecasts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandForecast {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "sku")
    private String sku;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    /**
     * Forecast period granularity: daily, weekly, monthly, quarterly, yearly
     */
    @Column(name = "period", nullable = false)
    private String period;

    /**
     * Algorithm used for generation: MOVING_AVERAGE, WEIGHTED_MOVING_AVERAGE, EXPONENTIAL_SMOOTHING, LINEAR_REGRESSION
     */
    @Column(name = "algorithm")
    private String algorithm;

    /**
     * Forecast lifecycle status: active, archived, obsolete
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "active";

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "total_predicted_demand")
    private Integer totalPredictedDemand;

    @Column(name = "average_daily_demand")
    private Double averageDailyDemand;

    @Column(name = "peak_demand_quantity")
    private Integer peakDemandQuantity;

    @Column(name = "safety_stock_recommendation")
    private Integer safetyStockRecommendation;

    @Column(name = "reorder_point_recommendation")
    private Integer reorderPointRecommendation;

    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = Instant.now();
        lastUpdatedAt = Instant.now();
        if (status == null) {
            status = "active";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}
