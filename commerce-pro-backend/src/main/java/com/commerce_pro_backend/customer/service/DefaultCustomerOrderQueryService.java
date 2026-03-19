package com.commerce_pro_backend.customer.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.customer.dto.OrderHistoryDTO;
import com.commerce_pro_backend.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * Concrete implementation — injects OrderRepository (order module).
 * The Customer module only sees the CustomerOrderQueryService interface.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultCustomerOrderQueryService implements CustomerOrderQueryService {

    private final OrderRepository orderRepository;

    @Override
    public PageResponse<OrderHistoryDTO> getOrderHistory(String customerId, Pageable pageable) {
        var page = orderRepository.findByCustomerId(customerId, pageable);
        var dtos = page.getContent().stream().map(o -> OrderHistoryDTO.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus() != null ? o.getStatus().name() : null)
                .paymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null)
                .fulfillmentStatus(o.getFulfillmentStatus() != null ? o.getFulfillmentStatus().name() : null)
                .totalAmount(o.getTotalAmount())
                .currency(o.getCurrency())
                .itemCount(o.getItems() != null ? o.getItems().size() : 0)
                .createdAt(o.getCreatedAt())
                .deliveredAt(o.getDeliveredAt())
                .build()
        ).collect(Collectors.toList());
        return PageResponse.from(page, dtos);
    }

    @Override
    public long countOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).size();
    }

    @Override
    public BigDecimal sumRevenueByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
