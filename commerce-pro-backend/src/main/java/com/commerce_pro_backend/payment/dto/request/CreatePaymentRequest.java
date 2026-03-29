package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.PaymentMethodType;
import com.commerce_pro_backend.payment.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    @NotNull(message = "Gateway provider is required")
    private GatewayProvider gatewayProvider;

    private PaymentMethodType paymentMethodType;

    /** ID of a stored PaymentMethod for this customer — optional, takes priority over paymentMethodType */
    private String paymentMethodId;

    @Size(max = 2000)
    private String internalNotes;
}
