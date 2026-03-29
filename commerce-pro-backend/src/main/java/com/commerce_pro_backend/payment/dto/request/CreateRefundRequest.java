package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.RefundReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRefundRequest {

    @NotBlank(message = "Payment transaction ID is required")
    private String paymentTransactionId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    private BigDecimal refundAmount;

    @NotNull(message = "Refund reason is required")
    private RefundReason reason;

    @Size(max = 1000, message = "Reason detail must be at most 1000 characters")
    private String reasonDetail;

    private Boolean refundShipping;
}
