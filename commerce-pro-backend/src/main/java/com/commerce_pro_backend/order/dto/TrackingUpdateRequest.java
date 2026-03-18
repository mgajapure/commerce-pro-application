package com.commerce_pro_backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for PATCH /orders/{id}/tracking */
@Data
public class TrackingUpdateRequest {
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    @NotBlank(message = "Carrier is required")
    private String carrier;
}
