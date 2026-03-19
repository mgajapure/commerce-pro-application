package com.commerce_pro_backend.customer.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lightweight order projection for customer order-history view. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderHistoryDTO {
    private String id;
    private String orderNumber;
    private String status;
    private String paymentStatus;
    private String fulfillmentStatus;
    private BigDecimal totalAmount;
    private String currency;
    private Integer itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
}
