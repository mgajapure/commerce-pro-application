package com.commerce_pro_backend.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private String id;
    private String orderNumber;

    // Customer
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Status
    private String status;
    private String paymentStatus;
    private String fulfillmentStatus;
    private String source;

    // Financials
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal refundedAmount;
    private String couponCode;
    private BigDecimal discountPercentage;
    private String currency;

    // Addresses
    private OrderAddressDTO shippingAddress;
    private OrderAddressDTO billingAddress;

    // Shipping
    private String shippingMethod;
    private String trackingNumber;
    private String carrier;

    // Risk / Hold
    private Integer riskScore;
    private Boolean isFlagged;
    private String holdReason;
    private String cancellationReason;
    private String internalNotes;
    private String customerNotes;

    // Timestamps
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Related data
    private List<OrderItemResponseDTO> items;
    private List<OrderStatusHistoryDTO> statusHistory;

    // Computed
    private Integer totalQuantity;
    private Boolean isEditable;
    private Boolean isCancellable;
}
