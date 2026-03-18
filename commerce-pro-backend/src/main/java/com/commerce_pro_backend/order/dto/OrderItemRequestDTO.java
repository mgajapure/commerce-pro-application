package com.commerce_pro_backend.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequestDTO {

    @NotBlank(message = "Product ID is required")
    private String productId;

    /** Optional variant clarification shown on the line item */
    private String variantInfo;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * Override unit price — if null the service resolves the current catalogue price.
     * Useful for manual orders where the operator negotiates a different price.
     */
    private BigDecimal unitPrice;

    /** Line-level discount amount (not percentage). */
    @Builder.Default
    private BigDecimal itemDiscount = BigDecimal.ZERO;

    /** Tax rate as a decimal fraction, e.g. 0.08 for 8%. */
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;
}
