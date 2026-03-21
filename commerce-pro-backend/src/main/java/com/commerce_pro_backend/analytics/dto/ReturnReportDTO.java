package com.commerce_pro_backend.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReturnReportDTO {

    // ── Summary ───────────────────────────────────────────────────────────────
    private long totalOrdersInPeriod;
    private long ordersWithReturns;
    private long totalReturnedUnits;
    private BigDecimal returnRatePct;           // orders with returns / total orders
    private BigDecimal returnedValueAmount;     // based on unit price * returnedQty
    private BigDecimal refundedAmount;

    // ── Reason breakdown ─────────────────────────────────────────────────────
    /** Cancellation / return reasons extracted from order cancellationReason field */
    private Map<String, Long> ordersByReason;

    // ── Product return rate ───────────────────────────────────────────────────
    private List<ProductReturnRateDTO> productReturnRates;

    @Data
    @Builder
    public static class ProductReturnRateDTO {
        private String productId;
        private String productName;
        private String sku;
        private String category;
        private long   unitsSold;
        private long   unitsReturned;
        private BigDecimal returnRatePct;
        private BigDecimal returnedValue;
    }

    // ── Trend ────────────────────────────────────────────────────────────────
    private List<ReturnTrendRowDTO> trend;

    @Data
    @Builder
    public static class ReturnTrendRowDTO {
        private String period;
        private long   returnsInitiated;
        private long   unitsReturned;
        private BigDecimal returnRatePct;
        private BigDecimal refundedAmount;
    }

    // ── Detail rows ───────────────────────────────────────────────────────────
    private List<ReturnDetailRowDTO> rows;
    private long totalRows;

    @Data
    @Builder
    public static class ReturnDetailRowDTO {
        private String orderNumber;
        private String customerName;
        private String productName;
        private String sku;
        private Integer returnedQuantity;
        private BigDecimal refundedAmount;
        private String reason;
        private String orderStatus;
        private String createdAt;
    }
}
