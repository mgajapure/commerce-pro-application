package com.commerce_pro_backend.customer.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerSummaryDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String status;
    private String tier;
    private String groupName;
    private BigDecimal lifetimeSpend;
    private Integer totalOrders;
    private Integer loyaltyPoints;
    private Integer riskScore;
    private Boolean isBlacklisted;
    private Boolean isFraudFlagged;
    private Set<String> tags;
    private LocalDateTime lastOrderAt;
    private LocalDateTime createdAt;
}
