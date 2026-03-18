package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateShipmentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    private String carrierId;

    private String trackingNumber;

    private String shippingMethod;
    private String serviceLevel;

    private LocalDate estimatedDeliveryDate;

    // Recipient — if null, copied from order shipping address
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

    @Size(max = 2000)
    private String notes;
}
