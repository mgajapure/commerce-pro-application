package com.commerce_pro_backend.analytics.controller;

import com.commerce_pro_backend.analytics.dto.*;
import com.commerce_pro_backend.analytics.service.*;
import com.commerce_pro_backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Analytics, reporting and KPI dashboard endpoints")
public class AnalyticsController {

    private final AnalyticsDashboardService dashboardService;
    private final SalesReportService        salesService;
    private final InventoryReportService    inventoryService;
    private final OrderReportService        orderService;
    private final CustomerReportService     customerService;
    private final FinancialReportService    financialService;
    private final ShippingReportService     shippingService;
    private final ReturnReportService       returnService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('analytics:dashboard:read')")
    @Operation(summary = "Get analytics dashboard KPIs",
               description = "Returns real-time KPIs for the main analytics dashboard: revenue, orders, customers, inventory, fulfilment and shipping metrics.")
    public ResponseEntity<ApiResponse<AnalyticsDashboardDTO>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", dashboardService.getDashboard()));
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    @PostMapping("/sales/overview")
    @PreAuthorize("hasAuthority('analytics:sales:read')")
    @Operation(summary = "Sales overview", description = "Aggregated revenue, orders, AOV with period-over-period comparison and trend series.")
    public ResponseEntity<ApiResponse<SalesOverviewDTO>> getSalesOverview(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Sales overview retrieved", salesService.getOverview(filter)));
    }

    @PostMapping("/sales/by-date")
    @PreAuthorize("hasAuthority('analytics:sales:read')")
    @Operation(summary = "Sales by date", description = "Revenue broken down by the requested time grouping (DAY/WEEK/MONTH/QUARTER/YEAR).")
    public ResponseEntity<ApiResponse<SalesReportDTO>> getSalesByDate(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Sales by date retrieved", salesService.getByDate(filter)));
    }

    @PostMapping("/sales/by-product")
    @PreAuthorize("hasAuthority('analytics:sales:read')")
    @Operation(summary = "Sales by product", description = "Revenue, units and AOV broken down per product SKU.")
    public ResponseEntity<ApiResponse<SalesReportDTO>> getSalesByProduct(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Sales by product retrieved", salesService.getByProduct(filter)));
    }

    @PostMapping("/sales/by-category")
    @PreAuthorize("hasAuthority('analytics:sales:read')")
    @Operation(summary = "Sales by category", description = "Revenue and units aggregated by product category.")
    public ResponseEntity<ApiResponse<SalesReportDTO>> getSalesByCategory(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Sales by category retrieved", salesService.getByCategory(filter)));
    }

    @PostMapping("/sales/by-channel")
    @PreAuthorize("hasAuthority('analytics:sales:read')")
    @Operation(summary = "Sales by channel", description = "Revenue breakdown by order source (STOREFRONT, MANUAL, API, IMPORT, MOBILE_APP).")
    public ResponseEntity<ApiResponse<SalesReportDTO>> getSalesByChannel(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Sales by channel retrieved", salesService.getByChannel(filter)));
    }

    // ── Inventory ─────────────────────────────────────────────────────────────

    @PostMapping("/inventory/stock-value")
    @PreAuthorize("hasAuthority('analytics:inventory:read')")
    @Operation(summary = "Inventory stock value", description = "Current stock quantities and valuation per SKU / warehouse.")
    public ResponseEntity<ApiResponse<InventoryReportDTO>> getInventoryStockValue(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Inventory stock value retrieved", inventoryService.getStockValue(filter)));
    }

    @PostMapping("/inventory/movement")
    @PreAuthorize("hasAuthority('analytics:inventory:read')")
    @Operation(summary = "Inventory movement", description = "All stock movements (IN, OUT, ADJUSTMENT, TRANSFER, RETURN, DAMAGED) in the date range.")
    public ResponseEntity<ApiResponse<InventoryReportDTO>> getInventoryMovement(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Inventory movement retrieved", inventoryService.getMovement(filter)));
    }

    @PostMapping("/inventory/ageing")
    @PreAuthorize("hasAuthority('analytics:inventory:read')")
    @Operation(summary = "Inventory ageing", description = "Identifies slow-moving stock bucketed as 0-30 / 31-60 / 61-90 / 90+ days since last movement.")
    public ResponseEntity<ApiResponse<InventoryReportDTO>> getInventoryAgeing(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Inventory ageing retrieved", inventoryService.getAgeing(filter)));
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    @PostMapping("/orders/summary")
    @PreAuthorize("hasAuthority('analytics:orders:read')")
    @Operation(summary = "Order report", description = "Fulfilment rate, cancellation rate, return rate and order detail rows.")
    public ResponseEntity<ApiResponse<OrderReportDTO>> getOrderReport(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Order report retrieved", orderService.getReport(filter)));
    }

    // ── Customers ─────────────────────────────────────────────────────────────

    @PostMapping("/customers/ltv")
    @PreAuthorize("hasAuthority('analytics:customers:read')")
    @Operation(summary = "Customer LTV report", description = "Lifetime value, tier distribution, acquisition sources and customer segmentation.")
    public ResponseEntity<ApiResponse<CustomerReportDTO>> getCustomerLtv(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Customer LTV report retrieved", customerService.getLtvReport(filter)));
    }

    @PostMapping("/customers/cohort")
    @PreAuthorize("hasAuthority('analytics:customers:read')")
    @Operation(summary = "Customer cohort analysis", description = "Monthly cohort retention matrix showing re-purchase rates over 12 months.")
    public ResponseEntity<ApiResponse<CustomerReportDTO>> getCustomerCohort(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Customer cohort retrieved", customerService.getCohortReport(filter)));
    }

    @PostMapping("/customers/churn")
    @PreAuthorize("hasAuthority('analytics:customers:read')")
    @Operation(summary = "Customer churn analysis", description = "At-risk customers (45+ days without order) with churn risk scores.")
    public ResponseEntity<ApiResponse<CustomerReportDTO>> getCustomerChurn(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Customer churn report retrieved", customerService.getChurnReport(filter)));
    }

    // ── Financial ─────────────────────────────────────────────────────────────

    @PostMapping("/financial/summary")
    @PreAuthorize("hasAuthority('analytics:financial:read')")
    @Operation(summary = "Financial report", description = "Gross/net revenue, COGS, gross profit, margin %, tax summary and period trend.")
    public ResponseEntity<ApiResponse<FinancialReportDTO>> getFinancialReport(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Financial report retrieved", financialService.getReport(filter)));
    }

    // ── Shipping ─────────────────────────────────────────────────────────────

    @PostMapping("/shipping/summary")
    @PreAuthorize("hasAuthority('analytics:shipping:read')")
    @Operation(summary = "Shipping report", description = "On-time delivery rate, average delivery days and per-carrier performance breakdown.")
    public ResponseEntity<ApiResponse<ShippingReportDTO>> getShippingReport(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Shipping report retrieved", shippingService.getReport(filter)));
    }

    // ── Returns ───────────────────────────────────────────────────────────────

    @PostMapping("/returns/summary")
    @PreAuthorize("hasAuthority('analytics:returns:read')")
    @Operation(summary = "Returns report", description = "Return rate, reason analysis, product-level return rates and refunded amounts.")
    public ResponseEntity<ApiResponse<ReturnReportDTO>> getReturnReport(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Returns report retrieved", returnService.getReport(filter)));
    }
}
