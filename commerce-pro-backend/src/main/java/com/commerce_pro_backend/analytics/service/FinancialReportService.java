package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.FinancialReportDTO;
import com.commerce_pro_backend.analytics.dto.FinancialReportDTO.*;
import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
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
public class FinancialReportService {

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;

    private static final List<OrderStatus> EXCLUDED = List.of(
            OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.DRAFT);

    public FinancialReportDTO getReport(ReportFilterDTO filter) {
        LocalDateTime[] range    = DateRangeHelper.resolve(filter);
        LocalDateTime[] previous = DateRangeHelper.previousPeriod(range);

        List<Order> orders    = orderRepository.findByCreatedAtBetweenAndStatusNotIn(range[0], range[1], EXCLUDED);
        List<Order> prevOrders = orderRepository.findByCreatedAtBetweenAndStatusNotIn(previous[0], previous[1], EXCLUDED);

        List<String> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> items = orderIds.isEmpty() ? List.of() : orderItemRepository.findByOrderIds(orderIds);

        BigDecimal grossRevenue = orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevRevenue  = prevOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount     = orders.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax          = orders.stream().map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping     = orders.stream().map(Order::getShippingCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds      = orders.stream().map(Order::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue   = grossRevenue.subtract(discount).subtract(refunds);

        // COGS: sum of costPrice * quantity from order items
        BigDecimal cogs = items.stream()
                .filter(i -> i.getCostPrice() != null)
                .map(i -> i.getCostPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossProfit = netRevenue.subtract(cogs);
        BigDecimal grossMargin = netRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(netRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Previous period margin
        List<String> prevIds = prevOrders.stream().map(Order::getId).toList();
        List<OrderItem> prevItems = prevIds.isEmpty() ? List.of() : orderItemRepository.findByOrderIds(prevIds);
        BigDecimal prevGross = prevOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevDisc  = prevOrders.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevRef   = prevOrders.stream().map(Order::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevNet   = prevGross.subtract(prevDisc).subtract(prevRef);
        BigDecimal prevCogs  = prevItems.stream().filter(i -> i.getCostPrice() != null)
                .map(i -> i.getCostPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevGrossProfit = prevNet.subtract(prevCogs);
        BigDecimal prevMargin = prevNet.compareTo(BigDecimal.ZERO) > 0
                ? prevGrossProfit.divide(prevNet, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Tax by effective rate (group items by taxRate)
        Map<String, BigDecimal> taxByRate = items.stream()
                .filter(i -> i.getTaxRate() != null && i.getTaxRate().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(
                        i -> i.getTaxRate().toPlainString(),
                        Collectors.reducing(BigDecimal.ZERO, OrderItem::getTaxAmount, BigDecimal::add)));

        // Margin rows by product
        List<Object[]> marginData = orderIds.isEmpty() ? List.of()
                : orderItemRepository.findMarginDataByProduct(orderIds);

        // Row: [productId, productName, sku, sumLineTotal, sumCogs, sumQty]
        List<MarginRowDTO> marginRows = marginData.stream().map(r -> {
            String productId   = r[0] != null ? (String) r[0] : "";
            String productName = r[1] != null ? (String) r[1] : "";
            String sku         = r[2] != null ? (String) r[2] : "";
            String category    = "Uncategorized";
            BigDecimal rev     = r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO;
            BigDecimal itemCogs = r[4] != null ? (BigDecimal) r[4] : BigDecimal.ZERO;
            long units         = r[5] != null ? ((Number) r[5]).longValue() : 0L;
            BigDecimal profit  = rev.subtract(itemCogs);
            BigDecimal margin  = rev.compareTo(BigDecimal.ZERO) > 0
                    ? profit.divide(rev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            BigDecimal avgSell = units > 0 ? rev.divide(BigDecimal.valueOf(units), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal avgCost = units > 0 ? itemCogs.divide(BigDecimal.valueOf(units), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            return MarginRowDTO.builder()
                    .dimension(productName).dimensionId(productId)
                    .revenue(rev).cogs(itemCogs).grossProfit(profit).marginPct(margin)
                    .unitsSold(units).avgSellingPrice(avgSell).avgCostPrice(avgCost)
                    .build();
        }).sorted(Comparator.comparing(MarginRowDTO::getRevenue).reversed()).toList();

        // Revenue trend
        String groupBy = filter.getGroupBy() != null ? filter.getGroupBy().toUpperCase() : "DAY";
        Map<String, List<Order>> grouped = orders.stream()
                .collect(Collectors.groupingBy(o -> periodKey(o.getCreatedAt(), groupBy)));

        List<RevenueTrendRowDTO> trend = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<Order> po = e.getValue();
                    List<String> pIds = po.stream().map(Order::getId).toList();
                    BigDecimal pGross = po.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal pDisc  = po.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal pRef   = po.stream().map(Order::getRefundedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal pNet   = pGross.subtract(pDisc).subtract(pRef);
                    BigDecimal pTax   = po.stream().map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    List<OrderItem> pItems = pIds.isEmpty() ? List.of() : orderItemRepository.findByOrderIds(pIds);
                    BigDecimal pCogs  = pItems.stream().filter(i -> i.getCostPrice() != null)
                            .map(i -> i.getCostPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal pProfit = pNet.subtract(pCogs);
                    BigDecimal pMargin = pNet.compareTo(BigDecimal.ZERO) > 0
                            ? pProfit.divide(pNet, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return RevenueTrendRowDTO.builder()
                            .period(e.getKey()).grossRevenue(pGross).netRevenue(pNet)
                            .cogs(pCogs).grossProfit(pProfit).grossMarginPct(pMargin)
                            .tax(pTax).refunds(pRef).build();
                }).toList();

        // Tax summary rows (grouped by period)
        List<TaxSummaryRowDTO> taxRows = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<Order> po = e.getValue();
                    BigDecimal pRev = po.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal pTax = po.stream().map(Order::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal rate = pRev.compareTo(BigDecimal.ZERO) > 0
                            ? pTax.divide(pRev, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return TaxSummaryRowDTO.builder()
                            .period(e.getKey()).orders((long) po.size())
                            .taxableRevenue(pRev).taxCollected(pTax).effectiveTaxRate(rate)
                            .build();
                }).toList();

        return FinancialReportDTO.builder()
                .grossRevenue(grossRevenue).totalDiscount(discount).netRevenue(netRevenue)
                .totalCogs(cogs).grossProfit(grossProfit).grossMarginPct(grossMargin)
                .totalTax(tax).totalShipping(shipping).totalRefunds(refunds)
                .netProfit(grossProfit.subtract(shipping))
                .previousGrossRevenue(prevRevenue)
                .revenueChangePct(growthPct(grossRevenue, prevRevenue))
                .previousGrossMarginPct(prevMargin)
                .marginChangePct(growthPct(grossMargin, prevMargin))
                .totalTaxCollected(tax).taxByRate(taxByRate)
                .marginRows(marginRows).revenueTrend(trend).taxSummaryRows(taxRows)
                .build();
    }

    private BigDecimal growthPct(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
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
