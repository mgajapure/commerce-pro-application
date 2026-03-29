package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.ChargebackReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateChargebackRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    private String paymentTransactionId;

    @NotNull(message = "Chargeback reason is required")
    private ChargebackReason reason;

    @Size(max = 50)
    private String reasonCode;

    @NotNull(message = "Disputed amount is required")
    @DecimalMin(value = "0.01", message = "Disputed amount must be greater than 0")
    private BigDecimal disputedAmount;

    @Size(min = 3, max = 3)
    private String currency;

    @Size(max = 200)
    private String gatewayDisputeId;

    @NotNull(message = "Response deadline is required")
    private LocalDateTime responseDeadline;

    @Size(max = 2000)
    private String internalNotes;
}
