package com.commerce_pro_backend.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreatePaymentLinkRequest {
    @NotBlank private String title;
    private String orderId;
    private String customerEmail;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    private String currency;
    private Boolean isOneTime;
    private Integer maxUses;
    private LocalDateTime expiresAt;
    private String description;
    private String metadata;
}
