package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.PayoutStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PayoutDTO {
    private String id;
    private String payoutRef;
    private PayoutStatus status;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private BigDecimal totalAmount;
    private BigDecimal totalFees;
    private BigDecimal netAmount;
    private String currency;
    private String bankAccountName;
    private String bankAccountLast4;
    private LocalDateTime initiatedAt;
    private LocalDateTime estimatedArrival;
    private LocalDateTime completedAt;
    private String failureReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
