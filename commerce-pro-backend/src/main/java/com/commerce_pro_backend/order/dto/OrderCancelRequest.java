package com.commerce_pro_backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for DELETE /orders/{id} (soft cancel) */
@Data
public class OrderCancelRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
