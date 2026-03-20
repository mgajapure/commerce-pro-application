package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class OrderReportDTO {

    // ── Summary ───────────────────────────────────────────────────────────────
    private long totalOrders;
    private long fulfilledOrders;
    private long cancelledOrders;
    private long returnedOrders;
    private long pendingOrders;
    private BigDecimal fulfillmentRatePct;
    private BigDecimal cancellationRatePct;
    private BigDecimal returnRatePct;
    private BigDecimal averageFulfillmentHours;   // CONFIRMED → SHIPPED

    // ── Status breakdown ─────────────────────────────────────────────────────
    private Map<String, Long> ordersByStatus;

    // ── Source breakdown ─────────────────────────────────────────────────────
    private Map<String, Long> ordersBySource;

    // ── Trend ─────────────────────────────────────────────────────────────────
    private List<OrderTrendRowDTO> trend;

    @Data
    @Builder
    public static class OrderTrendRowDTO {
        private String period;
        private long totalOrders;
        private long fulfilledOrders;
        private long cancelledOrders;
        private BigDecimal fulfillmentRatePct;
        private BigDecimal cancellationRatePct;
        private BigDecimal revenue;
    }

    // ── Detail rows (for paged tables) ───────────────────────────────────────
    private List<OrderDetailRowDTO> rows;
    private long totalRows;

    @Data
    @Builder
    public static class OrderDetailRowDTO {
        private String orderNumber;
        private String customerId;
        private String customerName;
        private String customerEmail;
        private String status;
        private String fulfillmentStatus;
        private String paymentStatus;
        private String source;
        private BigDecimal totalAmount;
        private Integer itemCount;
        private String createdAt;
        private String shippedAt;
        private String deliveredAt;
        private String cancellationReason;
        private String carrierName;
        private String trackingNumber;
    }
}
