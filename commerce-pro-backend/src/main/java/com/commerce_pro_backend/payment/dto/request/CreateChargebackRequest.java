package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.ChargebackReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateChargebackRequest {
    @NotBlank private String orderId;
    private String paymentTransactionId;
    @NotNull private ChargebackReason reason;
    private String reasonCode;
    @NotNull @DecimalMin("0.01") private BigDecimal disputedAmount;
    private String currency;
    private String gatewayDisputeId;
    @NotNull private LocalDateTime responseDeadline;
    private String internalNotes;
}
