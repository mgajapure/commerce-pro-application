package com.commerce_pro_backend.order.enums;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_CAPTURED,
    FAILED,
    VOIDED,
    REFUND_PENDING,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CHARGEBACK,
    DISPUTED
}
