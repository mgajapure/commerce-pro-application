package com.commerce_pro_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatusDTO {

    private long totalItems;
    private long lowStockCount;
    private long outOfStockCount;
    private BigDecimal totalValue;
}
