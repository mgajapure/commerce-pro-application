package com.commerce_pro_backend.payment.dto.request;

import com.commerce_pro_backend.payment.enums.RefundReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateRefundRequest {
    @NotBlank private String paymentTransactionId;
    @NotNull @DecimalMin("0.01") private BigDecimal refundAmount;
    @NotNull private RefundReason reason;
    private String reasonDetail;
    private Boolean refundShipping;
}
