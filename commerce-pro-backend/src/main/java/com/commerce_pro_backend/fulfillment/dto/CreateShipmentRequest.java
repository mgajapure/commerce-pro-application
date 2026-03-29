package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.DecimalMin;
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

    @Size(max = 100, message = "Tracking number must be at most 100 characters")
    private String trackingNumber;

    @Size(max = 100)
    private String shippingMethod;

    @Size(max = 100)
    private String serviceLevel;

    private LocalDate estimatedDeliveryDate;

    // Recipient — if null, copied from order shipping address
    @Size(max = 200)
    private String recipientName;

    @Size(max = 30)
    private String recipientPhone;

    @Size(max = 300)
    private String addressLine1;

    @Size(max = 300)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 100)
    private String country;

    private Integer weightGrams;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;

    @DecimalMin(value = "0", message = "Shipping cost cannot be negative")
    private BigDecimal shippingCost;

    @Size(max = 2000)
    private String notes;
}
