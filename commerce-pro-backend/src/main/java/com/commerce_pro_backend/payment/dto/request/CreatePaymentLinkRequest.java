package com.commerce_pro_backend.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreatePaymentLinkRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    private String orderId;

    @Email(message = "Customer email must be valid")
    @Size(max = 255)
    private String customerEmail;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    private Boolean isOneTime;

    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    private LocalDateTime expiresAt;

    @Size(max = 1000)
    private String description;

    private String metadata;
}
