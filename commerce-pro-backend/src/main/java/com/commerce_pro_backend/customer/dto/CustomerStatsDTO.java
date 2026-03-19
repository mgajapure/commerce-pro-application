package com.commerce_pro_backend.customer.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerStatsDTO {
    private Long totalCustomers;
    private Long activeCustomers;
    private Long newCustomersToday;
    private Long newCustomersThisMonth;
    private Long blacklistedCustomers;
    private Long fraudFlaggedCustomers;
    private Map<String, Long> byTier;
    private Map<String, Long> byStatus;
    private BigDecimal totalLifetimeRevenue;
    private BigDecimal averageLifetimeValue;
    private Long loyaltyPointsOutstanding;
}
