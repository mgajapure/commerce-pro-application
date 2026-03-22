package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.ReconciliationStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ReconciliationReportDTO {
    private String id;
    private String reportRef;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private ReconciliationStatus status;
    private Long totalTransactions;
    private BigDecimal totalRevenue;
    private BigDecimal totalRefunds;
    private BigDecimal totalChargebacks;
    private BigDecimal totalFees;
    private BigDecimal netRevenue;
    private BigDecimal varianceAmount;
    private Long matchedCount;
    private Long unmatchedCount;
    private LocalDateTime generatedAt;
    private String notes;
    private LocalDateTime createdAt;
}
