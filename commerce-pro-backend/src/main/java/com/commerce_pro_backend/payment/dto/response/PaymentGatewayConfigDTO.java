package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentGatewayConfigDTO {
    private String id;
    private GatewayProvider gatewayProvider;
    private String displayName;
    private Boolean isTestMode;
    private Boolean isActive;
    private Boolean isDefault;
    private String supportedCurrencies;
    private String webhookUrl;
    // API keys are intentionally NOT included in responses
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
