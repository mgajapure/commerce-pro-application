package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.CustomerReportDTO;
import com.commerce_pro_backend.analytics.dto.CustomerReportDTO.*;
import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.enums.CustomerStatus;
import com.commerce_pro_backend.customer.repository.CustomerRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomerReportService {

    private final CustomerRepository customerRepository;
    private final OrderRepository    orderRepository;

    private static final int CHURN_DAYS = 90;

    // ── LTV Report ───────────────────────────────────────────────────────────

    public CustomerReportDTO getLtvReport(ReportFilterDTO filter) {
        LocalDateTime[] range = DateRangeHelper.resolve(filter);
        List<Customer> customers = customerRepository.findAll();

        long newInPeriod = customers.stream()
                .filter(c -> c.getCreatedAt() != null
                        && !c.getCreatedAt().isBefore(range[0])
                        && !c.getCreatedAt().isAfter(range[1]))
                .count();

        long active  = customers.stream().filter(c -> c.getStatus() == CustomerStatus.ACTIVE).count();
        long churned = customers.stream().filter(this::isChurned).count();

        BigDecimal avgLtv = customers.isEmpty() ? BigDecimal.ZERO
                : customers.stream()
                    .map(c -> c.getLifetimeSpend() != null ? c.getLifetimeSpend() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(customers.size()), 4, RoundingMode.HALF_UP);

        // Tier breakdown
        Map<String, Long> byTier = customers.stream()
                .collect(Collectors.groupingBy(c -> c.getTier().name(), Collectors.counting()));

        // Acquisition source breakdown
        Map<String, Long> bySource = customers.stream()
                .filter(c -> c.getAcquisitionSource() != null)
                .collect(Collectors.groupingBy(Customer::getAcquisitionSource, Collectors.counting()));

        // LTV rows
        List<CustomerLtvRowDTO> rows = customers.stream()
                .sorted(Comparator.comparing(
                        c -> c.getLifetimeSpend() != null ? c.getLifetimeSpend() : BigDecimal.ZERO,
                        Comparator.reverseOrder()))
                .map(c -> {
                    int daysSinceLast = c.getLastOrderAt() != null
                            ? (int) ChronoUnit.DAYS.between(c.getLastOrderAt(), LocalDateTime.now())
                            : -1;
                    String segment = computeSegment(c, daysSinceLast);
                    return CustomerLtvRowDTO.builder()
                            .customerId(c.getId())
                            .customerName(c.getFirstName() + " " + c.getLastName())
                            .email(c.getEmail())
                            .tier(c.getTier().name())
                            .status(c.getStatus().name())
                            .totalOrders(c.getTotalOrders())
                            .lifetimeSpend(c.getLifetimeSpend())
                            .averageOrderValue(c.getAverageOrderValue())
                            .firstOrderAt(fmt(c.getCreatedAt()))
                            .lastOrderAt(fmt(c.getLastOrderAt()))
                            .daysSinceLastOrder(daysSinceLast >= 0 ? daysSinceLast : null)
                            .loyaltyPoints(c.getLoyaltyPoints())
                            .acquisitionSource(c.getAcquisitionSource())
                            .segment(segment)
                            .build();
                }).toList();

        return CustomerReportDTO.builder()
                .totalCustomers((long) customers.size())
                .activeCustomers(active)
                .newCustomersInPeriod(newInPeriod)
                .churned(churned)
                .averageLtv(avgLtv)
                .medianLtv(computeMedian(customers))
                .customersByTier(byTier)
                .customersByAcquisitionSource(bySource)
                .ltvRows(rows)
                .build();
    }

    // ── Cohort Report ─────────────────────────────────────────────────────────

    public CustomerReportDTO getCohortReport(ReportFilterDTO filter) {
        LocalDateTime[] range = DateRangeHelper.resolve(filter);

        // Group customers by acquisition month
        List<Customer> customers = customerRepository.findByCreatedAtBetween(range[0], range[1]);
        Map<String, List<Customer>> cohorts = customers.stream()
                .filter(c -> c.getCreatedAt() != null)
                .collect(Collectors.groupingBy(c ->
                        c.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"))));

        List<CohortRowDTO> cohortRows = new ArrayList<>();
        for (Map.Entry<String, List<Customer>> entry : new TreeMap<>(cohorts).entrySet()) {
            String month = entry.getKey();
            List<Customer> cohortCustomers = entry.getValue();
            int size = cohortCustomers.size();

            // For each subsequent month, count how many placed at least one order
            List<BigDecimal> retention = new ArrayList<>();
            for (int m = 0; m <= 11; m++) {
                int monthOffset = m;
                long retained = cohortCustomers.stream()
                        .filter(c -> hadOrderInMonth(c.getId(), month, monthOffset))
                        .count();
                retention.add(size > 0
                        ? BigDecimal.valueOf(retained).divide(BigDecimal.valueOf(size), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO);
            }
            cohortRows.add(CohortRowDTO.builder()
                    .cohortMonth(month)
                    .cohortSize(size)
                    .retentionByMonth(retention)
                    .build());
        }

        return CustomerReportDTO.builder()
                .cohortRows(cohortRows)
                .build();
    }

    // ── Churn Report ──────────────────────────────────────────────────────────

    public CustomerReportDTO getChurnReport(ReportFilterDTO filter) {
        List<Customer> atRisk = customerRepository.findAll().stream()
                .filter(c -> c.getStatus() == CustomerStatus.ACTIVE && c.getTotalOrders() > 0)
                .filter(c -> {
                    if (c.getLastOrderAt() == null) return false;
                    long days = ChronoUnit.DAYS.between(c.getLastOrderAt(), LocalDateTime.now());
                    return days >= 45; // at risk if no order in 45+ days
                })
                .toList();

        List<ChurnRowDTO> rows = atRisk.stream()
                .sorted(Comparator.comparing(
                        c -> c.getLifetimeSpend() != null ? c.getLifetimeSpend() : BigDecimal.ZERO,
                        Comparator.reverseOrder()))
                .map(c -> {
                    long days = ChronoUnit.DAYS.between(c.getLastOrderAt(), LocalDateTime.now());
                    // Simple churn risk score: 0-100 based on days since last order (90 days = 100)
                    BigDecimal risk = BigDecimal.valueOf(Math.min(100, days * 100 / CHURN_DAYS));
                    return ChurnRowDTO.builder()
                            .customerId(c.getId())
                            .customerName(c.getFirstName() + " " + c.getLastName())
                            .email(c.getEmail())
                            .tier(c.getTier().name())
                            .lifetimeSpend(c.getLifetimeSpend())
                            .totalOrders(c.getTotalOrders())
                            .lastOrderAt(fmt(c.getLastOrderAt()))
                            .daysSinceLastOrder((int) days)
                            .churnRisk(risk)
                            .build();
                }).toList();

        return CustomerReportDTO.builder()
                .churnRows(rows)
                .churned((long) rows.size())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isChurned(Customer c) {
        if (c.getLastOrderAt() == null) return false;
        return ChronoUnit.DAYS.between(c.getLastOrderAt(), LocalDateTime.now()) > CHURN_DAYS;
    }

    private String computeSegment(Customer c, int daysSinceLast) {
        if (c.getLifetimeSpend() != null && c.getLifetimeSpend().compareTo(new BigDecimal("5000")) >= 0) return "High Value";
        if (daysSinceLast > CHURN_DAYS) return "Churned";
        if (daysSinceLast > 45) return "At Risk";
        if (c.getTotalOrders() <= 1) return "New";
        return "Active";
    }

    private BigDecimal computeMedian(List<Customer> customers) {
        if (customers.isEmpty()) return BigDecimal.ZERO;
        List<BigDecimal> sorted = customers.stream()
                .map(c -> c.getLifetimeSpend() != null ? c.getLifetimeSpend() : BigDecimal.ZERO)
                .sorted()
                .toList();
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 0
                ? sorted.get(mid - 1).add(sorted.get(mid)).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)
                : sorted.get(mid);
    }

    /** Simple check: did customer place any order in the given cohort month + offset? */
    private boolean hadOrderInMonth(String customerId, String cohortMonth, int monthOffset) {
        // We rely on Order.createdAt between first and last of target month
        String[] parts = cohortMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        java.time.YearMonth ym = java.time.YearMonth.of(year, month).plusMonths(monthOffset);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to   = ym.atEndOfMonth().atTime(23, 59, 59);
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream().anyMatch(o ->
                o.getCreatedAt() != null
                && !o.getCreatedAt().isBefore(from)
                && !o.getCreatedAt().isAfter(to)
                && o.getStatus() != OrderStatus.CANCELLED
                && o.getStatus() != OrderStatus.DRAFT);
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
