package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShippingRuleRequestDTO {

    @NotBlank(message = "Rule name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    private String carrierId;

    @NotBlank(message = "Condition type is required")
    private String conditionType;

    private BigDecimal conditionMin;
    private BigDecimal conditionMax;
    private String conditionValue;

    @NotBlank(message = "Shipping method is required")
    @Size(max = 100)
    private String shippingMethod;

    @Size(max = 100)
    private String serviceLevel;

    private Integer priority;
    private Boolean isActive;
}
