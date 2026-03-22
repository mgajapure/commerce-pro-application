package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.Budget;
import com.commerce_pro_backend.finance.enums.BudgetStatus;
import com.commerce_pro_backend.finance.repository.*;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.repository.OrderItemRepository;
import com.commerce_pro_backend.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfitLossService {

    private final OrderRepository      orderRepository;
    private final OrderItemRepository  orderItemRepository;
    private final ExpenseRepository    expenseRepository;
    private final BudgetRepository     budgetRepository;

    private static final List<OrderStatus> EXCLUDED =
            List.of(OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.DRAFT);

    public ProfitLossDTO getPnL(LocalDate from, LocalDate to, String periodLabel) {
        LocalDateTime fromDT = from.atStartOfDay();
        LocalDateTime toDT   = to.atTime(23, 59, 59);

        // Revenue
        BigDecimal grossRevenue = orderRepository.sumRevenueInRange(EXCLUDED, fromDT, toDT);
        BigDecimal totalTax     = orderRepository.sumTaxInRange(EXCLUDED, fromDT, toDT);
        BigDecimal salesReturns = BigDecimal.ZERO; // refundedAmount would need separate query
        BigDecimal netRevenue   = grossRevenue.subtract(salesReturns);

        // COGS — from order items cost price
        List<String> orderIds = orderRepository.findByCreatedAtBetweenAndStatusNotIn(fromDT, toDT, EXCLUDED)
                .stream().map(o -> o.getId()).toList();
        BigDecimal cogs = orderIds.isEmpty() ? BigDecimal.ZERO :
                orderItemRepository.findByOrderIds(orderIds).stream()
                .filter(i -> i.getCostPrice() != null)
                .map(i -> i.getCostPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossProfit = netRevenue.subtract(cogs);
        BigDecimal grossMargin = netRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(netRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Operating Expenses
        BigDecimal totalExpenses = expenseRepository.sumExpensesInRange(from, to);
        List<Object[]> catRaw    = expenseRepository.sumByCategory(from, to);
        Map<String, BigDecimal> byCategory = catRaw.stream()
                .collect(Collectors.toMap(r -> (String) r[1], r -> (BigDecimal) r[2]));

        BigDecimal ebitda      = grossProfit.subtract(totalExpenses);
        BigDecimal netIncome   = ebitda.subtract(totalTax);
        BigDecimal netMargin   = netRevenue.compareTo(BigDecimal.ZERO) > 0
                ? netIncome.divide(netRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Budget comparison
        int year = from.getYear();
        BigDecimal budgetedNet = BigDecimal.ZERO;
        var budgetOpt = budgetRepository.findByFiscalYearAndStatus(year, BudgetStatus.APPROVED);
        if (budgetOpt.isPresent()) {
            Budget b = budgetOpt.get();
            budgetedNet = (b.getTotalRevenueBudget() != null ? b.getTotalRevenueBudget() : BigDecimal.ZERO)
                    .subtract(b.getTotalExpenseBudget() != null ? b.getTotalExpenseBudget() : BigDecimal.ZERO);
        }

        return ProfitLossDTO.builder()
                .periodLabel(periodLabel != null ? periodLabel : from + " to " + to)
                .startDate(from).endDate(to)
                .grossRevenue(grossRevenue).salesReturns(salesReturns).netRevenue(netRevenue)
                .cogs(cogs).grossProfit(grossProfit).grossMarginPct(grossMargin)
                .totalOperatingExpenses(totalExpenses).expenseByCategory(byCategory)
                .totalShipping(BigDecimal.ZERO).gatewayFees(BigDecimal.ZERO).chargebacks(BigDecimal.ZERO)
                .ebitda(ebitda).totalTax(totalTax).netIncome(netIncome).netMarginPct(netMargin)
                .previousPeriodNetIncome(BigDecimal.ZERO).netIncomeChangePct(BigDecimal.ZERO)
                .budgetedNetIncome(budgetedNet).budgetVariance(netIncome.subtract(budgetedNet))
                .build();
    }

    public ProfitLossDTO getYTD() {
        LocalDate today = LocalDate.now();
        return getPnL(today.withDayOfYear(1), today, "Year to Date " + today.getYear());
    }

    public ProfitLossDTO getMonthToDate() {
        LocalDate today = LocalDate.now();
        return getPnL(today.withDayOfMonth(1), today, "MTD " + today.getMonth() + " " + today.getYear());
    }
}
