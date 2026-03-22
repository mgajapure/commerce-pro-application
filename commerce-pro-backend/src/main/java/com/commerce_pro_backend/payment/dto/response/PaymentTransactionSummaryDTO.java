package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentTransactionSummaryDTO {
    private String id;
    private String transactionRef;
    private String orderNumber;
    private String customerName;
    private TransactionType transactionType;
    private TransactionStatus status;
    private GatewayProvider gatewayProvider;
    private BigDecimal amount;
    private String currency;
    private String cardLast4;
    private String cardBrand;
    private Boolean isFlagged;
    private LocalDateTime createdAt;
}
