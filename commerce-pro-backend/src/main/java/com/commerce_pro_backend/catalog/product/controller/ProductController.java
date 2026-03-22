package com.commerce_pro_backend.catalog.product.controller;

import com.commerce_pro_backend.catalog.product.dto.*;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product REST Controller
 * RESTful API endpoints for Product management
 * 
 * Base URL: /api/products
 */
@Slf4j
@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    // ==================== CRUD OPERATIONS ====================

    /**
     * Get all products with pagination and filtering
     * GET /api/products
     */
    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve paginated list of products with optional filtering")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryDTO>>> getProducts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "stockStatus", required = false) String stockStatus,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "minRating", required = false) Integer minRating,
            @RequestParam(name = "featured", required = false) Boolean featured,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDirection", required = false, defaultValue = "asc") String sortDirection,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        ProductFilterDTO filter = ProductFilterDTO.builder()
                .searchQuery(search)
                .status(status)
                .category(category)
                .stockStatus(stockStatus)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .featured(featured)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<ProductSummaryDTO> productPage = productService.getProducts(filter, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(
                "Products retrieved successfully", 
                PageResponse.from(productPage)
        ));
    }

    /**
     * Get product by ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a single product by its unique identifier")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductById(
            @PathVariable(name = "id") String id) {
        
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    /**
     * Create new product
     * POST /api/products
     */
    @PostMapping
    @Operation(summary = "Create product", description = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        
        ProductResponseDTO createdProduct = productService.createProduct(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", createdProduct));
    }

    /**
     * Update product (full update)
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(
            @PathVariable(name = "id") String id,
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        
        ProductResponseDTO updatedProduct = productService.updateProduct(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
    }

    /**
     * Partial update product
     * PATCH /api/products/{id}
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Patch product", description = "Partially update a product")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> patchProduct(
            @PathVariable(name = "id") String id,
            @RequestBody Map<String, Object> updates) {
        
        ProductResponseDTO updatedProduct = productService.patchProduct(id, updates);
        return ResponseEntity.ok(ApiResponse.success("Product patched successfully", updatedProduct));
    }

    /**
     * Delete product
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Delete a product by ID")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable(name = "id") String id) {
        
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    // ==================== SEARCH & FILTER ====================

    /**
     * Search products
     * GET /api/products/search?query={query}
     */
    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by name, SKU, or brand")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryDTO>>> searchProducts(
            @RequestParam(name = "query") String query,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ProductSummaryDTO> productPage = productService.searchProducts(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                "Search completed", 
                PageResponse.from(productPage)
        ));
    }

    /**
     * Get products by category
     * GET /api/products/category/{category}
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieve products filtered by category")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryDTO>>> getProductsByCategory(
            @PathVariable(name = "category") String category,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ProductSummaryDTO> productPage = productService.getProductsByCategory(category, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                "Products retrieved successfully", 
                PageResponse.from(productPage)
        ));
    }

    /**
     * Get featured products
     * GET /api/products/featured
     */
    @GetMapping("/featured")
    @Operation(summary = "Get featured products", description = "Retrieve featured products")
    public ResponseEntity<ApiResponse<List<ProductSummaryDTO>>> getFeaturedProducts(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        
        List<ProductSummaryDTO> products = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Featured products retrieved", products));
    }

    // ==================== STOCK MANAGEMENT ====================

    /**
     * Update product stock
     * POST /api/products/{id}/stock
     */
    @PostMapping("/{id}/stock")
    @Operation(summary = "Update stock", description = "Update product stock quantity")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateStock(
            @PathVariable(name = "id") String id,
            @Valid @RequestBody StockUpdateDTO stockUpdate) {
        
        ProductResponseDTO updatedProduct = productService.updateStock(id, stockUpdate);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", updatedProduct));
    }

    /**
     * Get low stock products
     * GET /api/products/stock/low
     */
    @GetMapping("/stock/low")
    @Operation(summary = "Get low stock products", description = "Retrieve products with low stock")
    public ResponseEntity<ApiResponse<List<ProductSummaryDTO>>> getLowStockProducts() {
        
        List<ProductSummaryDTO> products = productService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success("Low stock products retrieved", products));
    }

    /**
     * Get out of stock products
     * GET /api/products/stock/out-of-stock
     */
    @GetMapping("/stock/out-of-stock")
    @Operation(summary = "Get out of stock products", description = "Retrieve out of stock products")
    public ResponseEntity<ApiResponse<List<ProductSummaryDTO>>> getOutOfStockProducts() {
        
        List<ProductSummaryDTO> products = productService.getOutOfStockProducts();
        return ResponseEntity.ok(ApiResponse.success("Out of stock products retrieved", products));
    }

    // ==================== DASHBOARD & STATS ====================

    /**
     * Get top selling products
     * GET /api/products/dashboard/top-selling
     */
    @GetMapping("/dashboard/top-selling")
    @Operation(summary = "Get top selling products", description = "Retrieve top selling products for dashboard")
    public ResponseEntity<ApiResponse<List<ProductDashboardDTO>>> getTopSellingProducts(
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        
        List<ProductDashboardDTO> products = productService.getTopSellingProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Top selling products retrieved", products));
    }

    /**
     * Get top revenue products
     * GET /api/products/dashboard/top-revenue
     */
    @GetMapping("/dashboard/top-revenue")
    @Operation(summary = "Get top revenue products", description = "Retrieve top revenue products for dashboard")
    public ResponseEntity<ApiResponse<List<ProductDashboardDTO>>> getTopRevenueProducts(
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        
        List<ProductDashboardDTO> products = productService.getTopRevenueProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Top revenue products retrieved", products));
    }

    /**
     * Get product statistics
     * GET /api/products/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Get product statistics", description = "Retrieve product statistics and counts")
    public ResponseEntity<ApiResponse<ProductStatsDTO>> getProductStats() {
        
        ProductStatsDTO stats = productService.getProductStats();
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", stats));
    }

    // ==================== VALIDATION & REFERENCE ====================

    /**
     * Check if SKU exists
     * GET /api/products/validate/sku?sku={sku}
     */
    @GetMapping("/validate/sku")
    @Operation(summary = "Check SKU availability", description = "Check if a SKU is already in use")
    public ResponseEntity<ApiResponse<Boolean>> checkSkuExists(
            @RequestParam(name = "sku") String sku,
            @RequestParam(name = "excludeId", required = false) String excludeId) {
        
        boolean exists = excludeId != null 
                ? productService.skuExistsForOtherProduct(sku, excludeId)
                : productService.skuExists(sku);
        
        return ResponseEntity.ok(ApiResponse.success("SKU check completed", exists));
    }

    /**
     * Get all brands
     * GET /api/products/reference/brands
     */
    @GetMapping("/reference/brands")
    @Operation(summary = "Get all brands", description = "Retrieve list of all unique brands")
    public ResponseEntity<ApiResponse<List<String>>> getAllBrands() {
        
        List<String> brands = productService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.success("Brands retrieved", brands));
    }

    /**
     * Get all tags
     * GET /api/products/reference/tags
     */
    @GetMapping("/reference/tags")
    @Operation(summary = "Get all tags", description = "Retrieve list of all unique tags")
    public ResponseEntity<ApiResponse<List<String>>> getAllTags() {
        
        List<String> tags = productService.getAllTags();
        return ResponseEntity.ok(ApiResponse.success("Tags retrieved", tags));
    }

    /**
     * Bulk delete products
     * POST /api/products/bulk-delete
     */
    @PostMapping("/bulk-delete")
    @Operation(summary = "Bulk delete products", description = "Delete multiple products by IDs")
    public ResponseEntity<ApiResponse<Void>> bulkDeleteProducts(
            @RequestBody List<String> ids) {
        
        for (String id : ids) {
            productService.deleteProduct(id);
        }
        return ResponseEntity.ok(ApiResponse.success("Products deleted successfully", null));
    }

    /**
     * Bulk update status
     * POST /api/products/bulk-status
     */
    @PostMapping("/bulk-status")
    @Operation(summary = "Bulk update status", description = "Update status for multiple products")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateStatus(
            @RequestBody Map<String, Object> request) {
        
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) request.get("ids");
        String status = (String) request.get("status");
        
        for (String id : ids) {
            productService.patchProduct(id, Map.of("status", status));
        }
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", null));
    }
}
