package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One order in the fulfillment queue — orders ready to be picked. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FulfillmentQueueItemDTO {
    private String orderId;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private String status;            // OrderStatus
    private String fulfillmentStatus; // FulfillmentStatus
    private BigDecimal totalAmount;
    private String currency;
    private int totalItems;
    private int pendingItems;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private boolean isFlagged;
    private int riskScore;
    private String shippingMethod;
}
