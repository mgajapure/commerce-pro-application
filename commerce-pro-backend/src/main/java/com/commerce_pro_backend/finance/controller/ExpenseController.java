package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.*;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.enums.ExpenseStatus;
import com.commerce_pro_backend.finance.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/v1/finance/expenses")
@RequiredArgsConstructor @Tag(name = "Finance - Expenses", description = "Operating expense tracking and approval workflow")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:expense:read')")
    @Operation(summary = "List expenses")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseSummaryDTO>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String periodId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "expenseDate") @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved",
                expenseService.listExpenses(search, status, categoryId, periodId, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:expense:read')")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<ApiResponse<ExpenseDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Expense retrieved", expenseService.getById(id)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('finance:expense:read')")
    @Operation(summary = "Expense statistics by category")
    public ResponseEntity<ApiResponse<ExpenseStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Expense stats retrieved", expenseService.getStats()));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('finance:expense:read')")
    @Operation(summary = "List expense categories")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryDTO>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", expenseService.getCategories()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:expense:create')")
    @Operation(summary = "Create expense")
    public ResponseEntity<ApiResponse<ExpenseDTO>> create(@Valid @RequestBody CreateExpenseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created", expenseService.createExpense(req)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('finance:expense:approve')")
    @Operation(summary = "Approve expense")
    public ResponseEntity<ApiResponse<ExpenseDTO>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Expense approved", expenseService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('finance:expense:approve')")
    @Operation(summary = "Reject expense")
    public ResponseEntity<ApiResponse<ExpenseDTO>> reject(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Expense rejected",
                expenseService.reject(id, body.getOrDefault("reason", "Rejected"))));
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAuthority('finance:expense:approve')")
    @Operation(summary = "Mark expense as paid", description = "Auto-posts Expense/Cash journal entry")
    public ResponseEntity<ApiResponse<ExpenseDTO>> markPaid(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Expense marked as paid", expenseService.markPaid(id)));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('finance:expense:manage')")
    @Operation(summary = "Create expense category")
    public ResponseEntity<ApiResponse<ExpenseCategoryDTO>> createCategory(
            @Valid @RequestBody CreateExpenseCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created", expenseService.createCategory(req)));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('finance:expense:manage')")
    @Operation(summary = "Update expense category")
    public ResponseEntity<ApiResponse<ExpenseCategoryDTO>> updateCategory(
            @PathVariable String id, @Valid @RequestBody CreateExpenseCategoryRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", expenseService.updateCategory(id, req)));
    }
}
