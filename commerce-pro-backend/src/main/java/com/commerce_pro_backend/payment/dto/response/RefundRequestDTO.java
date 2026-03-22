package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.RefundReason;
import com.commerce_pro_backend.payment.enums.RefundStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class RefundRequestDTO {
    private String id;
    private String refundRef;
    private String paymentTransactionId;
    private String orderId;
    private String orderNumber;
    private String customerId;
    private String customerName;
    private RefundStatus status;
    private RefundReason reason;
    private String reasonDetail;
    private BigDecimal refundAmount;
    private String currency;
    private Boolean refundShipping;
    private String requestedBy;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
    private String gatewayRefundId;
    private LocalDateTime processedAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
