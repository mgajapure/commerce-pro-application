package com.commerce_pro_backend.payment.dto.request;
import lombok.Data;

@Data
public class FlagTransactionRequest {
    private boolean flagged;
    private String reason;
}
