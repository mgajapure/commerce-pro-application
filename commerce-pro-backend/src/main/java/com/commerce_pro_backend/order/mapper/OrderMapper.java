package com.commerce_pro_backend.order.mapper;

import com.commerce_pro_backend.order.dto.*;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.entity.OrderAddress;
import com.commerce_pro_backend.order.entity.OrderItem;
import com.commerce_pro_backend.order.entity.OrderStatusHistory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper — consistent with ProductMapper and InventoryMapper patterns.
 * No MapStruct here; keeps transformations explicit and easy to debug.
 */
@Component
public class OrderMapper {

    // ── Order → Summary ──────────────────────────────────────────────────────

    public OrderSummaryDTO toSummaryDTO(Order order) {
        if (order == null) return null;

        int itemCount = order.getItems() != null ? order.getItems().size() : 0;
        int totalQty  = order.getItems() != null ? order.getTotalQuantity() : 0;

        return OrderSummaryDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .fulfillmentStatus(order.getFulfillmentStatus() != null ? order.getFulfillmentStatus().name() : null)
                .source(order.getSource() != null ? order.getSource().name() : null)
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .totalQuantity(totalQty)
                .itemCount(itemCount)
                .isFlagged(order.getIsFlagged())
                .riskScore(order.getRiskScore())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public List<OrderSummaryDTO> toSummaryDTOList(List<Order> orders) {
        if (orders == null) return List.of();
        return orders.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    // ── Order → Full Response ─────────────────────────────────────────────────

    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) return null;

        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .fulfillmentStatus(order.getFulfillmentStatus() != null ? order.getFulfillmentStatus().name() : null)
                .source(order.getSource() != null ? order.getSource().name() : null)
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingCost(order.getShippingCost())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .refundedAmount(order.getRefundedAmount())
                .couponCode(order.getCouponCode())
                .discountPercentage(order.getDiscountPercentage())
                .currency(order.getCurrency())
                .shippingAddress(toAddressDTO(order.getShippingAddress()))
                .billingAddress(toAddressDTO(order.getBillingAddress()))
                .shippingMethod(order.getShippingMethod())
                .trackingNumber(order.getTrackingNumber())
                .carrier(order.getCarrier())
                .riskScore(order.getRiskScore())
                .isFlagged(order.getIsFlagged())
                .holdReason(order.getHoldReason())
                .cancellationReason(order.getCancellationReason())
                .internalNotes(order.getInternalNotes())
                .customerNotes(order.getCustomerNotes())
                .confirmedAt(order.getConfirmedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .closedAt(order.getClosedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .createdBy(order.getCreatedBy())
                .updatedBy(order.getUpdatedBy())
                .items(toItemResponseDTOList(order.getItems()))
                .statusHistory(toStatusHistoryDTOList(order.getStatusHistory()))
                .totalQuantity(order.getTotalQuantity())
                .isEditable(order.isEditable())
                .isCancellable(order.isCancellable())
                .build();
    }

    // ── Address ───────────────────────────────────────────────────────────────

    public OrderAddressDTO toAddressDTO(OrderAddress address) {
        if (address == null) return null;
        return OrderAddressDTO.builder()
                .fullName(address.getFullName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .phone(address.getPhone())
                .build();
    }

    public OrderAddress toAddressEntity(OrderAddressDTO dto) {
        if (dto == null) return null;
        return OrderAddress.builder()
                .fullName(dto.getFullName())
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .phone(dto.getPhone())
                .build();
    }

    // ── OrderItem ─────────────────────────────────────────────────────────────

    public OrderItemResponseDTO toItemResponseDTO(OrderItem item) {
        if (item == null) return null;
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .productImageUrl(item.getProductImageUrl())
                .variantInfo(item.getVariantInfo())
                .unitPrice(item.getUnitPrice())
                .costPrice(item.getCostPrice())
                .itemDiscount(item.getItemDiscount())
                .taxRate(item.getTaxRate())
                .taxAmount(item.getTaxAmount())
                .lineTotal(item.getLineTotal())
                .quantity(item.getQuantity())
                .fulfilledQuantity(item.getFulfilledQuantity())
                .returnedQuantity(item.getReturnedQuantity())
                .pendingFulfillmentQuantity(item.getPendingFulfillmentQuantity())
                .createdAt(item.getCreatedAt())
                .build();
    }

    public List<OrderItemResponseDTO> toItemResponseDTOList(List<OrderItem> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toItemResponseDTO).collect(Collectors.toList());
    }

    // ── Status History ────────────────────────────────────────────────────────

    public OrderStatusHistoryDTO toStatusHistoryDTO(OrderStatusHistory history) {
        if (history == null) return null;
        return OrderStatusHistoryDTO.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus() != null ? history.getPreviousStatus().name() : null)
                .newStatus(history.getNewStatus() != null ? history.getNewStatus().name() : null)
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .createdAt(history.getCreatedAt())
                .build();
    }

    public List<OrderStatusHistoryDTO> toStatusHistoryDTOList(List<OrderStatusHistory> histories) {
        if (histories == null) return List.of();
        return histories.stream().map(this::toStatusHistoryDTO).collect(Collectors.toList());
    }
}
