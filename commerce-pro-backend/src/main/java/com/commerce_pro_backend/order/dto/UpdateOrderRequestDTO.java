package com.commerce_pro_backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequestDTO {

    @Size(max = 200)
    private String customerName;

    @Email(message = "Customer email must be valid")
    @Size(max = 255)
    private String customerEmail;

    @Size(max = 30)
    private String customerPhone;

    @Valid
    private OrderAddressDTO shippingAddress;

    @Valid
    private OrderAddressDTO billingAddress;

    private java.util.List<@Valid OrderItemRequestDTO> items;

    @DecimalMin(value = "0", message = "Shipping cost cannot be negative")
    private BigDecimal shippingCost;

    @DecimalMin(value = "0", message = "Discount amount cannot be negative")
    private BigDecimal discountAmount;

    @Size(max = 50)
    private String couponCode;

    @Size(max = 100)
    private String shippingMethod;

    @Size(max = 2000)
    private String internalNotes;

    @Size(max = 2000)
    private String customerNotes;
}
