package com.commerce_pro_backend.customer.service;

import com.commerce_pro_backend.customer.dto.OrderHistoryDTO;
import com.commerce_pro_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Interface for querying Order module data from within the Customer module.
 * Implemented by DefaultCustomerOrderQueryService which injects OrderRepository.
 * This keeps cross-module coupling at the interface boundary only.
 */
public interface CustomerOrderQueryService {
    PageResponse<OrderHistoryDTO> getOrderHistory(String customerId, Pageable pageable);
    long countOrdersByCustomer(String customerId);
    BigDecimal sumRevenueByCustomer(String customerId);
}
