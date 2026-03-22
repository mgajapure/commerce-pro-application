package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.PaymentMethodType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PayViaLinkRequest {
    private GatewayProvider gatewayProvider;
    private PaymentMethodType paymentMethodType;
    private String customerEmail;
}
