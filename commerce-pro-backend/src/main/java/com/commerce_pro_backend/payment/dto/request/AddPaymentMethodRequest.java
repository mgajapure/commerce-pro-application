package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddPaymentMethodRequest {
    @NotBlank private String customerId;
    @NotNull  private PaymentMethodType methodType;
    private GatewayProvider gatewayProvider;
    private String gatewayToken;
    private String cardLast4;
    private String cardBrand;
    private String cardExpMonth;
    private String cardExpYear;
    private String cardHolderName;
    private String accountLabel;
    private Boolean isDefault;
}
