package com.commerce_pro_backend.order.dto;

import com.commerce_pro_backend.order.enums.OrderStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class BulkOrderActionRequest {

    private static final Set<OrderStatus> ALLOWED_BULK_TARGETS = Set.of(
        OrderStatus.CANCELLED,
        OrderStatus.ON_HOLD,
        OrderStatus.CONFIRMED,
        OrderStatus.PROCESSING,
        OrderStatus.CLOSED
    );

    @NotEmpty(message = "Order IDs are required")
    private List<String> orderIds;

    @NotNull(message = "Target status is required")
    private OrderStatus targetStatus;

    private String reason;

    @AssertTrue(message = "targetStatus must be one of: CANCELLED, ON_HOLD, CONFIRMED, PROCESSING, CLOSED")
    public boolean isTargetStatusAllowed() {
        return targetStatus != null && ALLOWED_BULK_TARGETS.contains(targetStatus);
    }
}
