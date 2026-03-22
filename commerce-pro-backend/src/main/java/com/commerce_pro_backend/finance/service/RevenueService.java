package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.repository.*;
import com.commerce_pro_backend.order.enums.OrderStatus;
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

@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueService {

    private final OrderRepository              orderRepository;
    private final CustomerInvoiceRepository    customerInvoiceRepository;
    private final VendorInvoiceRepository      vendorInvoiceRepository;
    private final ExpenseRepository            expenseRepository;

    private static final List<OrderStatus> EXCLUDED =
            List.of(OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.DRAFT);

    public RevenueOverviewDTO getOverview() {
        LocalDate today    = LocalDate.now();
        LocalDate mtdStart = today.withDayOfMonth(1);
        LocalDate ytdStart = today.withDayOfYear(1);

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd   = today.atTime(23, 59, 59);
        LocalDateTime mtdDT      = mtdStart.atStartOfDay();
        LocalDateTime ytdDT      = ytdStart.atStartOfDay();
        LocalDateTime now        = LocalDateTime.now();

        BigDecimal grossToday = orderRepository.sumRevenueInRange(EXCLUDED, todayStart, todayEnd);
        BigDecimal grossMTD   = orderRepository.sumRevenueInRange(EXCLUDED, mtdDT, now);
        BigDecimal grossYTD   = orderRepository.sumRevenueInRange(EXCLUDED, ytdDT, now);
        BigDecimal taxMTD     = orderRepository.sumTaxInRange(EXCLUDED, mtdDT, now);
        BigDecimal refundsMTD = BigDecimal.ZERO; // from order.refundedAmount
        long ordersToday      = orderRepository.countOrdersSince(todayStart);
        long ordersMTD        = orderRepository.countOrdersSince(mtdDT);
        BigDecimal aov        = orderRepository.averageOrderValue();

        // AR / AP
        BigDecimal outstandingAR = customerInvoiceRepository.sumTotalOutstanding();
        BigDecimal overdueAR     = customerInvoiceRepository.sumOverdueAmount(today);
        BigDecimal outstandingAP = vendorInvoiceRepository.sumTotalOutstanding();
        BigDecimal overdueAP     = vendorInvoiceRepository.sumOverdueAmount(today);

        // Expenses
        BigDecimal expMTD = expenseRepository.sumExpensesInRange(mtdStart, today);

        // Gross Profit rough estimate — COGS not available without OrderItem join (used in P&L)
        BigDecimal grossProfit = grossMTD.subtract(expMTD);
        BigDecimal margin = grossMTD.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(grossMTD, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return RevenueOverviewDTO.builder()
                .grossRevenueToday(grossToday).grossRevenueMTD(grossMTD).grossRevenueYTD(grossYTD)
                .netRevenueMTD(grossMTD).netRevenueYTD(grossYTD)
                .totalRefundsMTD(refundsMTD).totalTaxCollectedMTD(taxMTD)
                .totalShippingRevenueMTD(BigDecimal.ZERO)
                .ordersToday(ordersToday).ordersMTD(ordersMTD).averageOrderValue(aov)
                .revenueGrowthPct(BigDecimal.ZERO)
                .outstandingAR(outstandingAR).overdueAR(overdueAR)
                .outstandingAP(outstandingAP).overdueAP(overdueAP)
                .totalExpensesMTD(expMTD).grossProfitMTD(grossProfit).grossMarginPct(margin)
                .trend(List.of())
                .build();
    }
}
