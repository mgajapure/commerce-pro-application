package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShippingRuleDTO {
    private String id;
    private String name;
    private String description;
    private String carrierId;
    private String carrierName;
    private String conditionType;
    private BigDecimal conditionMin;
    private BigDecimal conditionMax;
    private String conditionValue;
    private String shippingMethod;
    private String serviceLevel;
    private Integer priority;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
