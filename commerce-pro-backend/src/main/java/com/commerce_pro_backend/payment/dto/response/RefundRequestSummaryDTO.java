package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.RefundReason;
import com.commerce_pro_backend.payment.enums.RefundStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class RefundRequestSummaryDTO {
    private String id;
    private String refundRef;
    private String orderNumber;
    private String customerName;
    private RefundStatus status;
    private RefundReason reason;
    private BigDecimal refundAmount;
    private String currency;
    private LocalDateTime createdAt;
}
