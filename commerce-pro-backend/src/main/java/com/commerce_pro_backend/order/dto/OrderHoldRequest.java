package com.commerce_pro_backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for PUT /orders/{id}/hold */
@Data
public class OrderHoldRequest {
    @NotBlank(message = "Hold reason is required")
    private String reason;
}
