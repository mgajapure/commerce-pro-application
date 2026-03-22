package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.ChargebackReason;
import com.commerce_pro_backend.payment.enums.ChargebackStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ChargebackSummaryDTO {
    private String id;
    private String disputeRef;
    private String orderNumber;
    private String customerName;
    private ChargebackStatus status;
    private ChargebackReason reason;
    private BigDecimal disputedAmount;
    private LocalDateTime responseDeadline;
    private boolean deadlineApproaching;
    private LocalDateTime createdAt;
}
