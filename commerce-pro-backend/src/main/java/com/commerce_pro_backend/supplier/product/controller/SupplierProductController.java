package com.commerce_pro_backend.supplier.product.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.supplier.product.dto.SupplierProductDto;
import com.commerce_pro_backend.supplier.product.service.SupplierProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/supplier-products")
@RequiredArgsConstructor
public class SupplierProductController {

    private final SupplierProductService supplierProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupplierProductDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Supplier products retrieved successfully", supplierProductService.getAllSupplierProducts(pageable)));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<SupplierProductDto.ListResponse>>> getBySupplier(
            @PathVariable String supplierId) {
        return ResponseEntity.ok(ApiResponse.success("Supplier products retrieved successfully", supplierProductService.getBySupplier(supplierId)));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<SupplierProductDto.ListResponse>>> getByProduct(
            @PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.success("Product suppliers retrieved successfully", supplierProductService.getByProduct(productId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierProductDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier product retrieved successfully", supplierProductService.getSupplierProduct(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierProductDto.Response>> create(
            @Valid @RequestBody SupplierProductDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Supplier product created successfully", supplierProductService.createSupplierProduct(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierProductDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody SupplierProductDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier product updated successfully", supplierProductService.updateSupplierProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        supplierProductService.deleteSupplierProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier product deleted successfully", null));
    }
}
