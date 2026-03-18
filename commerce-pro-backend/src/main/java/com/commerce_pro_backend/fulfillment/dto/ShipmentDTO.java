package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShipmentDTO {
    private String id;
    private String shipmentNumber;
    private String orderId;
    private String orderNumber;
    private String carrierId;
    private String carrierName;
    private String trackingNumber;
    private String trackingUrl;       // resolved from carrier template
    private String status;
    private String shippingMethod;
    private String serviceLevel;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String recipientName;
    private String recipientPhone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Integer weightGrams;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;
    private BigDecimal shippingCost;
    private String labelUrl;
    private String notes;
    private String exceptionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private List<TrackingEventDTO> trackingEvents;
}
