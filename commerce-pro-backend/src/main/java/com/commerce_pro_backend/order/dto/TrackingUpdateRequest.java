package com.commerce_pro_backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for PATCH /orders/{id}/tracking */
@Data
public class TrackingUpdateRequest {

    @NotBlank(message = "Tracking number is required")
    @Size(max = 100, message = "Tracking number must be at most 100 characters")
    private String trackingNumber;

    @NotBlank(message = "Carrier is required")
    @Size(max = 100, message = "Carrier name must be at most 100 characters")
    private String carrier;
}
