package com.commerce_pro_backend.supplier.evaluation.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.supplier.evaluation.dto.SupplierEvaluationDto;
import com.commerce_pro_backend.supplier.evaluation.service.SupplierEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/supplier-evaluations")
@RequiredArgsConstructor
public class SupplierEvaluationController {

    private final SupplierEvaluationService supplierEvaluationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupplierEvaluationDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Supplier evaluations retrieved successfully", supplierEvaluationService.getAllEvaluations(pageable)));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<Page<SupplierEvaluationDto.ListResponse>>> getBySupplier(
            @PathVariable String supplierId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Supplier evaluations retrieved successfully", supplierEvaluationService.getBySupplier(supplierId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierEvaluationDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier evaluation retrieved successfully", supplierEvaluationService.getEvaluation(id)));
    }

    @GetMapping("/supplier/{supplierId}/scores")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAverageScores(
            @PathVariable String supplierId) {
        return ResponseEntity.ok(ApiResponse.success("Supplier average scores retrieved successfully", supplierEvaluationService.getSupplierAverageScores(supplierId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierEvaluationDto.Response>> create(
            @Valid @RequestBody SupplierEvaluationDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Supplier evaluation created successfully", supplierEvaluationService.createEvaluation(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierEvaluationDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody SupplierEvaluationDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier evaluation updated successfully", supplierEvaluationService.updateEvaluation(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        supplierEvaluationService.deleteEvaluation(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier evaluation deleted successfully", null));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<SupplierEvaluationDto.Response>> submit(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Supplier evaluation submitted successfully", supplierEvaluationService.submitEvaluation(id)));
    }
}
