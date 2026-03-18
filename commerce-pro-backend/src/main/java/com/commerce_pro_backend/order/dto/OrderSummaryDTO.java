package com.commerce_pro_backend.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {

    private String id;
    private String orderNumber;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String status;
    private String paymentStatus;
    private String fulfillmentStatus;
    private String source;
    private BigDecimal totalAmount;
    private String currency;
    private Integer totalQuantity;
    private Integer itemCount;
    private Boolean isFlagged;
    private Integer riskScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
