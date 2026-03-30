package com.commerce_pro_backend.supplier.procurement.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.supplier.procurement.dto.ProcurementRequestDto;
import com.commerce_pro_backend.supplier.procurement.mapper.ProcurementRequestMapper;
import com.commerce_pro_backend.supplier.procurement.service.ProcurementRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/procurement-requests")
@RequiredArgsConstructor
public class ProcurementRequestController {

    private final ProcurementRequestService procurementRequestService;
    private final ProcurementRequestMapper procurementRequestMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProcurementRequestDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Procurement requests retrieved successfully",
                procurementRequestService.getAllProcurementRequests(pageable)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<ProcurementRequestDto.ListResponse>>> getByStatus(
            @PathVariable String status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Procurement requests retrieved successfully",
                procurementRequestService.getByStatus(status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProcurementRequestDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Procurement request retrieved successfully",
                procurementRequestService.getProcurementRequest(id)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Procurement request stats retrieved successfully", procurementRequestService.getProcurementStats()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProcurementRequestDto.Response>> create(
            @Valid @RequestBody ProcurementRequestDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Procurement request created successfully",
                    procurementRequestService.createProcurementRequest((request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProcurementRequestDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody ProcurementRequestDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Procurement request updated successfully",
                procurementRequestService.updateProcurementRequest(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        procurementRequestService.deleteProcurementRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Procurement request deleted successfully", null));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ProcurementRequestDto.Response>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Procurement request approved successfully",
                procurementRequestService.approveProcurementRequest(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ProcurementRequestDto.Response>> reject(
            @PathVariable String id,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Procurement request rejected successfully",
                procurementRequestService.rejectProcurementRequest(id, reason)));
    }
}
