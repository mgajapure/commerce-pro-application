package com.commerce_pro_backend.inventory.forecast.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.inventory.forecast.dto.DemandForecastDto;
import com.commerce_pro_backend.inventory.forecast.service.DemandForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Demand Forecasting.
 * Base URL: /v1/inventory/forecasts
 */
@Slf4j
@RestController
@RequestMapping("/v1/inventory/forecasts")
@RequiredArgsConstructor
@Tag(name = "Demand Forecasting", description = "Demand forecasting APIs for inventory planning")
public class DemandForecastController {

    private final DemandForecastService forecastService;

    // ==================== LIST & FILTER ====================

    @GetMapping
    @Operation(summary = "Get all demand forecasts (pageable)")
    public ResponseEntity<ApiResponse<PageResponse<DemandForecastDto.Response>>> getAllForecasts(
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "generatedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        PageResponse<DemandForecastDto.Response> page = forecastService.getAllForecasts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Demand forecasts retrieved successfully", page));
    }

    // ==================== GET BY ID ====================

    @GetMapping("/{id}")
    @Operation(summary = "Get demand forecast by ID")
    public ResponseEntity<ApiResponse<DemandForecastDto.Response>> getForecastById(
            @PathVariable String id) {

        DemandForecastDto.Response forecast = forecastService.getForecastById(id);
        return ResponseEntity.ok(ApiResponse.success("Demand forecast retrieved successfully", forecast));
    }

    // ==================== GET BY PRODUCT / WAREHOUSE ====================

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get demand forecasts by product ID")
    public ResponseEntity<ApiResponse<List<DemandForecastDto.Response>>> getForecastsByProduct(
            @PathVariable String productId) {

        List<DemandForecastDto.Response> forecasts = forecastService.getForecastsByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Product demand forecasts retrieved successfully", forecasts));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get demand forecasts by warehouse ID")
    public ResponseEntity<ApiResponse<List<DemandForecastDto.Response>>> getForecastsByWarehouse(
            @PathVariable String warehouseId) {

        List<DemandForecastDto.Response> forecasts = forecastService.getForecastsByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.success("Warehouse demand forecasts retrieved successfully", forecasts));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get demand forecasts by status (active / archived / obsolete)")
    public ResponseEntity<ApiResponse<List<DemandForecastDto.Response>>> getForecastsByStatus(
            @PathVariable String status) {

        List<DemandForecastDto.Response> forecasts = forecastService.getForecastsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Demand forecasts by status retrieved successfully", forecasts));
    }

    @GetMapping("/period/{period}")
    @Operation(summary = "Get demand forecasts by period (daily / weekly / monthly / quarterly / yearly)")
    public ResponseEntity<ApiResponse<List<DemandForecastDto.Response>>> getForecastsByPeriod(
            @PathVariable String period) {

        List<DemandForecastDto.Response> forecasts = forecastService.getForecastsByPeriod(period);
        return ResponseEntity.ok(ApiResponse.success("Demand forecasts by period retrieved successfully", forecasts));
    }

    // ==================== CREATE / UPDATE ====================

    @PostMapping
    @Operation(summary = "Create a demand forecast record manually")
    public ResponseEntity<ApiResponse<DemandForecastDto.Response>> createForecast(
            @Valid @RequestBody DemandForecastDto.Request request) {

        DemandForecastDto.Response created = forecastService.createForecast(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demand forecast created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a demand forecast record")
    public ResponseEntity<ApiResponse<DemandForecastDto.Response>> updateForecast(
            @PathVariable String id,
            @Valid @RequestBody DemandForecastDto.Request request) {

        DemandForecastDto.Response updated = forecastService.updateForecast(id, request);
        return ResponseEntity.ok(ApiResponse.success("Demand forecast updated successfully", updated));
    }

    // ==================== FORECAST GENERATION ====================

    @PostMapping("/generate")
    @Operation(summary = "Generate a demand forecast using a forecasting algorithm")
    public ResponseEntity<ApiResponse<DemandForecastDto.Response>> generateForecast(
            @Valid @RequestBody DemandForecastDto.GenerateRequest request) {

        DemandForecastDto.Response generated = forecastService.generateForecast(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demand forecast generated successfully", generated));
    }

    // ==================== STATUS TRANSITIONS ====================

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a demand forecast")
    public ResponseEntity<ApiResponse<DemandForecastDto.Response>> archiveForecast(
            @PathVariable String id) {

        DemandForecastDto.Response archived = forecastService.archiveForecast(id);
        return ResponseEntity.ok(ApiResponse.success("Demand forecast archived successfully", archived));
    }

    @PostMapping("/{id}/obsolete")
    @Operation(summary = "Mark a demand forecast as obsolete")
    public ResponseEntity<ApiResponse<DemandForecastDto.Response>> markObsolete(
            @PathVariable String id) {

        DemandForecastDto.Response obsolete = forecastService.markObsolete(id);
        return ResponseEntity.ok(ApiResponse.success("Demand forecast marked as obsolete", obsolete));
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a demand forecast")
    public ResponseEntity<ApiResponse<Void>> deleteForecast(
            @PathVariable String id) {

        forecastService.deleteForecast(id);
        return ResponseEntity.ok(ApiResponse.success("Demand forecast deleted successfully", null));
    }
}
