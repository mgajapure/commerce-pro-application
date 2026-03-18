package com.commerce_pro_backend.order.enums;

/**
 * Order lifecycle statuses — mirrors the full state machine defined in the design document.
 */
public enum OrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    PAYMENT_FAILED,
    CONFIRMED,
    ON_HOLD,
    PROCESSING,
    PARTIALLY_FULFILLED,
    FULFILLED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    RETURN_INITIATED,
    RETURN_IN_TRANSIT,
    RETURN_RECEIVED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CLOSED
}
