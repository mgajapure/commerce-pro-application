package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class FinancialReportDTO {

    // ── P&L summary ───────────────────────────────────────────────────────────
    private BigDecimal grossRevenue;
    private BigDecimal totalDiscount;
    private BigDecimal netRevenue;
    private BigDecimal totalCogs;               // sum of (costPrice * qty) from order_items
    private BigDecimal grossProfit;
    private BigDecimal grossMarginPct;
    private BigDecimal totalTax;
    private BigDecimal totalShipping;
    private BigDecimal totalRefunds;
    private BigDecimal netProfit;               // grossProfit - shipping costs etc.

    // ── Previous period ───────────────────────────────────────────────────────
    private BigDecimal previousGrossRevenue;
    private BigDecimal revenueChangePct;
    private BigDecimal previousGrossMarginPct;
    private BigDecimal marginChangePct;

    // ── Tax summary breakdown ─────────────────────────────────────────────────
    private BigDecimal totalTaxCollected;
    private Map<String, BigDecimal> taxByRate;      // "0.05" -> amount

    // ── Margin rows (per product/category) ───────────────────────────────────
    private List<MarginRowDTO> marginRows;

    @Data
    @Builder
    public static class MarginRowDTO {
        private String dimension;
        private String dimensionId;
        private BigDecimal revenue;
        private BigDecimal cogs;
        private BigDecimal grossProfit;
        private BigDecimal marginPct;
        private long      unitsSold;
        private BigDecimal avgSellingPrice;
        private BigDecimal avgCostPrice;
    }

    // ── Revenue trend ────────────────────────────────────────────────────────
    private List<RevenueTrendRowDTO> revenueTrend;

    @Data
    @Builder
    public static class RevenueTrendRowDTO {
        private String period;
        private BigDecimal grossRevenue;
        private BigDecimal netRevenue;
        private BigDecimal cogs;
        private BigDecimal grossProfit;
        private BigDecimal grossMarginPct;
        private BigDecimal tax;
        private BigDecimal refunds;
    }

    // ── Tax report rows ───────────────────────────────────────────────────────
    private List<TaxSummaryRowDTO> taxSummaryRows;

    @Data
    @Builder
    public static class TaxSummaryRowDTO {
        private String period;
        private long   orders;
        private BigDecimal taxableRevenue;
        private BigDecimal taxCollected;
        private BigDecimal effectiveTaxRate;
    }
}
