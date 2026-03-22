package com.commerce_pro_backend.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreatePayoutRequest {
    @NotNull private LocalDateTime periodStart;
    @NotNull private LocalDateTime periodEnd;
    private String bankAccountName;
    private String bankAccountLast4;
    private String notes;
}
