package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class InventoryReportDTO {

    // ── Stock value summary ───────────────────────────────────────────────────
    private BigDecimal totalStockValue;
    private long      totalSkus;
    private long      inStockSkus;
    private long      lowStockSkus;
    private long      outOfStockSkus;
    private long      overstockSkus;
    private BigDecimal averageUnitCost;

    // ── Stock value rows ──────────────────────────────────────────────────────
    private List<StockValueRowDTO> stockValueRows;

    @Data
    @Builder
    public static class StockValueRowDTO {
        private String productId;
        private String productName;
        private String sku;
        private String category;
        private String warehouseName;
        private Integer quantityOnHand;
        private Integer reserved;
        private Integer available;
        private BigDecimal unitCost;
        private BigDecimal totalValue;
        private String stockStatus;
        private String binLocation;
    }

    // ── Movement summary ──────────────────────────────────────────────────────
    private long totalMovements;
    private long inboundMovements;
    private long outboundMovements;
    private long adjustmentMovements;
    private long returnMovements;

    private List<StockMovementRowDTO> movementRows;

    @Data
    @Builder
    public static class StockMovementRowDTO {
        private String movementId;
        private String productName;
        private String sku;
        private String warehouseName;
        private String movementType;
        private Integer quantity;
        private Integer previousQuantity;
        private Integer newQuantity;
        private String reason;
        private String reference;
        private String createdAt;
        private String createdBy;
    }

    // ── Ageing analysis ───────────────────────────────────────────────────────
    private List<StockAgeingRowDTO> ageingRows;

    @Data
    @Builder
    public static class StockAgeingRowDTO {
        private String productId;
        private String productName;
        private String sku;
        private String warehouseName;
        private Integer quantityOnHand;
        private BigDecimal totalValue;
        /** Days since last stock movement (IN or ADJUSTMENT). */
        private Integer daysSinceLastMovement;
        /** Ageing bucket: "0-30", "31-60", "61-90", "90+" */
        private String ageingBucket;
        private String lastMovementDate;
    }
}
