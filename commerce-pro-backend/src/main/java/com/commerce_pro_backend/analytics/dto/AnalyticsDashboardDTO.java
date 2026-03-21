package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AnalyticsDashboardDTO {
    private BigDecimal revenueToday;
    private BigDecimal revenueMtd;
    private BigDecimal revenueYtd;
    private BigDecimal previousMonthRevenue;
    private BigDecimal revenueGrowthPct;

    private long ordersToday;
    private long ordersMtd;
    private BigDecimal aovMtd;

    private long newCustomersMtd;
    private long totalActiveCustomers;

    private BigDecimal totalInventoryValue;
    private long lowStockSkus;
    private long outOfStockSkus;

    private BigDecimal fulfillmentRateMtd;
    private BigDecimal onTimeDeliveryRateMtd;
    private BigDecimal cancellationRateMtd;
    private BigDecimal returnRateMtd;

    private List<SalesOverviewDTO.SalesTrendPointDTO> revenueLast30Days;
}
