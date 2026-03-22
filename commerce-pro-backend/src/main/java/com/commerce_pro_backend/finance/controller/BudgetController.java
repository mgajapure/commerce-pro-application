package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.*;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1/finance/budgets")
@RequiredArgsConstructor @Tag(name = "Finance - Budgets", description = "Budget management and variance analysis")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:budget:read')")
    @Operation(summary = "List budgets")
    public ResponseEntity<ApiResponse<PageResponse<BudgetDTO>>> list(
            @RequestParam(required = false) Integer fiscalYear,
            @PageableDefault(size = 20) @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Budgets retrieved", budgetService.listBudgets(fiscalYear, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:budget:read')")
    @Operation(summary = "Get budget with actuals")
    public ResponseEntity<ApiResponse<BudgetDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Budget retrieved", budgetService.getById(id)));
    }

    @GetMapping("/{id}/variance")
    @PreAuthorize("hasAuthority('finance:budget:read')")
    @Operation(summary = "Budget vs actual variance report")
    public ResponseEntity<ApiResponse<BudgetDTO>> getVariance(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Variance report retrieved", budgetService.getVarianceReport(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:budget:manage')")
    @Operation(summary = "Create budget")
    public ResponseEntity<ApiResponse<BudgetDTO>> create(@Valid @RequestBody CreateBudgetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget created", budgetService.createBudget(req)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('finance:budget:approve')")
    @Operation(summary = "Approve budget")
    public ResponseEntity<ApiResponse<BudgetDTO>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Budget approved", budgetService.approveBudget(id)));
    }
}
