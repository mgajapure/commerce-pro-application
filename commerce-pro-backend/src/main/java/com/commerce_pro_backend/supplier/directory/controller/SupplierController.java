package com.commerce_pro_backend.supplier.directory.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.supplier.directory.dto.SupplierDto;
import com.commerce_pro_backend.supplier.directory.mapper.SupplierMapper;
import com.commerce_pro_backend.supplier.directory.service.SupplierService;
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
@RequestMapping("/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierMapper supplierMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupplierDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Suppliers retrieved successfully",
                supplierService.getAllSuppliers(pageable).map(supplierMapper::toListResponse)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SupplierDto.ListResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success("Active suppliers retrieved successfully",
                supplierMapper.toListResponseList(supplierService.getAllActiveSuppliers())));
    }

    @GetMapping("/preferred")
    public ResponseEntity<ApiResponse<List<SupplierDto.ListResponse>>> getPreferred() {
        return ResponseEntity.ok(ApiResponse.success("Preferred suppliers retrieved successfully",
                supplierMapper.toListResponseList(supplierService.getPreferredSuppliers())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier retrieved successfully",
                supplierMapper.toResponse(supplierService.getSupplier(id))));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Supplier stats retrieved successfully", supplierService.getSupplierStats()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDto.Response>> create(
            @Valid @RequestBody SupplierDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Supplier created successfully",
                    supplierMapper.toResponse(supplierService.createSupplier(request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody SupplierDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier updated successfully",
                supplierMapper.toResponse(supplierService.updateSupplier(id, supplierMapper.toEntity(request)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier deleted successfully", null));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<Void>> toggleActive(
            @PathVariable String id,
            @RequestParam boolean active) {
        supplierService.toggleActive(id, active);
        return ResponseEntity.ok(ApiResponse.success("Supplier active status updated", null));
    }

    @PatchMapping("/{id}/preferred")
    public ResponseEntity<ApiResponse<Void>> togglePreferred(
            @PathVariable String id,
            @RequestParam boolean preferred) {
        supplierService.togglePreferred(id, preferred);
        return ResponseEntity.ok(ApiResponse.success("Supplier preferred status updated", null));
    }
}
