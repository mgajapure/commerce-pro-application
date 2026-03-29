package com.commerce_pro_backend.supplier.contract.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.supplier.contract.dto.SupplierContractDto;
import com.commerce_pro_backend.supplier.contract.mapper.SupplierContractMapper;
import com.commerce_pro_backend.supplier.contract.service.SupplierContractService;
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
@RequestMapping("/v1/supplier-contracts")
@RequiredArgsConstructor
public class SupplierContractController {

    private final SupplierContractService supplierContractService;
    private final SupplierContractMapper supplierContractMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupplierContractDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Supplier contracts retrieved successfully",
                supplierContractService.getAllContracts(pageable).map(supplierContractMapper::toListResponse)));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<SupplierContractDto.ListResponse>>> getBySupplier(
            @PathVariable String supplierId) {
        return ResponseEntity.ok(ApiResponse.success("Supplier contracts retrieved successfully",
                supplierContractMapper.toListResponseList(supplierContractService.getBySupplier(supplierId))));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<SupplierContractDto.ListResponse>>> getByStatus(
            @PathVariable String status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Supplier contracts retrieved successfully",
                supplierContractService.getByStatus(status, pageable).map(supplierContractMapper::toListResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierContractDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier contract retrieved successfully",
                supplierContractMapper.toResponse(supplierContractService.getContract(id))));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Supplier contract stats retrieved successfully", supplierContractService.getContractStats()));
    }

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<SupplierContractDto.ListResponse>>> getExpiring(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success("Expiring contracts retrieved successfully",
                supplierContractMapper.toListResponseList(supplierContractService.getExpiringContracts(days))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierContractDto.Response>> create(
            @Valid @RequestBody SupplierContractDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Supplier contract created successfully",
                    supplierContractMapper.toResponse(supplierContractService.createContract(supplierContractMapper.toEntity(request)))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierContractDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody SupplierContractDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier contract updated successfully",
                supplierContractMapper.toResponse(supplierContractService.updateContract(id, supplierContractMapper.toEntity(request)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        supplierContractService.deleteContract(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier contract deleted successfully", null));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<SupplierContractDto.Response>> activate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier contract activated successfully",
                supplierContractMapper.toResponse(supplierContractService.activateContract(id))));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<ApiResponse<SupplierContractDto.Response>> terminate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier contract terminated successfully",
                supplierContractMapper.toResponse(supplierContractService.terminateContract(id))));
    }
}
