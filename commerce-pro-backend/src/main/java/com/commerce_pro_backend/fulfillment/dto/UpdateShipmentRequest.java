package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateShipmentRequest {
    private String carrierId;
    private String trackingNumber;
    private String shippingMethod;
    private String serviceLevel;
    private LocalDate estimatedDeliveryDate;
    private Integer weightGrams;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;
    private BigDecimal shippingCost;
    private String notes;
    private String exceptionReason;
}
