package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.enums.ExpenseStatus;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.*;
import com.commerce_pro_backend.finance.specification.ExpenseSpecification;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository         expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final FinancialPeriodService    periodService;
    private final InvoiceNumberService      invoiceNumberService;
    private final JournalEntryService       journalEntryService;
    private final FinanceMapper             mapper;
    private final AuditService             auditService;
    private final CurrentUserService       currentUserService;

    public PageResponse<ExpenseSummaryDTO> listExpenses(
            String search, ExpenseStatus status, String categoryId,
            String periodId, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Expense> spec = ExpenseSpecification.withFilters(search, status, categoryId, periodId, from, to);
        Page<Expense> page = expenseRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(mapper::toExpenseSummaryDTO).toList());
    }

    public ExpenseDTO getById(String id) { return mapper.toExpenseDTO(findOrThrow(id)); }

    public ExpenseStatsDTO getStats() {
        LocalDate today = LocalDate.now();
        LocalDate mtdStart = today.withDayOfMonth(1);
        LocalDate ytdStart = today.withDayOfYear(1);

        BigDecimal mtd = expenseRepository.sumExpensesInRange(mtdStart, today);
        BigDecimal ytd = expenseRepository.sumExpensesInRange(ytdStart, today);

        List<Object[]> catRaw = expenseRepository.sumByCategory(ytdStart, today);
        Map<String, BigDecimal> byCategory = catRaw.stream()
                .collect(Collectors.toMap(r -> (String) r[1], r -> (BigDecimal) r[2]));

        return ExpenseStatsDTO.builder()
                .totalMTD(mtd).totalYTD(ytd)
                .pendingCount(expenseRepository.countByStatus(ExpenseStatus.PENDING))
                .pendingAmount(BigDecimal.ZERO) // resolved from query if needed
                .byCategory(byCategory).build();
    }

    public List<ExpenseCategoryDTO> getCategories() {
        return mapper.toExpenseCategoryDTOList(categoryRepository.findByIsActiveTrue());
    }

    @Transactional
    public ExpenseDTO createExpense(CreateExpenseRequest req) {
        String actor = currentUserService.getCurrentUserId();
        ExpenseCategory cat = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> ApiException.notFound("ExpenseCategory", req.getCategoryId()));
        FinancialPeriod period = periodService.resolvePeriodForDate(req.getExpenseDate());

        Expense expense = Expense.builder()
                .expenseRef(invoiceNumberService.generateExpenseRef())
                .title(req.getTitle()).description(null).category(cat).period(period)
                .vendorId(req.getVendorId()).expenseDate(req.getExpenseDate())
                .status(ExpenseStatus.PENDING)
                .amount(req.getAmount())
                .taxAmount(req.getTaxAmount() != null ? req.getTaxAmount() : BigDecimal.ZERO)
                .currency(req.getCurrency() != null ? req.getCurrency() : "USD")
                .paymentMethod(req.getPaymentMethod()).notes(null)
                .isRecurring(req.getIsRecurring() != null ? req.getIsRecurring() : false)
                .recurrenceInterval(req.getRecurrenceInterval())
                .createdBy(actor).updatedBy(actor).build();
        expense = expenseRepository.save(expense);
        auditService.log(actor, AuditAction.FINANCE_EXPENSE_CREATED, "Expense", expense.getId(),
                expense.getExpenseRef(), null, null, null, true);
        return mapper.toExpenseDTO(expense);
    }

    @Transactional
    public ExpenseDTO approve(String id) {
        Expense expense = findOrThrow(id);
        if (!expense.isApprovable()) throw ApiException.badRequest("Cannot approve expense with status: " + expense.getStatus());
        String actor = currentUserService.getCurrentUserId();
        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprovedAt(LocalDateTime.now());
        expense.setApprovedBy(actor);
        expense.setUpdatedBy(actor);
        expense = expenseRepository.save(expense);
        auditService.log(actor, AuditAction.FINANCE_EXPENSE_APPROVED, "Expense", expense.getId(),
                expense.getExpenseRef(), null, "PENDING", "APPROVED", true);
        return mapper.toExpenseDTO(expense);
    }

    @Transactional
    public ExpenseDTO reject(String id, String reason) {
        Expense expense = findOrThrow(id);
        if (!expense.isApprovable()) throw ApiException.badRequest("Cannot reject expense with status: " + expense.getStatus());
        String actor = currentUserService.getCurrentUserId();
        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setRejectionReason(reason);
        expense.setUpdatedBy(actor);
        expense = expenseRepository.save(expense);
        auditService.log(actor, AuditAction.FINANCE_EXPENSE_REJECTED, "Expense", expense.getId(),
                expense.getExpenseRef(), null, "PENDING", "REJECTED", true);
        return mapper.toExpenseDTO(expense);
    }

    @Transactional
    public ExpenseDTO markPaid(String id) {
        Expense expense = findOrThrow(id);
        if (expense.getStatus() != ExpenseStatus.APPROVED) throw ApiException.badRequest("Only APPROVED expenses can be paid");
        String actor = currentUserService.getCurrentUserId();
        expense.setStatus(ExpenseStatus.PAID);
        expense.setPaidAt(LocalDateTime.now());
        expense.setUpdatedBy(actor);
        expense = expenseRepository.save(expense);
        journalEntryService.postExpensePaid(expense);
        auditService.log(actor, AuditAction.FINANCE_EXPENSE_PAID, "Expense", expense.getId(),
                expense.getExpenseRef(), null, "APPROVED", "PAID", true);
        return mapper.toExpenseDTO(expense);
    }

    @Transactional
    public ExpenseCategoryDTO createCategory(CreateExpenseCategoryRequest req) {
        if (categoryRepository.findByCategoryCode(req.getCategoryCode()).isPresent()) {
            throw ApiException.conflict("Category code '" + req.getCategoryCode() + "' already exists");
        }
        String actor = currentUserService.getCurrentUserId();
        ExpenseCategory cat = ExpenseCategory.builder()
                .categoryCode(req.getCategoryCode()).name(req.getName())
                .description(req.getDescription()).parentId(req.getParentId())
                .glAccountCode(req.getGlAccountCode()).isActive(true)
                .budgetDefault(req.getBudgetDefault()).createdBy(actor).build();
        return mapper.toExpenseCategoryDTO(categoryRepository.save(cat));
    }

    @Transactional
    public ExpenseCategoryDTO updateCategory(String id, CreateExpenseCategoryRequest req) {
        ExpenseCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ExpenseCategory", id));
        cat.setName(req.getName());
        if (req.getDescription() != null) cat.setDescription(req.getDescription());
        if (req.getGlAccountCode() != null) cat.setGlAccountCode(req.getGlAccountCode());
        if (req.getBudgetDefault() != null) cat.setBudgetDefault(req.getBudgetDefault());
        return mapper.toExpenseCategoryDTO(categoryRepository.save(cat));
    }

    private Expense findOrThrow(String id) {
        return expenseRepository.findById(id).orElseThrow(() -> ApiException.notFound("Expense", id));
    }
}
