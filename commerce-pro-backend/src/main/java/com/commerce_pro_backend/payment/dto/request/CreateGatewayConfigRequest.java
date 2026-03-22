package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGatewayConfigRequest {
    @NotNull  private GatewayProvider gatewayProvider;
    @NotBlank private String displayName;
    private String apiKey;
    private String apiSecret;
    private String webhookSecret;
    private Boolean isTestMode;
    private String supportedCurrencies;
    private String webhookUrl;
    private String configMetadata;
}
