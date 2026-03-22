package com.commerce_pro_backend.payment.dto.response;

import com.commerce_pro_backend.payment.enums.PaymentLinkStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentLinkDTO {
    private String id;
    private String slug;
    private String payUrl;
    private String title;
    private String orderId;
    private String orderNumber;
    private String customerEmail;
    private PaymentLinkStatus status;
    private BigDecimal amount;
    private String currency;
    private Boolean isOneTime;
    private Integer maxUses;
    private Integer usesCount;
    private LocalDateTime expiresAt;
    private String description;
    private LocalDateTime paidAt;
    private boolean payable;
    private LocalDateTime createdAt;
}
