package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import com.commerce_pro_backend.analytics.dto.SalesOverviewDTO;
import com.commerce_pro_backend.analytics.dto.SalesOverviewDTO.SalesTrendPointDTO;
import com.commerce_pro_backend.analytics.dto.SalesReportDTO;
import com.commerce_pro_backend.analytics.dto.SalesReportDTO.SalesRowDTO;
import com.commerce_pro_backend.analytics.dto.SalesReportDTO.TopProductDTO;
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
public class SalesReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private static final List<OrderStatus> EXCLUDED = List.of(
            OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.DRAFT);

    // ── Overview ──────────────────────────────────────────────────────────────

    public SalesOverviewDTO getOverview(ReportFilterDTO filter) {
        LocalDateTime[] range    = DateRangeHelper.resolve(filter);
        LocalDateTime[] previous = DateRangeHelper.previousPeriod(range);

        List<Order> orders    = fetchRevenueOrders(range[0], range[1]);
        List<Order> prevOrders = fetchRevenueOrders(previous[0], previous[1]);

        BigDecimal revenue     = sumRevenue(orders);
        BigDecimal prevRevenue = sumRevenue(prevOrders);
        long orderCount        = orders.size();
        long prevOrderCount    = prevOrders.size();
        BigDecimal aov         = orderCount > 0
                ? revenue.divide(BigDecimal.valueOf(orderCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal prevAov     = prevOrderCount > 0
                ? prevRevenue.divide(BigDecimal.valueOf(prevOrderCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return SalesOverviewDTO.builder()
                .totalRevenue(revenue)
                .previousPeriodRevenue(prevRevenue)
                .revenueGrowthPct(growthPct(revenue, prevRevenue))
                .totalOrders(orderCount)
                .previousPeriodOrders(prevOrderCount)
                .orderGrowthPct(growthPct(BigDecimal.valueOf(orderCount), BigDecimal.valueOf(prevOrderCount)))
                .averageOrderValue(aov)
                .previousPeriodAov(prevAov)
                .aovGrowthPct(growthPct(aov, prevAov))
                .totalDiscount(orders.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalTax(orders.stream().map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalShipping(orders.stream().map(Order::getShippingCost).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalUnits(orderItemRepository.sumUnitsByOrderIds(orders.stream().map(Order::getId).toList()))
                .trend(buildTrend(orders, filter.getGroupBy()))
                .build();
    }

    // ── By Date ──────────────────────────────────────────────────────────────

    public SalesReportDTO getByDate(ReportFilterDTO filter) {
        LocalDateTime[] range   = DateRangeHelper.resolve(filter);
        LocalDateTime[] previous = DateRangeHelper.previousPeriod(range);

        List<Order> orders    = fetchRevenueOrders(range[0], range[1]);
        List<Order> prevOrders = fetchRevenueOrders(previous[0], previous[1]);

        String groupBy = filter.getGroupBy() != null ? filter.getGroupBy().toUpperCase() : "DAY";

        Map<String, List<Order>> grouped = groupOrdersByPeriod(orders, groupBy);

        List<SalesRowDTO> rows = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> toSalesRow(e.getKey(), e.getValue()))
                .toList();

        BigDecimal totalRev = sumRevenue(orders);
        // back-fill sharePct
        rows.forEach(r -> {
            if (totalRev.compareTo(BigDecimal.ZERO) > 0) {
                r.setSharePct(r.getRevenue().divide(totalRev, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            }
        });

        return buildSummary(orders, prevOrders, rows, filter);
    }

    // ── By Product ───────────────────────────────────────────────────────────

    public SalesReportDTO getByProduct(ReportFilterDTO filter) {
        LocalDateTime[] range    = DateRangeHelper.resolve(filter);
        LocalDateTime[] previous = DateRangeHelper.previousPeriod(range);

        List<Order> orders    = fetchRevenueOrders(range[0], range[1]);
        List<Order> prevOrders = fetchRevenueOrders(previous[0], previous[1]);

        List<OrderItem> items = orderItemRepository.findByOrderIds(
                orders.stream().map(Order::getId).toList());

        Map<String, List<OrderItem>> byProduct = items.stream()
                .collect(Collectors.groupingBy(i -> i.getProductId() != null ? i.getProductId() : "unknown"));

        BigDecimal totalRev = items.stream().map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SalesRowDTO> rows = byProduct.entrySet().stream()
                .map(e -> {
                    List<OrderItem> pItems = e.getValue();
                    BigDecimal rev = pItems.stream().map(OrderItem::getLineTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    long units = pItems.stream().mapToLong(OrderItem::getQuantity).sum();
                    String name = pItems.get(0).getProductName();
                    BigDecimal discount = pItems.stream().map(i ->
                            i.getItemDiscount().multiply(BigDecimal.valueOf(i.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal tax = pItems.stream().map(OrderItem::getTaxAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sharePct = totalRev.compareTo(BigDecimal.ZERO) > 0
                            ? rev.divide(totalRev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    // unique order count for this product
                    long orderCount = pItems.stream()
                            .map(i -> i.getOrder().getId()).distinct().count();

                    return SalesRowDTO.builder()
                            .dimension(name)
                            .dimensionId(e.getKey())
                            .revenue(rev)
                            .discount(discount)
                            .tax(tax)
                            .shipping(BigDecimal.ZERO)
                            .refunded(BigDecimal.ZERO)
                            .orders(orderCount)
                            .units(units)
                            .aov(orderCount > 0 ? rev.divide(BigDecimal.valueOf(orderCount), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .sharePct(sharePct)
                            .build();
                })
                .sorted(Comparator.comparing(SalesRowDTO::getRevenue).reversed())
                .toList();

        return buildSummary(orders, prevOrders, rows, filter);
    }

    // ── By Category ──────────────────────────────────────────────────────────

    public SalesReportDTO getByCategory(ReportFilterDTO filter) {
        LocalDateTime[] range    = DateRangeHelper.resolve(filter);
        LocalDateTime[] previous = DateRangeHelper.previousPeriod(range);

        List<Order> orders    = fetchRevenueOrders(range[0], range[1]);
        List<Order> prevOrders = fetchRevenueOrders(previous[0], previous[1]);

        List<OrderItem> items = orderItemRepository.findByOrderIds(
                orders.stream().map(Order::getId).toList());

        // category info is not directly on OrderItem; fetch from product repository
        // We join via the productId from the snapshot — items without product keep "Uncategorized"
        List<Object[]> catRaw = orderItemRepository.sumRevenueByCategory(
                orders.stream().map(Order::getId).toList());
        // Row format: [productId, productName, sumLineTotal, sumQuantity]
        // Group by category requires a product lookup; we use productName as dimension fallback
        // and aggregate by first letter of productName as a category proxy when no product repo available
        // For a cleaner implementation wire in ProductRepository — here we group by item-level name
        Map<String, BigDecimal[]> catAgg = new LinkedHashMap<>();
        for (Object[] row : catRaw) {
            String name = row[1] != null ? (String) row[1] : "Unknown";
            BigDecimal rev = (BigDecimal) row[2];
            long units = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            catAgg.merge(name, new BigDecimal[]{rev, BigDecimal.valueOf(units)},
                (a, b) -> new BigDecimal[]{a[0].add(b[0]), a[1].add(b[1])});
        }

        BigDecimal totalRev = orders.stream().map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SalesRowDTO> rows = catAgg.entrySet().stream().map(e -> {
            BigDecimal rev   = e.getValue()[0];
            long units       = e.getValue()[1].longValue();
            BigDecimal sharePct = totalRev.compareTo(BigDecimal.ZERO) > 0
                    ? rev.divide(totalRev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            return SalesRowDTO.builder()
                    .dimension(e.getKey())
                    .revenue(rev)
                    .units(units)
                    .sharePct(sharePct)
                    .orders(0L)
                    .discount(BigDecimal.ZERO)
                    .tax(BigDecimal.ZERO)
                    .shipping(BigDecimal.ZERO)
                    .refunded(BigDecimal.ZERO)
                    .aov(BigDecimal.ZERO)
                    .build();
        }).sorted(Comparator.comparing(SalesRowDTO::getRevenue).reversed()).toList();

        return buildSummary(orders, prevOrders, rows, filter);
    }

    // ── By Channel ───────────────────────────────────────────────────────────

    public SalesReportDTO getByChannel(ReportFilterDTO filter) {
        LocalDateTime[] range    = DateRangeHelper.resolve(filter);
        LocalDateTime[] previous = DateRangeHelper.previousPeriod(range);

        List<Order> orders    = fetchRevenueOrders(range[0], range[1]);
        List<Order> prevOrders = fetchRevenueOrders(previous[0], previous[1]);

        Map<String, List<Order>> byChannel = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getSource().name()));

        BigDecimal totalRev = sumRevenue(orders);

        List<SalesRowDTO> rows = byChannel.entrySet().stream()
                .map(e -> {
                    List<Order> co = e.getValue();
                    BigDecimal rev = sumRevenue(co);
                    long cnt = co.size();
                    BigDecimal aov = cnt > 0
                            ? rev.divide(BigDecimal.valueOf(cnt), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal sharePct = totalRev.compareTo(BigDecimal.ZERO) > 0
                            ? rev.divide(totalRev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return SalesRowDTO.builder()
                            .dimension(e.getKey())
                            .revenue(rev)
                            .orders(cnt)
                            .aov(aov)
                            .sharePct(sharePct)
                            .discount(co.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                            .tax(co.stream().map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                            .shipping(co.stream().map(Order::getShippingCost).reduce(BigDecimal.ZERO, BigDecimal::add))
                            .refunded(co.stream().map(Order::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                            .units(0L)
                            .build();
                })
                .sorted(Comparator.comparing(SalesRowDTO::getRevenue).reversed())
                .toList();

        return buildSummary(orders, prevOrders, rows, filter);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Order> fetchRevenueOrders(LocalDateTime from, LocalDateTime to) {
        return orderRepository.findByCreatedAtBetweenAndStatusNotIn(from, to, EXCLUDED);
    }

    private BigDecimal sumRevenue(List<Order> orders) {
        return orders.stream().map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal growthPct(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private List<SalesTrendPointDTO> buildTrend(List<Order> orders, String rawGroupBy) {
        String groupBy = rawGroupBy != null ? rawGroupBy.toUpperCase() : "DAY";
        Map<String, List<Order>> grouped = groupOrdersByPeriod(orders, groupBy);
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<Order> po = e.getValue();
                    BigDecimal rev = sumRevenue(po);
                    long cnt = po.size();
                    return SalesTrendPointDTO.builder()
                            .period(e.getKey())
                            .revenue(rev)
                            .orders(cnt)
                            .aov(cnt > 0 ? rev.divide(BigDecimal.valueOf(cnt), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .units(0L)
                            .build();
                }).toList();
    }

    private Map<String, List<Order>> groupOrdersByPeriod(List<Order> orders, String groupBy) {
        return orders.stream().collect(Collectors.groupingBy(o -> periodKey(o.getCreatedAt(), groupBy)));
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

    private SalesRowDTO toSalesRow(String period, List<Order> po) {
        BigDecimal rev = sumRevenue(po);
        long cnt = po.size();
        return SalesRowDTO.builder()
                .dimension(period)
                .revenue(rev)
                .orders(cnt)
                .aov(cnt > 0 ? rev.divide(BigDecimal.valueOf(cnt), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .discount(po.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .tax(po.stream().map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .shipping(po.stream().map(Order::getShippingCost).reduce(BigDecimal.ZERO, BigDecimal::add))
                .refunded(po.stream().map(Order::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .units(0L)
                .sharePct(BigDecimal.ZERO)
                .build();
    }

    private SalesReportDTO buildSummary(List<Order> orders, List<Order> prevOrders,
                                        List<SalesRowDTO> rows, ReportFilterDTO filter) {
        BigDecimal rev     = sumRevenue(orders);
        BigDecimal prevRev = sumRevenue(prevOrders);
        long cnt           = orders.size();
        long prevCnt       = prevOrders.size();

        // Top 10 products
        List<Object[]> topRawAll = orderItemRepository.findTopProductsByRevenue(
                orders.stream().map(Order::getId).toList(), 10);
        // Row: [productId, productName, sku, sumQty, sumLineTotal]
        List<Object[]> topRaw = topRawAll.stream().limit(10).toList();
        List<TopProductDTO> top = topRaw.stream().map(r -> TopProductDTO.builder()
                .productId(r[0] != null ? (String) r[0] : "")
                .productName(r[1] != null ? (String) r[1] : "")
                .sku(r[2] != null ? (String) r[2] : "")
                .category("")
                .unitsSold(r[3] != null ? ((Number) r[3]).longValue() : 0L)
                .revenue(r[4] != null ? (BigDecimal) r[4] : BigDecimal.ZERO)
                .build()).toList();

        return SalesReportDTO.builder()
                .totalRevenue(rev)
                .previousRevenue(prevRev)
                .revenueChangePct(growthPct(rev, prevRev))
                .totalDiscount(orders.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalTax(orders.stream().map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalShipping(orders.stream().map(Order::getShippingCost).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalRefunded(orders.stream().map(Order::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .netRevenue(rev.subtract(orders.stream().map(Order::getRefundedAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .totalOrders(cnt)
                .previousOrders(prevCnt)
                .ordersChangePct(growthPct(BigDecimal.valueOf(cnt), BigDecimal.valueOf(prevCnt)))
                .averageOrderValue(cnt > 0 ? rev.divide(BigDecimal.valueOf(cnt), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalUnits(0L)
                .rows(rows)
                .topProducts(top)
                .build();
    }
}
