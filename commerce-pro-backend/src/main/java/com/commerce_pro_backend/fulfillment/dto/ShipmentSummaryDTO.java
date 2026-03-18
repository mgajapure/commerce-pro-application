package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShipmentSummaryDTO {
    private String id;
    private String shipmentNumber;
    private String orderId;
    private String orderNumber;
    private String carrierName;
    private String trackingNumber;
    private String status;
    private String shippingMethod;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String recipientName;
    private String city;
    private String country;
    private BigDecimal shippingCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
