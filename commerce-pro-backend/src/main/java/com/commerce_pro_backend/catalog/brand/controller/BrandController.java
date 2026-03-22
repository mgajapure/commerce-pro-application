package com.commerce_pro_backend.catalog.brand.controller;

import com.commerce_pro_backend.catalog.brand.dto.BrandDto;
import com.commerce_pro_backend.catalog.brand.service.BrandService;
import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BrandDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Brands retrieved successfully", brandService.getAllBrands(pageable)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BrandDto.ListResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success("Active brands retrieved successfully", brandService.getAllActiveBrands()));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<BrandDto.ListResponse>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.success("Featured brands retrieved successfully", brandService.getFeaturedBrands()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Brand retrieved successfully", brandService.getBrand(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<BrandDto.Response>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Brand retrieved successfully", brandService.getBrandBySlug(slug)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandDto.Response>> create(
            @Valid @RequestBody BrandDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Brand created successfully", brandService.createBrand(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody BrandDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Brand updated successfully", brandService.updateBrand(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand deleted successfully", null));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<Void>> toggleActive(
            @PathVariable String id,
            @RequestParam boolean active) {
        brandService.toggleActive(id, active);
        return ResponseEntity.ok(ApiResponse.success("Brand active status updated", null));
    }

    @PatchMapping("/{id}/featured")
    public ResponseEntity<ApiResponse<Void>> toggleFeatured(
            @PathVariable String id,
            @RequestParam boolean featured) {
        brandService.toggleFeatured(id, featured);
        return ResponseEntity.ok(ApiResponse.success("Brand featured status updated", null));
    }
}
