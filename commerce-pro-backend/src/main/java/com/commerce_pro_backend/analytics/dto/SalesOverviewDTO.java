package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Top-level summary returned by the sales-overview endpoint. */
@Data
@Builder
public class SalesOverviewDTO {

    private BigDecimal totalRevenue;
    private BigDecimal previousPeriodRevenue;
    private BigDecimal revenueGrowthPct;

    private long totalOrders;
    private long previousPeriodOrders;
    private BigDecimal orderGrowthPct;

    private BigDecimal averageOrderValue;
    private BigDecimal previousPeriodAov;
    private BigDecimal aovGrowthPct;

    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal totalShipping;

    private long totalUnits;

    /** Time-series breakdown matching the requested groupBy. */
    private List<SalesTrendPointDTO> trend;

    @Data
    @Builder
    public static class SalesTrendPointDTO {
        private String period;          // "2024-01", "2024-W03", "2024-01-15" etc.
        private BigDecimal revenue;
        private long orders;
        private BigDecimal aov;
        private long units;
    }
}
