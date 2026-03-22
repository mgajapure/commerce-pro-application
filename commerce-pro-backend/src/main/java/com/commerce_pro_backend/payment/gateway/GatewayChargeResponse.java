package com.commerce_pro_backend.payment.gateway;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GatewayChargeResponse {
    private boolean success;
    private String gatewayTransactionId;
    private String gatewayChargeId;
    private String authorizationCode;
    private BigDecimal gatewayFee;
    private String responseCode;
    private String responseMessage;
    private String failureReason;
    // Returned by gateway after tokenisation — safe to store
    private String cardLast4;
    private String cardBrand;
    private String cardExpMonth;
    private String cardExpYear;
}
