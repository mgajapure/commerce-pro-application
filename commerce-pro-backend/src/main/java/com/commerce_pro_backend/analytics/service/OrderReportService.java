package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.OrderReportDTO;
import com.commerce_pro_backend.analytics.dto.OrderReportDTO.*;
import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.OrderStatus;
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
public class OrderReportService {

    private final OrderRepository orderRepository;

    private static final List<OrderStatus> FULFILLED_STATUSES = List.of(
            OrderStatus.FULFILLED, OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED, OrderStatus.CLOSED);
    private static final List<OrderStatus> CANCELLED_STATUSES = List.of(OrderStatus.CANCELLED);
    private static final List<OrderStatus> RETURNED_STATUSES  = List.of(
            OrderStatus.RETURN_INITIATED, OrderStatus.RETURN_IN_TRANSIT,
            OrderStatus.RETURN_RECEIVED, OrderStatus.REFUNDED, OrderStatus.PARTIALLY_REFUNDED);

    public OrderReportDTO getReport(ReportFilterDTO filter) {
        LocalDateTime[] range = DateRangeHelper.resolve(filter);
        List<Order> all = orderRepository.findAllByCreatedAtBetween(range[0], range[1]);

        long total      = all.size();
        long fulfilled  = all.stream().filter(o -> FULFILLED_STATUSES.contains(o.getStatus())).count();
        long cancelled  = all.stream().filter(o -> CANCELLED_STATUSES.contains(o.getStatus())).count();
        long returned   = all.stream().filter(o -> RETURNED_STATUSES.contains(o.getStatus())).count();
        long pending    = total - fulfilled - cancelled - returned;

        BigDecimal fulfillmentRate  = pct(fulfilled, total);
        BigDecimal cancellationRate = pct(cancelled, total);
        BigDecimal returnRate       = pct(returned, total);

        // Average fulfillment time (CONFIRMED → SHIPPED)
        OptionalDouble avgHours = all.stream()
                .filter(o -> o.getConfirmedAt() != null && o.getShippedAt() != null)
                .mapToLong(o -> java.time.temporal.ChronoUnit.HOURS.between(o.getConfirmedAt(), o.getShippedAt()))
                .filter(h -> h >= 0)
                .average();

        // Status breakdown
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        // Source breakdown
        Map<String, Long> bySource = all.stream()
                .collect(Collectors.groupingBy(o -> o.getSource().name(), Collectors.counting()));

        // Trend
        String groupBy = filter.getGroupBy() != null ? filter.getGroupBy().toUpperCase() : "DAY";
        Map<String, List<Order>> grouped = all.stream()
                .collect(Collectors.groupingBy(o -> periodKey(o.getCreatedAt(), groupBy)));

        List<OrderTrendRowDTO> trend = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<Order> po = e.getValue();
                    long pTotal     = po.size();
                    long pFulfilled = po.stream().filter(o -> FULFILLED_STATUSES.contains(o.getStatus())).count();
                    long pCancelled = po.stream().filter(o -> CANCELLED_STATUSES.contains(o.getStatus())).count();
                    BigDecimal rev  = po.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return OrderTrendRowDTO.builder()
                            .period(e.getKey())
                            .totalOrders(pTotal)
                            .fulfilledOrders(pFulfilled)
                            .cancelledOrders(pCancelled)
                            .fulfillmentRatePct(pct(pFulfilled, pTotal))
                            .cancellationRatePct(pct(pCancelled, pTotal))
                            .revenue(rev)
                            .build();
                }).toList();

        // Detail rows
        List<OrderDetailRowDTO> rows = all.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::toDetailRow)
                .toList();

        return OrderReportDTO.builder()
                .totalOrders(total)
                .fulfilledOrders(fulfilled)
                .cancelledOrders(cancelled)
                .returnedOrders(returned)
                .pendingOrders(Math.max(0, pending))
                .fulfillmentRatePct(fulfillmentRate)
                .cancellationRatePct(cancellationRate)
                .returnRatePct(returnRate)
                .averageFulfillmentHours(avgHours.isPresent()
                        ? BigDecimal.valueOf(avgHours.getAsDouble()).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .ordersByStatus(byStatus)
                .ordersBySource(bySource)
                .trend(trend)
                .rows(rows)
                .totalRows(total)
                .build();
    }

    private OrderDetailRowDTO toDetailRow(Order o) {
        return OrderDetailRowDTO.builder()
                .orderNumber(o.getOrderNumber())
                .customerId(o.getCustomerId())
                .customerName(o.getCustomerName())
                .customerEmail(o.getCustomerEmail())
                .status(o.getStatus().name())
                .fulfillmentStatus(o.getFulfillmentStatus().name())
                .paymentStatus(o.getPaymentStatus().name())
                .source(o.getSource().name())
                .totalAmount(o.getTotalAmount())
                .itemCount(o.getItems().size())
                .createdAt(fmt(o.getCreatedAt()))
                .shippedAt(fmt(o.getShippedAt()))
                .deliveredAt(fmt(o.getDeliveredAt()))
                .cancellationReason(o.getCancellationReason())
                .carrierName(o.getCarrier())
                .trackingNumber(o.getTrackingNumber())
                .build();
    }

    private BigDecimal pct(long part, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
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

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
