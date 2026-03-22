package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.ChargebackReason;
import com.commerce_pro_backend.payment.enums.ChargebackStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ChargebackDisputeDTO {
    private String id;
    private String disputeRef;
    private String paymentTransactionId;
    private String orderId;
    private String orderNumber;
    private String customerId;
    private String customerName;
    private ChargebackStatus status;
    private ChargebackReason reason;
    private String reasonCode;
    private BigDecimal disputedAmount;
    private String currency;
    private String gatewayDisputeId;
    private LocalDateTime responseDeadline;
    private LocalDateTime evidenceSubmittedAt;
    private String evidenceNotes;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private Boolean isWon;
    private boolean deadlineApproaching;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
