package com.commerce_pro_backend.payment.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class PaymentStatsDTO {
    private long totalTransactions;
    private long transactionsToday;
    private BigDecimal totalRevenue;
    private BigDecimal revenueToday;
    private BigDecimal pendingCaptureAmount;
    private long pendingRefunds;
    private BigDecimal pendingRefundAmount;
    private long activeDisputes;
    private BigDecimal disputedAmount;
    private long activePaymentLinks;
    private BigDecimal nextPayoutAmount;
}
