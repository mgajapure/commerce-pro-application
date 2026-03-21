package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ShippingReportDTO {

    // ── On-time delivery summary ──────────────────────────────────────────────
    private long totalShipments;
    private long deliveredShipments;
    private long onTimeDeliveries;
    private long lateDeliveries;
    private long inTransit;
    private long lost;
    private long returnedToSender;
    private BigDecimal onTimeRatePct;
    private BigDecimal averageDeliveryDays;

    // ── Per-carrier performance ───────────────────────────────────────────────
    private List<CarrierPerformanceDTO> carrierPerformance;

    @Data
    @Builder
    public static class CarrierPerformanceDTO {
        private String carrierId;
        private String carrierName;
        private long   totalShipments;
        private long   delivered;
        private long   onTime;
        private long   late;
        private long   lost;
        private long   returnedToSender;
        private BigDecimal onTimeRatePct;
        private BigDecimal avgDeliveryDays;
        private BigDecimal totalShippingCost;
        private BigDecimal avgShippingCost;
        private BigDecimal avgWeightGrams;
    }

    // ── Shipment detail rows ──────────────────────────────────────────────────
    private List<ShipmentDetailRowDTO> rows;
    private long totalRows;

    @Data
    @Builder
    public static class ShipmentDetailRowDTO {
        private String shipmentNumber;
        private String orderNumber;
        private String carrierName;
        private String trackingNumber;
        private String status;
        private String shippedAt;
        private String estimatedDeliveryDate;
        private String actualDeliveryDate;
        private Integer deliveryDays;
        private Boolean onTime;
        private BigDecimal shippingCost;
        private String recipientCity;
        private String recipientCountry;
    }

    // ── Trend ────────────────────────────────────────────────────────────────
    private List<ShippingTrendRowDTO> trend;

    @Data
    @Builder
    public static class ShippingTrendRowDTO {
        private String period;
        private long shipments;
        private BigDecimal onTimeRatePct;
        private BigDecimal avgDeliveryDays;
        private BigDecimal totalCost;
    }
}
