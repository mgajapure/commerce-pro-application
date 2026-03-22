package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.*;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxReportService {

    private final TaxReportRepository         taxReportRepository;
    private final TaxJurisdictionRepository   jurisdictionRepository;
    private final FinancialPeriodService       periodService;
    private final InvoiceNumberService         invoiceNumberService;
    private final OrderRepository              orderRepository;
    private final FinanceMapper                mapper;
    private final AuditService                 auditService;
    private final CurrentUserService           currentUserService;
    private final ObjectMapper                 objectMapper;

    private static final List<OrderStatus> EXCLUDED_STATUSES =
            List.of(OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.DRAFT);

    public PageResponse<TaxReportDTO> listReports(Pageable pageable) {
        Page<TaxReport> page = taxReportRepository.findAll(pageable);
        return PageResponse.from(page, page.getContent().stream().map(mapper::toTaxReportDTO).toList());
    }

    public TaxReportDTO getById(String id) {
        return mapper.toTaxReportDTO(findOrThrow(id));
    }

    @Transactional
    public TaxReportDTO generateReport(GenerateTaxReportRequest req) {
        FinancialPeriod period = periodService.findOrThrow(req.getPeriodId());
        String actor = currentUserService.getCurrentUserId();
        String jName = null;
        if (req.getJurisdictionId() != null) {
            jName = jurisdictionRepository.findById(req.getJurisdictionId())
                    .map(TaxJurisdiction::getName).orElse(null);
        }

        // Aggregate tax data from orders in the period's date range
        LocalDateTime from = period.getStartDate().atStartOfDay();
        LocalDateTime to   = period.getEndDate().atTime(23, 59, 59);

        BigDecimal totalSales   = orderRepository.sumRevenueInRange(EXCLUDED_STATUSES, from, to);
        BigDecimal taxCollected = orderRepository.sumTaxInRange(EXCLUDED_STATUSES, from, to);
        long       orderCount   = orderRepository.countByStatusInAndCreatedAtBetween(
                List.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERED, OrderStatus.SHIPPED,
                        OrderStatus.FULFILLED, OrderStatus.CLOSED),
                from, to);

        // Simple breakdown — in production this would be per-rate from OrderItem.taxRate
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put("collected", taxCollected);

        String breakdownJson = "{}";
        try { breakdownJson = objectMapper.writeValueAsString(breakdown); } catch (Exception ignored) {}

        TaxReport report = TaxReport.builder()
                .reportRef(invoiceNumberService.generateTaxReportRef())
                .period(period).reportType(req.getReportType())
                .jurisdictionId(req.getJurisdictionId()).jurisdictionName(jName)
                .periodStart(from).periodEnd(to).status("DRAFT")
                .totalSales(totalSales).taxableSales(totalSales).exemptSales(BigDecimal.ZERO)
                .taxCollected(taxCollected).taxPaidToVendors(BigDecimal.ZERO)
                .netTaxPayable(taxCollected).orderCount(orderCount)
                .taxBreakdown(breakdownJson)
                .generatedAt(LocalDateTime.now()).generatedBy(actor)
                .notes(req.getNotes())
                .build();
        report = taxReportRepository.save(report);
        auditService.log(actor, AuditAction.FINANCE_TAX_REPORT_GENERATED, "TaxReport", report.getId(),
                report.getReportRef(), null, null, null, true);
        return mapper.toTaxReportDTO(report);
    }

    @Transactional
    public TaxReportDTO fileReport(String id) {
        TaxReport report = findOrThrow(id);
        String actor = currentUserService.getCurrentUserId();
        report.setStatus("FILED");
        report.setFiledAt(LocalDateTime.now());
        report.setFiledBy(actor);
        report = taxReportRepository.save(report);
        auditService.log(actor, AuditAction.FINANCE_TAX_REPORT_FILED, "TaxReport", report.getId(),
                report.getReportRef(), null, "DRAFT", "FILED", true);
        return mapper.toTaxReportDTO(report);
    }

    private TaxReport findOrThrow(String id) {
        return taxReportRepository.findById(id).orElseThrow(() -> ApiException.notFound("TaxReport", id));
    }
}
