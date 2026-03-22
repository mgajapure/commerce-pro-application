package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.enums.BudgetStatus;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.*;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository       budgetRepository;
    private final FinancialPeriodService periodService;
    private final ExpenseRepository      expenseRepository;
    private final OrderRepository        orderRepository;
    private final FinanceMapper          mapper;
    private final AuditService          auditService;
    private final CurrentUserService    currentUserService;

    private static final List<OrderStatus> EXCLUDED =
            List.of(OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.DRAFT);

    public PageResponse<BudgetDTO> listBudgets(Integer fiscalYear, Pageable pageable) {
        Page<Budget> page = fiscalYear != null
                ? budgetRepository.findByFiscalYear(fiscalYear, pageable)
                : budgetRepository.findAll(pageable);
        return PageResponse.from(page, page.getContent().stream()
                .map(b -> mapper.toBudgetDTO(b, BigDecimal.ZERO, BigDecimal.ZERO)).toList());
    }

    public BudgetDTO getById(String id) {
        Budget b = findOrThrow(id);
        BigDecimal revenueActual = computeRevenueActual(b);
        BigDecimal expenseActual = computeExpenseActual(b);
        return mapper.toBudgetDTO(b, revenueActual, expenseActual);
    }

    @Transactional
    public BudgetDTO createBudget(CreateBudgetRequest req) {
        String actor = currentUserService.getCurrentUserId();
        FinancialPeriod period = req.getPeriodId() != null ? periodService.findOrThrow(req.getPeriodId()) : null;

        Budget budget = Budget.builder()
                .name(req.getName()).description(req.getDescription())
                .fiscalYear(req.getFiscalYear()).period(period)
                .status(BudgetStatus.DRAFT)
                .currency(req.getCurrency() != null ? req.getCurrency() : "USD")
                .notes(null).createdBy(actor).updatedBy(actor).build();

        List<BudgetLine> lines = new ArrayList<>();
        BigDecimal totalRev = BigDecimal.ZERO, totalExp = BigDecimal.ZERO;
        int sort = 0;
        for (BudgetLineRequest lr : req.getLines()) {
            BudgetLine line = BudgetLine.builder()
                    .budget(budget).lineName(lr.getLineName())
                    .categoryId(lr.getCategoryId()).accountCode(lr.getAccountCode())
                    .lineType(lr.getLineType() != null ? lr.getLineType() : "EXPENSE")
                    .budgetedAmount(lr.getBudgetedAmount()).sortOrder(sort++).notes(lr.getNotes()).build();
            lines.add(line);
            if ("REVENUE".equalsIgnoreCase(line.getLineType())) totalRev = totalRev.add(lr.getBudgetedAmount());
            else totalExp = totalExp.add(lr.getBudgetedAmount());
        }
        budget.setLines(lines);
        budget.setTotalRevenueBudget(totalRev);
        budget.setTotalExpenseBudget(totalExp);
        budget = budgetRepository.save(budget);
        auditService.log(actor, AuditAction.FINANCE_BUDGET_CREATED, "Budget", budget.getId(),
                budget.getName(), null, null, null, true);
        return mapper.toBudgetDTO(budget, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional
    public BudgetDTO approveBudget(String id) {
        Budget budget = findOrThrow(id);
        if (budget.getStatus() != BudgetStatus.DRAFT && budget.getStatus() != BudgetStatus.PENDING_APPROVAL) {
            throw ApiException.badRequest("Budget status must be DRAFT or PENDING_APPROVAL to approve");
        }
        String actor = currentUserService.getCurrentUserId();
        budget.setStatus(BudgetStatus.APPROVED);
        budget.setApprovedAt(LocalDateTime.now());
        budget.setApprovedBy(actor);
        budget.setUpdatedBy(actor);
        budget = budgetRepository.save(budget);
        auditService.log(actor, AuditAction.FINANCE_BUDGET_APPROVED, "Budget", budget.getId(),
                budget.getName(), null, "DRAFT", "APPROVED", true);
        return mapper.toBudgetDTO(budget, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public BudgetDTO getVarianceReport(String id) {
        Budget budget = findOrThrow(id);
        return mapper.toBudgetDTO(budget, computeRevenueActual(budget), computeExpenseActual(budget));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private BigDecimal computeRevenueActual(Budget b) {
        if (b.getPeriod() == null) return BigDecimal.ZERO;
        try {
            LocalDateTime from = b.getPeriod().getStartDate().atStartOfDay();
            LocalDateTime to   = b.getPeriod().getEndDate().atTime(23, 59, 59);
            return orderRepository.sumRevenueInRange(EXCLUDED, from, to);
        } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private BigDecimal computeExpenseActual(Budget b) {
        if (b.getPeriod() == null) return BigDecimal.ZERO;
        try {
            return expenseRepository.sumByPeriod(b.getPeriod().getId());
        } catch (Exception e) { return BigDecimal.ZERO; }
    }

    Budget findOrThrow(String id) {
        return budgetRepository.findById(id).orElseThrow(() -> ApiException.notFound("Budget", id));
    }
}
