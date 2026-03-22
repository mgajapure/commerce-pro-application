package com.commerce_pro_backend.catalog.attribute.controller;

import com.commerce_pro_backend.catalog.attribute.dto.AttributeDto;
import com.commerce_pro_backend.catalog.attribute.service.AttributeService;
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
@RequestMapping("/v1/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AttributeDto.ListResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(attributeService.getAllAttributes(pageable)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<AttributeDto.Response>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(attributeService.getAllActiveAttributes()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AttributeDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(attributeService.getAttribute(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AttributeDto.Response>> create(
            @Valid @RequestBody AttributeDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(attributeService.createAttribute(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AttributeDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody AttributeDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success(attributeService.updateAttribute(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable String id) {
        attributeService.deleteAttribute(id);
        return ResponseEntity.ok(ApiResponse.success("Attribute deleted successfully"));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<String>> toggleActive(
            @PathVariable String id,
            @RequestParam boolean active) {
        attributeService.toggleActive(id, active);
        return ResponseEntity.ok(ApiResponse.success("Active status updated"));
    }
}
