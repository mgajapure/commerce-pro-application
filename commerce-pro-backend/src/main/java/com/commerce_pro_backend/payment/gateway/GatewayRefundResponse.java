package com.commerce_pro_backend.payment.gateway;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GatewayRefundResponse {
    private boolean success;
    private String gatewayRefundId;
    private String gatewayStatus;
    private String responseCode;
    private String responseMessage;
    private String failureReason;
}
