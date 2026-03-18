package com.commerce_pro_backend.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryDTO {

    private String id;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime createdAt;
}
