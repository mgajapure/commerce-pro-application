package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.payment.dto.request.GenerateReconciliationRequest;
import com.commerce_pro_backend.payment.dto.response.ReconciliationReportDTO;
import com.commerce_pro_backend.payment.dto.response.ReconciliationSummaryDTO;
import com.commerce_pro_backend.payment.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/payments/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Payment Reconciliation", description = "Period-based ledger matching — internal totals vs gateway settlement")
public class ReconciliationController {

    private final ReconciliationService service;

    @GetMapping
    @Operation(summary = "List reconciliation reports")
    public ResponseEntity<ApiResponse<PageResponse<ReconciliationSummaryDTO>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Reconciliation reports retrieved", service.list(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reconciliation report detail including all matched/unmatched entries")
    public ResponseEntity<ApiResponse<ReconciliationReportDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Reconciliation report retrieved", service.getById(id)));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate a new reconciliation report for a date range",
               description = "Aggregates all settled transactions in the period and matches them against gateway totals")
    public ResponseEntity<ApiResponse<ReconciliationReportDTO>> generate(
            @Valid @RequestBody GenerateReconciliationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reconciliation report generated", service.generate(req)));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a reconciliation report", description = "Mark as finalized — no further edits permitted")
    public ResponseEntity<ApiResponse<ReconciliationReportDTO>> close(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Reconciliation report closed", service.close(id)));
    }
}
