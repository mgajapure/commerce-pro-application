package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.*;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.enums.VendorStatus;
import com.commerce_pro_backend.finance.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1/finance/vendors")
@RequiredArgsConstructor @Tag(name = "Finance - Vendors", description = "Vendor master data management")
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:vendor:read')")
    @Operation(summary = "List vendors")
    public ResponseEntity<ApiResponse<PageResponse<VendorSummaryDTO>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "name") @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved",
                vendorService.listVendors(search, status, category, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:vendor:read')")
    @Operation(summary = "Get vendor by ID")
    public ResponseEntity<ApiResponse<VendorDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor retrieved", vendorService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:vendor:manage')")
    @Operation(summary = "Create vendor")
    public ResponseEntity<ApiResponse<VendorDTO>> create(@Valid @RequestBody CreateVendorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vendor created", vendorService.createVendor(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:vendor:manage')")
    @Operation(summary = "Update vendor")
    public ResponseEntity<ApiResponse<VendorDTO>> update(@PathVariable String id,
            @Valid @RequestBody UpdateVendorRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Vendor updated", vendorService.updateVendor(id, req)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('finance:vendor:manage')")
    @Operation(summary = "Change vendor status")
    public ResponseEntity<ApiResponse<VendorDTO>> changeStatus(
            @PathVariable String id, @RequestParam VendorStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Vendor status updated", vendorService.changeStatus(id, status)));
    }
}
