package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LoyaltyAdjustDTO {
    @NotNull private Integer points;       // positive = add, negative = deduct
    @NotBlank private String description;
}
