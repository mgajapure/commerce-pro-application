package com.commerce_pro_backend.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {

    private long totalOrders;
    private long ordersToday;
    private long pendingOrders;
    private long processingOrders;
    private long confirmedOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long flaggedOrders;
    private long onHoldOrders;

    private BigDecimal totalRevenue;
    private BigDecimal revenueToday;
    private BigDecimal averageOrderValue;

    /** Map of status name → count */
    private Map<String, Long> statusBreakdown;
}
