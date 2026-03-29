package com.commerce_pro_backend.order.dto;

import com.commerce_pro_backend.order.enums.OrderSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequestDTO {

    private String customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email must be valid")
    private String customerEmail;

    @Size(max = 30)
    private String customerPhone;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    @Builder.Default
    private List<OrderItemRequestDTO> items = new ArrayList<>();

    @Valid
    private OrderAddressDTO shippingAddress;

    @Valid
    private OrderAddressDTO billingAddress;

    @Builder.Default
    private OrderSource source = OrderSource.MANUAL;

    @Size(max = 50)
    private String couponCode;

    @DecimalMin(value = "0", message = "Shipping cost cannot be negative")
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "Discount amount cannot be negative")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Size(max = 100)
    private String shippingMethod;

    @Size(max = 2000)
    private String customerNotes;

    @Size(max = 2000)
    private String internalNotes;

    @Builder.Default
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a valid ISO 4217 code (e.g. USD)")
    private String currency = "USD";
}
