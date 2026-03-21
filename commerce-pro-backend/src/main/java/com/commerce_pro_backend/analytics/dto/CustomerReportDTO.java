package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CustomerReportDTO {

    // ── LTV summary ───────────────────────────────────────────────────────────
    private long totalCustomers;
    private long activeCustomers;
    private long newCustomersInPeriod;
    private long churned;                          // no order in 90 days
    private BigDecimal averageLtv;
    private BigDecimal medianLtv;
    private BigDecimal topDecileLtv;               // avg LTV of top-10%

    // ── Tier breakdown ────────────────────────────────────────────────────────
    private Map<String, Long> customersByTier;

    // ── Acquisition ───────────────────────────────────────────────────────────
    private Map<String, Long> customersByAcquisitionSource;

    // ── LTV rows ──────────────────────────────────────────────────────────────
    private List<CustomerLtvRowDTO> ltvRows;

    @Data
    @Builder
    public static class CustomerLtvRowDTO {
        private String customerId;
        private String customerName;
        private String email;
        private String tier;
        private String status;
        private Integer totalOrders;
        private BigDecimal lifetimeSpend;
        private BigDecimal averageOrderValue;
        private String firstOrderAt;
        private String lastOrderAt;
        private Integer daysSinceLastOrder;
        private Integer loyaltyPoints;
        private String acquisitionSource;
        private String segment;                 // "High Value", "At Risk", "Churned" etc.
    }

    // ── Cohort analysis ───────────────────────────────────────────────────────
    /** Rows keyed by acquisition cohort (month), cols = month 0,1,2… retention % */
    private List<CohortRowDTO> cohortRows;

    @Data
    @Builder
    public static class CohortRowDTO {
        private String cohortMonth;         // "2024-01"
        private int    cohortSize;
        private List<BigDecimal> retentionByMonth;  // index = months after acquisition
    }

    // ── Churn analysis ────────────────────────────────────────────────────────
    private List<ChurnRowDTO> churnRows;

    @Data
    @Builder
    public static class ChurnRowDTO {
        private String customerId;
        private String customerName;
        private String email;
        private String tier;
        private BigDecimal lifetimeSpend;
        private Integer totalOrders;
        private String lastOrderAt;
        private Integer daysSinceLastOrder;
        private BigDecimal churnRisk;    // 0-100 score
    }

    // ── Trend ────────────────────────────────────────────────────────────────
    private List<CustomerTrendRowDTO> trend;

    @Data
    @Builder
    public static class CustomerTrendRowDTO {
        private String period;
        private long newCustomers;
        private long activeCustomers;
        private long churnedCustomers;
        private BigDecimal avgOrderValue;
        private BigDecimal totalRevenue;
    }
}
