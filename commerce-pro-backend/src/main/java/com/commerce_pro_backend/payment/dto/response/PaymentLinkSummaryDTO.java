package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.PaymentLinkStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentLinkSummaryDTO {
    private String id;
    private String slug;
    private String title;
    private PaymentLinkStatus status;
    private BigDecimal amount;
    private String currency;
    private Integer usesCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
