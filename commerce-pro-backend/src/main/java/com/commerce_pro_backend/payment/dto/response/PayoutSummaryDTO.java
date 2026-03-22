package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.PayoutStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PayoutSummaryDTO {
    private String id;
    private String payoutRef;
    private PayoutStatus status;
    private BigDecimal netAmount;
    private String currency;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime createdAt;
}
