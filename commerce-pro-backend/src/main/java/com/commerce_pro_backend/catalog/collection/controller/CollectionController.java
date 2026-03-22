package com.commerce_pro_backend.catalog.collection.controller;

import com.commerce_pro_backend.catalog.collection.dto.CollectionDto;
import com.commerce_pro_backend.catalog.collection.service.CollectionService;
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
@RequestMapping("/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CollectionDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getAllCollections(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CollectionDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getCollection(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CollectionDto.Response>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getCollectionBySlug(slug)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<CollectionDto.ListResponse>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getFeaturedCollections()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollectionDto.Response>> create(
            @Valid @RequestBody CollectionDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(collectionService.createCollection(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CollectionDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody CollectionDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.updateCollection(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable String id) {
        collectionService.deleteCollection(id);
        return ResponseEntity.ok(ApiResponse.success("Collection deleted successfully"));
    }

    @PostMapping("/{id}/products")
    public ResponseEntity<ApiResponse<CollectionDto.Response>> addProducts(
            @PathVariable String id,
            @Valid @RequestBody CollectionDto.ProductIdsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.addProducts(id, request.getProductIds())));
    }

    @DeleteMapping("/{id}/products")
    public ResponseEntity<ApiResponse<CollectionDto.Response>> removeProducts(
            @PathVariable String id,
            @Valid @RequestBody CollectionDto.ProductIdsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.removeProducts(id, request.getProductIds())));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CollectionDto.StatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getStats()));
    }
}
