package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SalesReportDTO {

    // ── Summary ───────────────────────────────────────────────────────────────
    private BigDecimal totalRevenue;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal totalShipping;
    private BigDecimal netRevenue;               // revenue - discount - refunds
    private BigDecimal totalRefunded;
    private long      totalOrders;
    private long      totalUnits;
    private BigDecimal averageOrderValue;

    // ── Period comparison ─────────────────────────────────────────────────────
    private BigDecimal previousRevenue;
    private BigDecimal revenueChangePct;
    private long      previousOrders;
    private BigDecimal ordersChangePct;

    // ── Breakdown rows ────────────────────────────────────────────────────────
    private List<SalesRowDTO> rows;

    @Data
    @Builder
    public static class SalesRowDTO {
        private String  dimension;       // date, product name, category, channel
        private String  dimensionId;     // id of the dimension if applicable
        private BigDecimal revenue;
        private BigDecimal discount;
        private BigDecimal tax;
        private BigDecimal shipping;
        private BigDecimal refunded;
        private long   orders;
        private long   units;
        private BigDecimal aov;
        private BigDecimal sharePct;     // % of total revenue
    }

    // ── Top performers ────────────────────────────────────────────────────────
    private List<TopProductDTO> topProducts;

    @Data
    @Builder
    public static class TopProductDTO {
        private String productId;
        private String productName;
        private String sku;
        private String category;
        private long   unitsSold;
        private BigDecimal revenue;
    }
}
