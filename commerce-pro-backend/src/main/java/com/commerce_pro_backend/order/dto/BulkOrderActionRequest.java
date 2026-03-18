package com.commerce_pro_backend.order.dto;

import com.commerce_pro_backend.order.enums.OrderStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Request body for POST /orders/bulk-action */
@Data
public class BulkOrderActionRequest {

    @NotEmpty(message = "Order IDs are required")
    private List<String> orderIds;

    @NotNull(message = "Target status is required")
    private OrderStatus targetStatus;

    private String reason;
}
