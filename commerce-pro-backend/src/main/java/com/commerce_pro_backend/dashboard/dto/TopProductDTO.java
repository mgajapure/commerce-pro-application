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
public class TopProductDTO {

    private String productId;
    private String productName;
    private String sku;
    private String imageUrl;
    private int totalSold;
    private BigDecimal totalRevenue;
}
