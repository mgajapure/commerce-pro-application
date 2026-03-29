package com.commerce_pro_backend.payment.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreatePayoutRequest {

    @NotNull(message = "Period start is required")
    private LocalDateTime periodStart;

    @NotNull(message = "Period end is required")
    private LocalDateTime periodEnd;

    @Size(max = 200)
    private String bankAccountName;

    @Pattern(regexp = "\\d{4}", message = "Bank account last 4 must be exactly 4 digits")
    private String bankAccountLast4;

    @Size(max = 2000)
    private String notes;

    @AssertTrue(message = "Period end must be after period start")
    public boolean isPeriodValid() {
        return periodStart == null || periodEnd == null || periodEnd.isAfter(periodStart);
    }
}
