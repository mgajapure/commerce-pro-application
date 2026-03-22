package com.commerce_pro_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentOrderDTO {

    private String orderId;
    private String orderNumber;
    private String customerName;
    private BigDecimal total;
    private String status;
    private LocalDateTime createdAt;
}
