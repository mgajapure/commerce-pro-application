package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.ReconciliationStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ReconciliationSummaryDTO {
    private String id;
    private String reportRef;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private ReconciliationStatus status;
    private Long totalTransactions;
    private BigDecimal netRevenue;
    private Long unmatchedCount;
    private LocalDateTime generatedAt;
}
