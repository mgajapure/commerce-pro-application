package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.service.ProfitLossService;
import com.commerce_pro_backend.finance.service.RevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Slf4j @RestController @RequestMapping("/v1/finance/revenue")
@RequiredArgsConstructor @Tag(name = "Finance - Revenue", description = "Revenue overview and trends")
public class RevenueController {

    private final RevenueService   revenueService;
    private final ProfitLossService plService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('finance:revenue:read')")
    @Operation(summary = "Revenue overview", description = "Dashboard KPIs — today, MTD, YTD with AR/AP snapshot")
    public ResponseEntity<ApiResponse<RevenueOverviewDTO>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success("Revenue overview retrieved", revenueService.getOverview()));
    }

    @GetMapping("/pnl")
    @PreAuthorize("hasAuthority('finance:pnl:read')")
    @Operation(summary = "Profit & Loss statement", description = "P&L for a custom date range")
    public ResponseEntity<ApiResponse<ProfitLossDTO>> getPnL(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String label) {
        return ResponseEntity.ok(ApiResponse.success("P&L retrieved", plService.getPnL(from, to, label)));
    }

    @GetMapping("/pnl/ytd")
    @PreAuthorize("hasAuthority('finance:pnl:read')")
    @Operation(summary = "Year-to-date P&L")
    public ResponseEntity<ApiResponse<ProfitLossDTO>> getYtdPnL() {
        return ResponseEntity.ok(ApiResponse.success("YTD P&L retrieved", plService.getYTD()));
    }

    @GetMapping("/pnl/mtd")
    @PreAuthorize("hasAuthority('finance:pnl:read')")
    @Operation(summary = "Month-to-date P&L")
    public ResponseEntity<ApiResponse<ProfitLossDTO>> getMtdPnL() {
        return ResponseEntity.ok(ApiResponse.success("MTD P&L retrieved", plService.getMonthToDate()));
    }
}
