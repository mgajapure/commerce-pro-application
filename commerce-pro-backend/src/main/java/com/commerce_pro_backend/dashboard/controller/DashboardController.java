package com.commerce_pro_backend.dashboard.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.dashboard.dto.*;
import com.commerce_pro_backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard aggregation endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary",
               description = "Returns aggregated dashboard summary including revenue, orders, customers, products and growth metrics.")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getSummary(
            @RequestParam(name = "period", required = false, defaultValue = "30d") String period) {
        DashboardSummaryDTO summary = dashboardService.getSummary(period);
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", summary));
    }

    @GetMapping("/kpis")
    @Operation(summary = "Get KPI cards",
               description = "Returns key performance indicator cards for the dashboard.")
    public ResponseEntity<ApiResponse<List<KpiCardDTO>>> getKpis() {
        List<KpiCardDTO> kpis = dashboardService.getKpis();
        return ResponseEntity.ok(ApiResponse.success("KPI cards retrieved successfully", kpis));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Get top selling products",
               description = "Returns the top selling products by sales count.")
    public ResponseEntity<ApiResponse<List<TopProductDTO>>> getTopProducts(
            @RequestParam(name = "limit", required = false, defaultValue = "5") int limit) {
        List<TopProductDTO> products = dashboardService.getTopProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Top products retrieved successfully", products));
    }

    @GetMapping("/recent-orders")
    @Operation(summary = "Get recent orders",
               description = "Returns the most recent orders.")
    public ResponseEntity<ApiResponse<List<RecentOrderDTO>>> getRecentOrders(
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        List<RecentOrderDTO> orders = dashboardService.getRecentOrders(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent orders retrieved successfully", orders));
    }

    @GetMapping("/inventory-status")
    @Operation(summary = "Get inventory status summary",
               description = "Returns inventory status including total items, low stock, out of stock counts and total value.")
    public ResponseEntity<ApiResponse<InventoryStatusDTO>> getInventoryStatus() {
        InventoryStatusDTO status = dashboardService.getInventoryStatus();
        return ResponseEntity.ok(ApiResponse.success("Inventory status retrieved successfully", status));
    }

    @GetMapping("/sales-chart")
    @Operation(summary = "Get sales chart data",
               description = "Returns chart data for sales visualization with labels and datasets.")
    public ResponseEntity<ApiResponse<SalesChartDTO>> getSalesChart(
            @RequestParam(name = "period", required = false, defaultValue = "7d") String period) {
        SalesChartDTO chart = dashboardService.getSalesChart(period);
        return ResponseEntity.ok(ApiResponse.success("Sales chart data retrieved successfully", chart));
    }
}
