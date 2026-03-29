package com.commerce_pro_backend.inventory.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.inventory.dto.*;
import com.commerce_pro_backend.inventory.service.InventoryService;
import com.commerce_pro_backend.inventory.service.LowStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inventory REST Controller
 * Base URL: /api/inventory
 */
@Slf4j
@RestController
@RequestMapping("/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management APIs")
public class InventoryController {

    private final InventoryService inventoryService;
    private final LowStockService lowStockService;

    // ==================== INVENTORY ENDPOINTS ====================

    @GetMapping
    @Operation(summary = "Get inventory with filtering")
    public ResponseEntity<ApiResponse<PageResponse<InventoryDTO>>> getInventory(
            @RequestParam(name="search", required = false) String search,
            @RequestParam(name="warehouseId", required = false) String warehouseId,
            @RequestParam(name="status", required = false) String status,
            @RequestParam(name="category", required = false) String category,
            @RequestParam(name="productId", required = false) String productId,
            @RequestParam(name="lowStockOnly", required = false) Boolean lowStockOnly,
            @RequestParam(name="outOfStockOnly", required = false) Boolean outOfStockOnly,
            @RequestParam(name="sortBy", required = false) String sortBy,
            @RequestParam(name="sortDirection", required = false, defaultValue = "asc") String sortDirection,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        InventoryFilterDTO filter = InventoryFilterDTO.builder()
                .searchQuery(search)
                .warehouseId(warehouseId)
                .status(status)
                .category(category)
                .productId(productId)
                .lowStockOnly(lowStockOnly)
                .outOfStockOnly(outOfStockOnly)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<InventoryDTO> page = inventoryService.getInventory(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Inventory retrieved successfully", page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory by ID")
    public ResponseEntity<ApiResponse<InventoryDTO>> getInventoryById(
            @PathVariable String id) {
        InventoryDTO inventory = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory retrieved successfully", inventory));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory by product ID")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getInventoryByProduct(
            @PathVariable String productId) {
        List<InventoryDTO> inventory = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Product inventory retrieved successfully", inventory));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get inventory by warehouse ID")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getInventoryByWarehouse(
            @PathVariable String warehouseId) {
        List<InventoryDTO> inventory = inventoryService.getInventoryByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.success("Warehouse inventory retrieved successfully", inventory));
    }

    @PostMapping
    @Operation(summary = "Create inventory record")
    public ResponseEntity<ApiResponse<InventoryDTO>> createInventory(
            @Valid @RequestBody InventoryRequestDTO requestDTO) {
        InventoryDTO created = inventoryService.createInventory(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inventory record")
    public ResponseEntity<ApiResponse<InventoryDTO>> updateInventory(
            @PathVariable String id,
            @Valid @RequestBody InventoryRequestDTO requestDTO) {
        InventoryDTO updated = inventoryService.updateInventory(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Inventory updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete inventory record")
    public ResponseEntity<ApiResponse<Void>> deleteInventory(
            @PathVariable String id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory deleted successfully", null));
    }

    // ==================== WAREHOUSE ENDPOINTS ====================

    @GetMapping("/warehouses")
    @Operation(summary = "Get all warehouses")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getAllWarehouses() {
        List<WarehouseDTO> warehouses = inventoryService.getAllWarehouses();
        return ResponseEntity.ok(ApiResponse.success("Warehouses retrieved successfully", warehouses));
    }

    @GetMapping("/warehouses/active")
    @Operation(summary = "Get active warehouses")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getActiveWarehouses() {
        List<WarehouseDTO> warehouses = inventoryService.getActiveWarehouses();
        return ResponseEntity.ok(ApiResponse.success("Active warehouses retrieved successfully", warehouses));
    }

    @GetMapping("/warehouses/{id}")
    @Operation(summary = "Get warehouse by ID")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getWarehouseById(
            @PathVariable String id) {
        WarehouseDTO warehouse = inventoryService.getWarehouseById(id);
        return ResponseEntity.ok(ApiResponse.success("Warehouse retrieved successfully", warehouse));
    }

    @PostMapping("/warehouses")
    @Operation(summary = "Create warehouse")
    public ResponseEntity<ApiResponse<WarehouseDTO>> createWarehouse(
            @Valid @RequestBody WarehouseRequestDTO requestDTO) {
        WarehouseDTO created = inventoryService.createWarehouse(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Warehouse created successfully", created));
    }

    @PutMapping("/warehouses/{id}")
    @Operation(summary = "Update warehouse")
    public ResponseEntity<ApiResponse<WarehouseDTO>> updateWarehouse(
            @PathVariable String id,
            @Valid @RequestBody WarehouseRequestDTO requestDTO) {
        WarehouseDTO updated = inventoryService.updateWarehouse(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Warehouse updated successfully", updated));
    }

    @DeleteMapping("/warehouses/{id}")
    @Operation(summary = "Delete warehouse")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(
            @PathVariable String id) {
        inventoryService.deleteWarehouse(id);
        return ResponseEntity.ok(ApiResponse.success("Warehouse deleted successfully", null));
    }

    

    // ==================== STOCK OPERATIONS ====================

    @PostMapping("/{id}/stock")
    @Operation(summary = "Adjust stock for inventory item")
    public ResponseEntity<ApiResponse<InventoryDTO>> adjustStock(
            @PathVariable String id,
            @Valid @RequestBody StockUpdateRequestDTO requestDTO) {
        InventoryDTO updated = inventoryService.adjustStock(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", updated));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer stock between warehouses")
    public ResponseEntity<ApiResponse<Void>> transferStock(
            @Valid @RequestBody StockTransferRequestDTO requestDTO) {
        inventoryService.transferStock(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Stock transferred successfully", null));
    }

    @PostMapping("/transfer/batch")
    @Operation(summary = "Batch transfer stock between warehouses",
               description = "Transfer multiple products between the same source and destination warehouse in one atomic operation")
    public ResponseEntity<ApiResponse<Void>> batchTransferStock(
            @Valid @RequestBody BatchStockTransferRequestDTO requestDTO) {
        inventoryService.batchTransferStock(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Batch stock transfer completed successfully", null));
    }

    // ==================== STOCK MOVEMENTS ====================

    @GetMapping("/{id}/movements")
    @Operation(summary = "Get stock movements for inventory")
    public ResponseEntity<ApiResponse<List<StockMovementDTO>>> getStockMovements(
            @PathVariable String id) {
        List<StockMovementDTO> movements = inventoryService.getStockMovements(id);
        return ResponseEntity.ok(ApiResponse.success("Stock movements retrieved successfully", movements));
    }

    @GetMapping("/product/{productId}/movements")
    @Operation(summary = "Get stock movements for product")
    public ResponseEntity<ApiResponse<List<StockMovementDTO>>> getProductStockMovements(
            @PathVariable String productId) {
        List<StockMovementDTO> movements = inventoryService.getProductStockMovements(productId);
        return ResponseEntity.ok(ApiResponse.success("Product stock movements retrieved successfully", movements));
    }

    // ==================== LOW/OUT OF STOCK ====================

    @GetMapping("/stock/low")
    @Operation(summary = "Get low stock items")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getLowStockItems() {
        List<InventoryDTO> items = inventoryService.getLowStockItems();
        return ResponseEntity.ok(ApiResponse.success("Low stock items retrieved successfully", items));
    }

    @GetMapping("/stock/out-of-stock")
    @Operation(summary = "Get out of stock items")
    public ResponseEntity<ApiResponse<List<InventoryDTO>>> getOutOfStockItems() {
        List<InventoryDTO> items = inventoryService.getOutOfStockItems();
        return ResponseEntity.ok(ApiResponse.success("Out of stock items retrieved successfully", items));
    }

    // ==================== LOW STOCK ALERTS ====================

    @GetMapping("/alerts/low-stock")
    @Operation(summary = "Get low stock alerts")
    public ResponseEntity<ApiResponse<List<LowStockAlertDTO>>> getLowStockAlerts() {
        List<LowStockAlertDTO> alerts = lowStockService.getLowStockAlerts();
        return ResponseEntity.ok(ApiResponse.success("Low stock alerts retrieved successfully", alerts));
    }

    @GetMapping("/alerts/critical")
    @Operation(summary = "Get critical stock alerts (out of stock)")
    public ResponseEntity<ApiResponse<List<LowStockAlertDTO>>> getCriticalAlerts() {
        List<LowStockAlertDTO> alerts = lowStockService.getCriticalAlerts();
        return ResponseEntity.ok(ApiResponse.success("Critical alerts retrieved successfully", alerts));
    }

    @GetMapping("/alerts/warehouse/{warehouseId}")
    @Operation(summary = "Get alerts by warehouse")
    public ResponseEntity<ApiResponse<List<LowStockAlertDTO>>> getAlertsByWarehouse(
            @PathVariable String warehouseId) {
        List<LowStockAlertDTO> alerts = lowStockService.getAlertsByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.success("Warehouse alerts retrieved successfully", alerts));
    }

    // ==================== STATISTICS ====================

    @GetMapping("/stats")
    @Operation(summary = "Get inventory statistics")
    public ResponseEntity<ApiResponse<InventoryStatsDTO>> getInventoryStats() {
        InventoryStatsDTO stats = inventoryService.getInventoryStats();
        return ResponseEntity.ok(ApiResponse.success("Inventory statistics retrieved successfully", stats));
    }
}
