package com.commerce_pro_backend.dashboard.service;

import com.commerce_pro_backend.catalog.product.entity.Product;
import com.commerce_pro_backend.catalog.product.repository.ProductRepository;
import com.commerce_pro_backend.customer.repository.CustomerRepository;
import com.commerce_pro_backend.dashboard.dto.*;
import com.commerce_pro_backend.inventory.entity.Inventory;
import com.commerce_pro_backend.inventory.repository.InventoryRepository;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;

    // ── Summary ─────────────────────────────────────────────────────────────────

    public DashboardSummaryDTO getSummary(String period) {
        LocalDateTime currentStart = periodStart(period);
        LocalDateTime previousStart = previousPeriodStart(period, currentStart);

        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        long totalOrders = orderRepository.count();
        long totalCustomers = customerRepository.count();
        long totalProducts = productRepository.count();
        BigDecimal averageOrderValue = orderRepository.averageOrderValue();

        // Growth calculations
        long currentOrders = orderRepository.countOrdersSince(currentStart);
        long previousOrders = orderRepository.countOrdersSince(previousStart) - currentOrders;
        double orderGrowth = computeGrowth(currentOrders, previousOrders);

        long currentCustomers = customerRepository.countCreatedSince(currentStart);
        long previousCustomers = customerRepository.countCreatedSince(previousStart) - currentCustomers;
        double customerGrowth = computeGrowth(currentCustomers, previousCustomers);

        BigDecimal currentRevenue = orderRepository.sumRevenueSince(currentStart);
        BigDecimal previousRevenue = orderRepository.sumRevenueSince(previousStart).subtract(currentRevenue);
        double revenueGrowth = computeGrowthDecimal(currentRevenue, previousRevenue);

        return DashboardSummaryDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .averageOrderValue(averageOrderValue)
                .revenueGrowthPercent(revenueGrowth)
                .orderGrowthPercent(orderGrowth)
                .customerGrowthPercent(customerGrowth)
                .build();
    }

    // ── KPIs ────────────────────────────────────────────────────────────────────

    public List<KpiCardDTO> getKpis() {
        List<KpiCardDTO> kpis = new ArrayList<>();

        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        long totalOrders = orderRepository.count();
        long totalCustomers = customerRepository.count();
        BigDecimal aov = orderRepository.averageOrderValue();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long lowStockItems = inventoryRepository.countByStatus(Inventory.StockStatus.LOW_STOCK);

        kpis.add(KpiCardDTO.builder()
                .title("Total Revenue")
                .value(formatCurrency(totalRevenue))
                .change(0)
                .changePercent(0)
                .trend(KpiCardDTO.Trend.STABLE)
                .period("All time")
                .build());

        kpis.add(KpiCardDTO.builder()
                .title("Total Orders")
                .value(String.valueOf(totalOrders))
                .change(0)
                .changePercent(0)
                .trend(KpiCardDTO.Trend.STABLE)
                .period("All time")
                .build());

        kpis.add(KpiCardDTO.builder()
                .title("Total Customers")
                .value(String.valueOf(totalCustomers))
                .change(0)
                .changePercent(0)
                .trend(KpiCardDTO.Trend.STABLE)
                .period("All time")
                .build());

        kpis.add(KpiCardDTO.builder()
                .title("Average Order Value")
                .value(formatCurrency(aov))
                .change(0)
                .changePercent(0)
                .trend(KpiCardDTO.Trend.STABLE)
                .period("All time")
                .build());

        kpis.add(KpiCardDTO.builder()
                .title("Pending Orders")
                .value(String.valueOf(pendingOrders))
                .change(0)
                .changePercent(0)
                .trend(pendingOrders > 0 ? KpiCardDTO.Trend.UP : KpiCardDTO.Trend.STABLE)
                .period("Current")
                .build());

        kpis.add(KpiCardDTO.builder()
                .title("Low Stock Items")
                .value(String.valueOf(lowStockItems))
                .change(0)
                .changePercent(0)
                .trend(lowStockItems > 0 ? KpiCardDTO.Trend.DOWN : KpiCardDTO.Trend.STABLE)
                .period("Current")
                .build());

        return kpis;
    }

    // ── Top Products ────────────────────────────────────────────────────────────

    public List<TopProductDTO> getTopProducts(int limit) {
        List<Product> topProducts = productRepository.findTopSelling(PageRequest.of(0, limit));
        return topProducts.stream()
                .map(p -> TopProductDTO.builder()
                        .productId(p.getId())
                        .productName(p.getName())
                        .sku(p.getSku())
                        .imageUrl(p.getImageUrl())
                        .totalSold(p.getSalesCount() != null ? p.getSalesCount() : 0)
                        .totalRevenue(p.getRevenue() != null ? p.getRevenue() : BigDecimal.ZERO)
                        .build())
                .toList();
    }

    // ── Recent Orders ───────────────────────────────────────────────────────────

    public List<RecentOrderDTO> getRecentOrders(int limit) {
        Page<Order> page = orderRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));

        return page.getContent().stream()
                .map(o -> RecentOrderDTO.builder()
                        .orderId(o.getId())
                        .orderNumber(o.getOrderNumber())
                        .customerName(o.getCustomerName())
                        .total(o.getTotalAmount())
                        .status(o.getStatus().name())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();
    }

    // ── Inventory Status ────────────────────────────────────────────────────────

    public InventoryStatusDTO getInventoryStatus() {
        long totalItems = inventoryRepository.count();
        long lowStockCount = inventoryRepository.countByStatus(Inventory.StockStatus.LOW_STOCK);
        long outOfStockCount = inventoryRepository.countByStatus(Inventory.StockStatus.OUT_OF_STOCK);
        BigDecimal totalValue = inventoryRepository.getTotalInventoryValue();

        return InventoryStatusDTO.builder()
                .totalItems(totalItems)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .totalValue(totalValue != null ? totalValue : BigDecimal.ZERO)
                .build();
    }

    // ── Sales Chart ─────────────────────────────────────────────────────────────

    public SalesChartDTO getSalesChart(String period) {
        int days = parseDays(period);
        LocalDate today = LocalDate.now();

        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenueData = new ArrayList<>();
        List<BigDecimal> ordersData = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            labels.add(date.format(formatter));

            List<OrderStatus> excluded = List.of(OrderStatus.CANCELLED);
            BigDecimal dayRevenue = orderRepository.sumRevenueInRange(excluded, dayStart, dayEnd);
            revenueData.add(dayRevenue != null ? dayRevenue : BigDecimal.ZERO);

            long dayOrders = orderRepository.countOrdersSince(dayStart) - orderRepository.countOrdersSince(dayEnd);
            ordersData.add(BigDecimal.valueOf(Math.max(0, dayOrders)));
        }

        List<SalesChartDTO.Dataset> datasets = new ArrayList<>();
        datasets.add(SalesChartDTO.Dataset.builder()
                .label("Revenue")
                .data(revenueData)
                .build());
        datasets.add(SalesChartDTO.Dataset.builder()
                .label("Orders")
                .data(ordersData)
                .build());

        return SalesChartDTO.builder()
                .labels(labels)
                .datasets(datasets)
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private int parseDays(String period) {
        if (period == null) return 7;
        return switch (period.toLowerCase()) {
            case "30d" -> 30;
            case "90d" -> 90;
            default -> 7;
        };
    }

    private LocalDateTime periodStart(String period) {
        int days = parseDays(period);
        return LocalDate.now().minusDays(days).atStartOfDay();
    }

    private LocalDateTime previousPeriodStart(String period, LocalDateTime currentStart) {
        int days = parseDays(period);
        return currentStart.minusDays(days);
    }

    private double computeGrowth(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((double) (current - previous) / previous) * 100.0;
    }

    private double computeGrowthDecimal(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
