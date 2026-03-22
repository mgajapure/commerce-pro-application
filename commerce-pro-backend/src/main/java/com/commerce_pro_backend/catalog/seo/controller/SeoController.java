package com.commerce_pro_backend.catalog.seo.controller;

import com.commerce_pro_backend.catalog.seo.dto.SeoDto;
import com.commerce_pro_backend.catalog.seo.service.SeoService;
import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/seo")
@RequiredArgsConstructor
public class SeoController {

    private final SeoService seoService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SeoDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(seoService.getAllSeoEntries(pageable)));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<SeoDto.Response>> getSeoForEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(ApiResponse.success(seoService.getSeoForEntity(entityType, entityId)));
    }

    @PutMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<SeoDto.Response>> upsertSeoForEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @Valid @RequestBody SeoDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success(seoService.upsertSeoForEntity(entityType, entityId, request)));
    }

    @DeleteMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<String>> deleteSeoForEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        seoService.deleteSeoForEntity(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("SEO metadata deleted successfully"));
    }

    @PostMapping("/audit/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<SeoDto.SeoAuditResult>> auditSeo(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(ApiResponse.success(seoService.auditSeoForEntity(entityType, entityId)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SeoDto.StatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(seoService.getStats()));
    }
}
