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
public class OrderItemResponseDTO {

    private String id;
    private String productId;
    private String sku;
    private String productName;
    private String productImageUrl;
    private String variantInfo;

    private BigDecimal unitPrice;
    private BigDecimal costPrice;
    private BigDecimal itemDiscount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;

    private Integer quantity;
    private Integer fulfilledQuantity;
    private Integer returnedQuantity;
    private Integer pendingFulfillmentQuantity;

    private LocalDateTime createdAt;
}
