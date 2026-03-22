package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.service.CashFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1/finance/cashflow")
@RequiredArgsConstructor @Tag(name = "Finance - Cash Flow", description = "Cash flow forecasting")
public class CashFlowController {

    private final CashFlowService cashFlowService;

    @GetMapping("/forecast")
    @PreAuthorize("hasAuthority('finance:cashflow:read')")
    @Operation(summary = "30/60/90-day cash flow forecast", description = "Projected cash position from AR aging and AP due dates")
    public ResponseEntity<ApiResponse<CashFlowForecastDTO>> getForecast() {
        return ResponseEntity.ok(ApiResponse.success("Cash flow forecast retrieved", cashFlowService.getForecast()));
    }
}
