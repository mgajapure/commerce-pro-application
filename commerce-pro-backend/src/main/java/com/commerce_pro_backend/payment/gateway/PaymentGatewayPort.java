package com.commerce_pro_backend.payment.gateway;

import com.commerce_pro_backend.payment.enums.GatewayProvider;

import java.math.BigDecimal;

/**
 * Strategy interface for payment gateway integrations.
 *
 * The ManualPaymentGateway stub is the only implementation in dev.
 * Real gateways (Stripe, PayPal, etc.) implement this interface and
 * are swapped in without touching any service code.
 */
public interface PaymentGatewayPort {

    /** Immediate full charge — no separate capture step. */
    GatewayChargeResponse charge(GatewayChargeRequest request);

    /** Place an authorization hold — funds reserved but not captured. */
    GatewayChargeResponse authorize(GatewayChargeRequest request);

    /** Capture a previously authorized hold. */
    GatewayChargeResponse capture(String gatewayTransactionId, BigDecimal amount);

    /** Cancel an authorization before capture. */
    GatewayChargeResponse voidAuthorization(String gatewayTransactionId);

    /** Refund a captured / settled charge. */
    GatewayRefundResponse refund(String gatewayTransactionId, BigDecimal amount, String reason);

    /** Return true if this implementation handles the given provider. */
    boolean supports(GatewayProvider provider);
}
