package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.AnalyticsDashboardDTO;
import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import com.commerce_pro_backend.analytics.dto.SalesOverviewDTO.SalesTrendPointDTO;
import com.commerce_pro_backend.analytics.enums.ReportType;
import com.commerce_pro_backend.fulfillment.entity.Shipment;
import com.commerce_pro_backend.fulfillment.enums.ShipmentStatus;
import com.commerce_pro_backend.fulfillment.repository.ShipmentRepository;
import com.commerce_pro_backend.inventory.repository.InventoryRepository;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.customer.repository.CustomerRepository;
import com.commerce_pro_backend.customer.enums.CustomerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsDashboardService {

    private final OrderRepository     orderRepository;
    private final CustomerRepository  customerRepository;
    private final InventoryRepository inventoryRepository;
    private final ShipmentRepository  shipmentRepository;

    private static final List<OrderStatus> REVENUE_EXCLUDED = List.of(
            OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.DRAFT);
    private static final List<OrderStatus> FULFILLED = List.of(
            OrderStatus.FULFILLED, OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED, OrderStatus.CLOSED);

    public AnalyticsDashboardDTO getDashboard() {
        LocalDateTime now         = LocalDateTime.now();
        LocalDateTime startOfDay  = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfYear  = now.toLocalDate().withDayOfYear(1).atStartOfDay();

        // Revenue
        BigDecimal revenueToday = orderRepository.sumRevenueInRange(REVENUE_EXCLUDED, startOfDay, now);
        BigDecimal revenueMtd   = orderRepository.sumRevenueInRange(REVENUE_EXCLUDED, startOfMonth, now);
        BigDecimal revenueYtd   = orderRepository.sumRevenueInRange(REVENUE_EXCLUDED, startOfYear, now);

        // Previous month for growth
        LocalDateTime prevMonthStart = startOfMonth.minusMonths(1);
        LocalDateTime prevMonthEnd   = startOfMonth.minusSeconds(1);
        BigDecimal prevMonthRevenue  = orderRepository.sumRevenueInRange(REVENUE_EXCLUDED, prevMonthStart, prevMonthEnd);
        BigDecimal revenueGrowthPct  = prevMonthRevenue != null && prevMonthRevenue.compareTo(BigDecimal.ZERO) > 0
                ? revenueMtd.subtract(prevMonthRevenue).divide(prevMonthRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : null;

        // Orders
        long ordersToday = orderRepository.countOrdersSince(startOfDay);
        long ordersMtd   = orderRepository.countOrdersSince(startOfMonth);
        List<Order> mtdOrders = orderRepository.findByCreatedAtBetweenAndStatusNotIn(startOfMonth, now, REVENUE_EXCLUDED);
        BigDecimal aovMtd = !mtdOrders.isEmpty()
                ? revenueMtd.divide(BigDecimal.valueOf(mtdOrders.size()), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Customers
        long newCustomersMtd = customerRepository.findByCreatedAtBetween(startOfMonth, now).size();
        long activeCustomers = customerRepository.countByStatus(CustomerStatus.ACTIVE);

        // Inventory
        BigDecimal inventoryValue = inventoryRepository.getTotalInventoryValue();
        long lowStockSkus  = inventoryRepository.countByStatus(com.commerce_pro_backend.inventory.entity.Inventory.StockStatus.LOW_STOCK);
        long outStockSkus  = inventoryRepository.countByStatus(com.commerce_pro_backend.inventory.entity.Inventory.StockStatus.OUT_OF_STOCK);

        // Fulfillment rate MTD
        long totalMtdOrders    = orderRepository.findAllByCreatedAtBetween(startOfMonth, now).size();
        long fulfilledMtd      = orderRepository.countByStatusInAndCreatedAtBetween(FULFILLED, startOfMonth, now);
        BigDecimal fulfillRate = totalMtdOrders > 0
                ? BigDecimal.valueOf(fulfilledMtd).divide(BigDecimal.valueOf(totalMtdOrders), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // On-time delivery rate MTD
        List<Shipment> mtdShipments = shipmentRepository.findByCreatedAtBetween(startOfMonth, now);
        long totalShip = mtdShipments.size();
        long onTimeShip = mtdShipments.stream().filter(s ->
                s.getStatus() == ShipmentStatus.DELIVERED
                && s.getEstimatedDeliveryDate() != null
                && s.getActualDeliveryDate() != null
                && !s.getActualDeliveryDate().isAfter(s.getEstimatedDeliveryDate())).count();
        BigDecimal onTimeRate = totalShip > 0
                ? BigDecimal.valueOf(onTimeShip).divide(BigDecimal.valueOf(totalShip), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Cancellation rate MTD
        long cancelledMtd = orderRepository.countByStatusInAndCreatedAtBetween(
                List.of(OrderStatus.CANCELLED), startOfMonth, now);
        BigDecimal cancelRate = totalMtdOrders > 0
                ? BigDecimal.valueOf(cancelledMtd).divide(BigDecimal.valueOf(totalMtdOrders), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Return rate MTD
        List<OrderStatus> returnStatuses = List.of(OrderStatus.RETURN_INITIATED, OrderStatus.RETURN_IN_TRANSIT,
                OrderStatus.RETURN_RECEIVED, OrderStatus.REFUNDED, OrderStatus.PARTIALLY_REFUNDED);
        long returnedMtd = orderRepository.countByStatusInAndCreatedAtBetween(returnStatuses, startOfMonth, now);
        BigDecimal returnRate = totalMtdOrders > 0
                ? BigDecimal.valueOf(returnedMtd).divide(BigDecimal.valueOf(totalMtdOrders), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Last-30-days revenue trend (daily)
        LocalDateTime from30 = now.minusDays(29).toLocalDate().atStartOfDay();
        List<Order> last30 = orderRepository.findByCreatedAtBetweenAndStatusNotIn(from30, now, REVENUE_EXCLUDED);
        java.util.Map<String, List<Order>> byDay = last30.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));

        List<SalesTrendPointDTO> revLast30 = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            String day = now.minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<Order> dayOrders = byDay.getOrDefault(day, List.of());
            BigDecimal dayRev = dayOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            revLast30.add(SalesTrendPointDTO.builder()
                    .period(day).revenue(dayRev).orders(dayOrders.size()).units(0L).build());
        }

        return AnalyticsDashboardDTO.builder()
                .revenueToday(revenueToday)
                .revenueMtd(revenueMtd)
                .revenueYtd(revenueYtd)
                .previousMonthRevenue(prevMonthRevenue)
                .revenueGrowthPct(revenueGrowthPct)
                .ordersToday(ordersToday)
                .ordersMtd(ordersMtd)
                .aovMtd(aovMtd)
                .newCustomersMtd(newCustomersMtd)
                .totalActiveCustomers(activeCustomers)
                .totalInventoryValue(inventoryValue != null ? inventoryValue : BigDecimal.ZERO)
                .lowStockSkus(lowStockSkus)
                .outOfStockSkus(outStockSkus)
                .fulfillmentRateMtd(fulfillRate)
                .onTimeDeliveryRateMtd(onTimeRate)
                .cancellationRateMtd(cancelRate)
                .returnRateMtd(returnRate)
                .revenueLast30Days(revLast30)
                .build();
    }
}
