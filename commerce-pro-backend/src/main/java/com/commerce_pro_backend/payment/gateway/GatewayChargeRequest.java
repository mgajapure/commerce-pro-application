package com.commerce_pro_backend.payment.gateway;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.PaymentMethodType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

// ── GatewayChargeRequest ──────────────────────────────────────────────────────
@Data
@Builder
public class GatewayChargeRequest {
    private String orderId;
    private String orderNumber;
    private BigDecimal amount;
    private String currency;
    private GatewayProvider provider;
    private PaymentMethodType methodType;
    // Raw card data (NEVER stored — passed straight to gateway then discarded)
    private String cardNumber;
    private String cardExpMonth;
    private String cardExpYear;
    private String cardCvv;
    private String cardHolderName;
    // Or stored card token
    private String gatewayToken;
    private String idempotencyKey;
    private String customerEmail;
    private String ipAddress;
}
