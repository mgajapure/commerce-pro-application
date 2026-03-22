package com.commerce_pro_backend.inventory.valuation.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.inventory.valuation.dto.InventoryValuationDto;
import com.commerce_pro_backend.inventory.valuation.service.InventoryValuationService;
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
 * Inventory Valuation REST Controller
 * Base URL: /v1/inventory/valuations
 *
 * Provides full CRUD plus recalculation and method-change operations
 * for inventory cost valuation records (FIFO / LIFO / Weighted Average /
 * Specific Identification).
 */
@Slf4j
@RestController
@RequestMapping("/v1/inventory/valuations")
@RequiredArgsConstructor
@Tag(name = "Inventory Valuation", description = "Inventory valuation management APIs")
public class InventoryValuationController {

    private final InventoryValuationService valuationService;

    // ==================== LIST ====================

    @GetMapping
    @Operation(summary = "Get all inventory valuations (pageable)")
    public ResponseEntity<ApiResponse<PageResponse<InventoryValuationDto.Response>>> getAllValuations(
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        PageResponse<InventoryValuationDto.Response> page = valuationService.getAllValuations(pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Inventory valuations retrieved successfully", page));
    }

    // ==================== DETAIL ====================

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory valuation by ID")
    public ResponseEntity<ApiResponse<InventoryValuationDto.Response>> getValuationById(
            @PathVariable String id) {

        InventoryValuationDto.Response response = valuationService.getValuationById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Inventory valuation retrieved successfully", response));
    }

    // ==================== FILTER BY PRODUCT / WAREHOUSE ====================

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory valuations by product ID")
    public ResponseEntity<ApiResponse<List<InventoryValuationDto.Response>>> getValuationsByProduct(
            @PathVariable String productId) {

        List<InventoryValuationDto.Response> valuations =
                valuationService.getValuationsByProduct(productId);
        return ResponseEntity.ok(
                ApiResponse.success("Product valuations retrieved successfully", valuations));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get inventory valuations by warehouse ID")
    public ResponseEntity<ApiResponse<List<InventoryValuationDto.Response>>> getValuationsByWarehouse(
            @PathVariable String warehouseId) {

        List<InventoryValuationDto.Response> valuations =
                valuationService.getValuationsByWarehouse(warehouseId);
        return ResponseEntity.ok(
                ApiResponse.success("Warehouse valuations retrieved successfully", valuations));
    }

    // ==================== SUMMARY REPORT ====================

    @GetMapping("/summary")
    @Operation(summary = "Get valuation summary report")
    public ResponseEntity<ApiResponse<InventoryValuationDto.SummaryResponse>> getValuationSummary() {
        InventoryValuationDto.SummaryResponse summary = valuationService.getSummary();
        return ResponseEntity.ok(
                ApiResponse.success("Valuation summary retrieved successfully", summary));
    }

    // ==================== CREATE ====================

    @PostMapping
    @Operation(summary = "Create a new inventory valuation record")
    public ResponseEntity<ApiResponse<InventoryValuationDto.Response>> createValuation(
            @Valid @RequestBody InventoryValuationDto.Request request) {

        InventoryValuationDto.Response created = valuationService.createValuation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory valuation created successfully", created));
    }

    // ==================== UPDATE ====================

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing inventory valuation record")
    public ResponseEntity<ApiResponse<InventoryValuationDto.Response>> updateValuation(
            @PathVariable String id,
            @Valid @RequestBody InventoryValuationDto.Request request) {

        InventoryValuationDto.Response updated = valuationService.updateValuation(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Inventory valuation updated successfully", updated));
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an inventory valuation record")
    public ResponseEntity<ApiResponse<Void>> deleteValuation(
            @PathVariable String id) {

        valuationService.deleteValuation(id);
        return ResponseEntity.ok(
                ApiResponse.success("Inventory valuation deleted successfully", null));
    }

    // ==================== RECALCULATE ====================

    @PostMapping("/recalculate")
    @Operation(summary = "Recalculate inventory valuations",
               description = "Recalculates one or more valuations. "
                       + "When valuationIds is omitted all active valuations are recalculated.")
    public ResponseEntity<ApiResponse<List<InventoryValuationDto.Response>>> recalculateValuations(
            @Valid @RequestBody InventoryValuationDto.RecalculateRequest request) {

        List<InventoryValuationDto.Response> results =
                valuationService.recalculateValuations(request);
        return ResponseEntity.ok(
                ApiResponse.success("Inventory valuations recalculated successfully", results));
    }

    // ==================== CHANGE METHOD ====================

    @PostMapping("/{id}/change-method")
    @Operation(summary = "Change the valuation method for a specific record",
               description = "Changes the costing method and immediately triggers recalculation "
                       + "using the new method.")
    public ResponseEntity<ApiResponse<InventoryValuationDto.Response>> changeValuationMethod(
            @PathVariable String id,
            @Valid @RequestBody InventoryValuationDto.ChangeMethodRequest request) {

        InventoryValuationDto.Response updated =
                valuationService.changeValuationMethod(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Valuation method changed successfully", updated));
    }
}
