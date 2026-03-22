package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.*;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.service.*;
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
import java.util.List;

@RestController @RequestMapping("/v1/finance/tax")
@RequiredArgsConstructor @Tag(name = "Finance - Tax", description = "Tax rate configuration and reporting")
public class TaxController {

    private final TaxRateService   taxRateService;
    private final TaxReportService taxReportService;

    // ── Tax Rates ─────────────────────────────────────────────────────────────

    @GetMapping("/rates")
    @PreAuthorize("hasAuthority('finance:tax:read')")
    @Operation(summary = "List all tax rates")
    public ResponseEntity<ApiResponse<List<TaxRateDTO>>> listRates(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<TaxRateDTO> rates = activeOnly ? taxRateService.getActiveRates() : taxRateService.getAllRates();
        return ResponseEntity.ok(ApiResponse.success("Tax rates retrieved", rates));
    }

    @GetMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('finance:tax:read')")
    @Operation(summary = "Get tax rate by ID")
    public ResponseEntity<ApiResponse<TaxRateDTO>> getRateById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Tax rate retrieved", taxRateService.getById(id)));
    }

    @PostMapping("/rates")
    @PreAuthorize("hasAuthority('finance:tax:manage')")
    @Operation(summary = "Create tax rate")
    public ResponseEntity<ApiResponse<TaxRateDTO>> createRate(@Valid @RequestBody CreateTaxRateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tax rate created", taxRateService.createRate(req)));
    }

    @PutMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('finance:tax:manage')")
    @Operation(summary = "Update tax rate")
    public ResponseEntity<ApiResponse<TaxRateDTO>> updateRate(@PathVariable String id,
            @Valid @RequestBody CreateTaxRateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Tax rate updated", taxRateService.updateRate(id, req)));
    }

    @PatchMapping("/rates/{id}/activate")
    @PreAuthorize("hasAuthority('finance:tax:manage')")
    @Operation(summary = "Activate tax rate")
    public ResponseEntity<ApiResponse<TaxRateDTO>> activate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Tax rate activated", taxRateService.activate(id)));
    }

    @PatchMapping("/rates/{id}/deactivate")
    @PreAuthorize("hasAuthority('finance:tax:manage')")
    @Operation(summary = "Deactivate tax rate")
    public ResponseEntity<ApiResponse<TaxRateDTO>> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Tax rate deactivated", taxRateService.deactivate(id)));
    }

    // ── Jurisdictions ─────────────────────────────────────────────────────────

    @GetMapping("/jurisdictions")
    @PreAuthorize("hasAuthority('finance:tax:read')")
    @Operation(summary = "List tax jurisdictions")
    public ResponseEntity<ApiResponse<List<TaxJurisdictionDTO>>> listJurisdictions() {
        return ResponseEntity.ok(ApiResponse.success("Jurisdictions retrieved", taxRateService.getAllJurisdictions()));
    }

    @PostMapping("/jurisdictions")
    @PreAuthorize("hasAuthority('finance:tax:manage')")
    @Operation(summary = "Create tax jurisdiction")
    public ResponseEntity<ApiResponse<TaxJurisdictionDTO>> createJurisdiction(
            @Valid @RequestBody CreateJurisdictionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Jurisdiction created", taxRateService.createJurisdiction(req)));
    }

    // ── Tax Reports ───────────────────────────────────────────────────────────

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('finance:tax:read')")
    @Operation(summary = "List tax reports")
    public ResponseEntity<ApiResponse<PageResponse<TaxReportDTO>>> listReports(
            @PageableDefault(size = 20) @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Tax reports retrieved", taxReportService.listReports(pageable)));
    }

    @GetMapping("/reports/{id}")
    @PreAuthorize("hasAuthority('finance:tax:read')")
    @Operation(summary = "Get tax report by ID")
    public ResponseEntity<ApiResponse<TaxReportDTO>> getReportById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Tax report retrieved", taxReportService.getById(id)));
    }

    @PostMapping("/reports/generate")
    @PreAuthorize("hasAuthority('finance:tax:manage')")
    @Operation(summary = "Generate tax report for a period")
    public ResponseEntity<ApiResponse<TaxReportDTO>> generateReport(@Valid @RequestBody GenerateTaxReportRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tax report generated", taxReportService.generateReport(req)));
    }

    @PostMapping("/reports/{id}/file")
    @PreAuthorize("hasAuthority('finance:tax:manage')")
    @Operation(summary = "Mark tax report as filed")
    public ResponseEntity<ApiResponse<TaxReportDTO>> fileReport(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Tax report filed", taxReportService.fileReport(id)));
    }
}
