package com.commerce_pro_backend.order.dto;

import com.commerce_pro_backend.order.enums.OrderSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

    /** Optional — null means guest order */
    private String customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email must be valid")
    private String customerEmail;

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

    private String couponCode;

    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String shippingMethod;
    private String customerNotes;
    private String internalNotes;

    @Builder.Default
    private String currency = "USD";
}
