package com.commerce_pro_backend.order.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Partial-update request — all fields optional.
 * Only pre-fulfillment orders (DRAFT / PENDING_PAYMENT / CONFIRMED) can be edited.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequestDTO {

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    @Valid
    private OrderAddressDTO shippingAddress;

    @Valid
    private OrderAddressDTO billingAddress;

    @Valid
    private List<OrderItemRequestDTO> items;

    private BigDecimal shippingCost;
    private BigDecimal discountAmount;
    private String couponCode;
    private String shippingMethod;
    private String internalNotes;
    private String customerNotes;
}
