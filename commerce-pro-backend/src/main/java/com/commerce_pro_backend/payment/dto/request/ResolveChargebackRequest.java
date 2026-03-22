package com.commerce_pro_backend.payment.dto.request;
import lombok.Data;

@Data
public class ResolveChargebackRequest {
    private boolean won;
    private String notes;
}
