package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import com.commerce_pro_backend.analytics.dto.ReturnReportDTO;
import com.commerce_pro_backend.analytics.dto.ReturnReportDTO.*;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.entity.OrderItem;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.repository.OrderItemRepository;
import com.commerce_pro_backend.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReturnReportService {

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;

    private static final List<OrderStatus> RETURN_STATUSES = List.of(
            OrderStatus.RETURN_INITIATED, OrderStatus.RETURN_IN_TRANSIT,
            OrderStatus.RETURN_RECEIVED, OrderStatus.REFUNDED, OrderStatus.PARTIALLY_REFUNDED);

    public ReturnReportDTO getReport(ReportFilterDTO filter) {
        LocalDateTime[] range = DateRangeHelper.resolve(filter);
        List<Order> allOrders = orderRepository.findAllByCreatedAtBetween(range[0], range[1]);

        List<Order> returnedOrders = allOrders.stream()
                .filter(o -> RETURN_STATUSES.contains(o.getStatus()))
                .toList();

        long totalOrders    = allOrders.size();
        long ordersWithReturns = returnedOrders.size();

        // Items with returned quantities
        List<String> allIds = allOrders.stream().map(Order::getId).toList();
        List<OrderItem> returnedItems = allIds.isEmpty() ? List.of()
                : orderItemRepository.findReturnedItems(allIds);

        long totalReturnedUnits = returnedItems.stream().mapToLong(OrderItem::getReturnedQuantity).sum();
        BigDecimal returnedValue = returnedItems.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getReturnedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundedAmount = returnedOrders.stream()
                .map(Order::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal returnRate = totalOrders > 0
                ? BigDecimal.valueOf(ordersWithReturns)
                    .divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Reason breakdown from cancellationReason field of return orders
        Map<String, Long> reasonBreakdown = returnedOrders.stream()
                .filter(o -> o.getCancellationReason() != null && !o.getCancellationReason().isBlank())
                .collect(Collectors.groupingBy(
                        o -> normaliseReason(o.getCancellationReason()), Collectors.counting()));
        if (!returnedOrders.isEmpty()) {
            reasonBreakdown.merge("Unspecified",
                    (long) returnedOrders.stream().filter(o -> o.getCancellationReason() == null || o.getCancellationReason().isBlank()).count(),
                    Long::sum);
            reasonBreakdown.remove("Unspecified", 0L);
        }

        // Product return rates
        Map<String, List<OrderItem>> byProduct = returnedItems.stream()
                .collect(Collectors.groupingBy(i -> i.getProductId() != null ? i.getProductId() : "unknown"));

        // Total units sold per product
        List<OrderItem> allItems = allIds.isEmpty() ? List.of() : orderItemRepository.findByOrderIds(allIds);
        Map<String, Long> soldByProduct = allItems.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getProductId() != null ? i.getProductId() : "unknown",
                        Collectors.summingLong(OrderItem::getQuantity)));

        List<ProductReturnRateDTO> productRates = byProduct.entrySet().stream().map(e -> {
            List<OrderItem> ri = e.getValue();
            long unitsRet = ri.stream().mapToLong(OrderItem::getReturnedQuantity).sum();
            long unitsSold = soldByProduct.getOrDefault(e.getKey(), 1L);
            BigDecimal rate = unitsSold > 0
                    ? BigDecimal.valueOf(unitsRet).divide(BigDecimal.valueOf(unitsSold), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            BigDecimal retVal = ri.stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getReturnedQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String name = ri.get(0).getProductName();
            String sku  = ri.get(0).getSku();
            return ProductReturnRateDTO.builder()
                    .productId(e.getKey()).productName(name).sku(sku)
                    .unitsSold(unitsSold).unitsReturned(unitsRet)
                    .returnRatePct(rate).returnedValue(retVal).build();
        }).sorted(Comparator.comparing(ProductReturnRateDTO::getReturnRatePct).reversed()).toList();

        // Trend
        String groupBy = filter.getGroupBy() != null ? filter.getGroupBy().toUpperCase() : "DAY";
        Map<String, List<Order>> grouped = allOrders.stream()
                .collect(Collectors.groupingBy(o -> periodKey(o.getCreatedAt(), groupBy)));
        List<ReturnTrendRowDTO> trend = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<Order> po = e.getValue();
                    long pTotal = po.size();
                    long pRet = po.stream().filter(o -> RETURN_STATUSES.contains(o.getStatus())).count();
                    BigDecimal pRef = po.stream().map(Order::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal pRate = pTotal > 0
                            ? BigDecimal.valueOf(pRet).divide(BigDecimal.valueOf(pTotal), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return ReturnTrendRowDTO.builder()
                            .period(e.getKey()).returnsInitiated(pRet)
                            .unitsReturned(0L).returnRatePct(pRate).refundedAmount(pRef).build();
                }).toList();

        // Detail rows
        List<ReturnDetailRowDTO> rows = returnedItems.stream()
                .map(i -> {
                    Order o = i.getOrder();
                    return ReturnDetailRowDTO.builder()
                            .orderNumber(o.getOrderNumber())
                            .customerName(o.getCustomerName())
                            .productName(i.getProductName()).sku(i.getSku())
                            .returnedQuantity(i.getReturnedQuantity())
                            .refundedAmount(o.getRefundedAmount())
                            .reason(o.getCancellationReason())
                            .orderStatus(o.getStatus().name())
                            .createdAt(o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                            .build();
                }).toList();

        return ReturnReportDTO.builder()
                .totalOrdersInPeriod(totalOrders).ordersWithReturns(ordersWithReturns)
                .totalReturnedUnits(totalReturnedUnits).returnRatePct(returnRate)
                .returnedValueAmount(returnedValue).refundedAmount(refundedAmount)
                .ordersByReason(reasonBreakdown).productReturnRates(productRates)
                .trend(trend).rows(rows).totalRows((long) rows.size())
                .build();
    }

    private String normaliseReason(String reason) {
        if (reason == null) return "Unspecified";
        String lower = reason.toLowerCase();
        if (lower.contains("defect") || lower.contains("damage") || lower.contains("broken")) return "Defective/Damaged";
        if (lower.contains("wrong") || lower.contains("incorrect")) return "Wrong Item";
        if (lower.contains("size") || lower.contains("fit")) return "Size/Fit Issue";
        if (lower.contains("not as described") || lower.contains("expectation")) return "Not As Described";
        if (lower.contains("change") || lower.contains("mind")) return "Changed Mind";
        if (lower.contains("quality")) return "Quality Issue";
        if (lower.contains("late") || lower.contains("delay")) return "Arrived Late";
        return "Other";
    }

    private String periodKey(LocalDateTime dt, String groupBy) {
        return switch (groupBy) {
            case "WEEK"    -> dt.getYear() + "-W" + String.format("%02d", dt.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case "MONTH"   -> dt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "QUARTER" -> dt.getYear() + "-Q" + ((dt.getMonthValue() - 1) / 3 + 1);
            case "YEAR"    -> String.valueOf(dt.getYear());
            default        -> dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        };
    }
}
