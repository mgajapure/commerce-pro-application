package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentTransactionDTO {
    private String id;
    private String transactionRef;
    private String orderId;
    private String orderNumber;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private TransactionType transactionType;
    private TransactionStatus status;
    private GatewayProvider gatewayProvider;
    private String gatewayTransactionId;
    private String gatewayChargeId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal gatewayFee;
    private BigDecimal netAmount;
    private PaymentMethodType paymentMethodType;
    private String cardLast4;
    private String cardBrand;
    private String cardExpMonth;
    private String cardExpYear;
    private String authorizationCode;
    private LocalDateTime expiresAt;
    private LocalDateTime capturedAt;
    private LocalDateTime settledAt;
    private Integer riskScore;
    private Boolean isFlagged;
    private String flagReason;
    private String failureReason;
    private String internalNotes;
    private String parentTransactionId;
    private String paymentLinkId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
