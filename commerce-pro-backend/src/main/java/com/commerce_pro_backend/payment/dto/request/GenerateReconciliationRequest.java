package com.commerce_pro_backend.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GenerateReconciliationRequest {
    @NotNull private LocalDateTime periodStart;
    @NotNull private LocalDateTime periodEnd;
    private String notes;
}
