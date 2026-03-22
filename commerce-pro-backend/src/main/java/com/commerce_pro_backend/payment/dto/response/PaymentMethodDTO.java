package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.PaymentMethodType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentMethodDTO {
    private String id;
    private String customerId;
    private PaymentMethodType methodType;
    private GatewayProvider gatewayProvider;
    private String cardLast4;
    private String cardBrand;
    private String cardExpMonth;
    private String cardExpYear;
    private String cardHolderName;
    private String accountLabel;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}
