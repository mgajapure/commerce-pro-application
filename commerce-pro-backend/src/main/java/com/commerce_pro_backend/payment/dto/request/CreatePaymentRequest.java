package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.PaymentMethodType;
import com.commerce_pro_backend.payment.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {
    @NotBlank private String orderId;
    @NotNull  private TransactionType transactionType;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotBlank @Size(max=3) private String currency;
    @NotNull  private GatewayProvider gatewayProvider;
    private PaymentMethodType paymentMethodType;
    private String paymentMethodId;   // stored card ID
    private String internalNotes;
}
