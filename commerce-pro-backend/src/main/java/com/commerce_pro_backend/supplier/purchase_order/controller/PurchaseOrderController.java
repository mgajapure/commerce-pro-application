package com.commerce_pro_backend.supplier.purchase_order.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.supplier.purchase_order.dto.PurchaseOrderDto;
import com.commerce_pro_backend.supplier.purchase_order.mapper.PurchaseOrderMapper;
import com.commerce_pro_backend.supplier.purchase_order.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Purchase orders retrieved successfully",
                purchaseOrderService.getAllPurchaseOrders(pageable).map(purchaseOrderMapper::toListResponse)));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<PurchaseOrderDto.ListResponse>>> getBySupplier(
            @PathVariable String supplierId) {
        return ResponseEntity.ok(ApiResponse.success("Supplier purchase orders retrieved successfully",
                purchaseOrderMapper.toListResponseList(purchaseOrderService.getBySupplier(supplierId))));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<PurchaseOrderDto.ListResponse>>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success("Purchase orders retrieved successfully",
                purchaseOrderMapper.toListResponseList(purchaseOrderService.getByStatus(status))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order retrieved successfully",
                purchaseOrderMapper.toResponse(purchaseOrderService.getPurchaseOrder(id))));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Purchase order stats retrieved successfully", purchaseOrderService.getPurchaseOrderStats()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDto.Response>> create(
            @Valid @RequestBody PurchaseOrderDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Purchase order created successfully",
                    purchaseOrderMapper.toResponse(purchaseOrderService.createPurchaseOrder(purchaseOrderMapper.toEntity(request)))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody PurchaseOrderDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order updated successfully",
                purchaseOrderMapper.toResponse(purchaseOrderService.updatePurchaseOrder(id, purchaseOrderMapper.toEntity(request)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Purchase order deleted successfully", null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PurchaseOrderDto.Response>> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order status updated successfully",
                purchaseOrderMapper.toResponse(purchaseOrderService.updateStatus(id, status))));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderDto.Response>> receivePurchaseOrder(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order received successfully",
                purchaseOrderMapper.toResponse(purchaseOrderService.receivePurchaseOrder(id))));
    }
}
