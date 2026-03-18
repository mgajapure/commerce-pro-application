package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdatePickItemRequest {

    @NotNull(message = "Quantity picked is required")
    @Min(value = 0, message = "Quantity picked must be >= 0")
    private Integer quantityPicked;

    private String notes;
}
