package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.service.FinancialPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j @RestController @RequestMapping("/v1/finance/periods")
@RequiredArgsConstructor @Tag(name = "Finance - Periods", description = "Financial period lifecycle management")
public class FinancialPeriodController {

    private final FinancialPeriodService periodService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:period:read')")
    @Operation(summary = "List all financial periods")
    public ResponseEntity<ApiResponse<PageResponse<FinancialPeriodDTO>>> list(
            @PageableDefault(size = 24) @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Periods retrieved", periodService.getPeriodsPage(pageable)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('finance:period:read')")
    @Operation(summary = "Get current open period")
    public ResponseEntity<ApiResponse<FinancialPeriodDTO>> getCurrent() {
        return ResponseEntity.ok(ApiResponse.success("Current period retrieved", periodService.getCurrentPeriod()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:period:read')")
    @Operation(summary = "Get period by ID")
    public ResponseEntity<ApiResponse<FinancialPeriodDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Period retrieved", periodService.getById(id)));
    }

    @GetMapping("/year/{year}")
    @PreAuthorize("hasAuthority('finance:period:read')")
    @Operation(summary = "Get all periods for a fiscal year")
    public ResponseEntity<ApiResponse<List<FinancialPeriodDTO>>> getByYear(@PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.success("Periods retrieved", periodService.getByFiscalYear(year)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:period:manage')")
    @Operation(summary = "Create financial period")
    public ResponseEntity<ApiResponse<FinancialPeriodDTO>> create(@Valid @RequestBody CreatePeriodRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Period created", periodService.create(req)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('finance:period:close')")
    @Operation(summary = "Close a financial period", description = "Prevents new postings; period can still be reopened")
    public ResponseEntity<ApiResponse<FinancialPeriodDTO>> close(
            @PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.success("Period closed", periodService.closePeriod(id, notes)));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('finance:period:close')")
    @Operation(summary = "Lock a period permanently", description = "Irreversible — period cannot be reopened after locking")
    public ResponseEntity<ApiResponse<FinancialPeriodDTO>> lock(
            @PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.success("Period locked", periodService.lockPeriod(id, notes)));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('finance:period:close')")
    @Operation(summary = "Reopen a closed period", description = "Only works on CLOSED periods — LOCKED periods cannot be reopened")
    public ResponseEntity<ApiResponse<FinancialPeriodDTO>> reopen(
            @PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.success("Period reopened", periodService.reopenPeriod(id, notes)));
    }
}
