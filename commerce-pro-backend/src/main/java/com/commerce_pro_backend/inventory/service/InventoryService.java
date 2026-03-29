package com.commerce_pro_backend.inventory.service;

import com.commerce_pro_backend.catalog.product.entity.Product;
import com.commerce_pro_backend.catalog.product.repository.ProductRepository;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.inventory.dto.*;
import com.commerce_pro_backend.inventory.entity.Inventory;
import com.commerce_pro_backend.inventory.entity.StockMovement;
import com.commerce_pro_backend.inventory.entity.Warehouse;
import com.commerce_pro_backend.inventory.mapper.InventoryMapper;
import com.commerce_pro_backend.inventory.repository.InventoryRepository;
import com.commerce_pro_backend.inventory.repository.StockMovementRepository;
import com.commerce_pro_backend.inventory.repository.WarehouseRepository;
import com.commerce_pro_backend.inventory.specification.InventorySpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Inventory Service - Business logic for inventory management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;

    // ==================== WAREHOUSE OPERATIONS ====================

    public List<WarehouseDTO> getAllWarehouses() {
        return inventoryMapper.toWarehouseDTOList(warehouseRepository.findAll());
    }

    public List<WarehouseDTO> getActiveWarehouses() {
        return inventoryMapper.toWarehouseDTOList(warehouseRepository.findByIsActiveTrue());
    }

    public WarehouseDTO getWarehouseById(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Warehouse", id));
        return inventoryMapper.toWarehouseDTO(warehouse);
    }

    @Transactional
    public WarehouseDTO createWarehouse(WarehouseRequestDTO requestDTO) {
        if (requestDTO.getCode() != null && warehouseRepository.existsByCode(requestDTO.getCode())) {
            throw ApiException.conflict("Warehouse with code '" + requestDTO.getCode() + "' already exists");
        }

        Warehouse warehouse = inventoryMapper.toWarehouseEntity(requestDTO);
        
        // If this is set as default, unset any existing default
        if (Boolean.TRUE.equals(warehouse.getIsDefault())) {
            warehouseRepository.findByIsDefaultTrue().ifPresent(existing -> {
                existing.setIsDefault(false);
                warehouseRepository.save(existing);
            });
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Created warehouse: {} with code: {}", saved.getId(), saved.getCode());
        return inventoryMapper.toWarehouseDTO(saved);
    }

    @Transactional
    public WarehouseDTO updateWarehouse(String id, WarehouseRequestDTO requestDTO) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Warehouse", id));

        // Check code uniqueness if changed
        if (requestDTO.getCode() != null && !requestDTO.getCode().equals(warehouse.getCode()) 
                && warehouseRepository.existsByCode(requestDTO.getCode())) {
            throw ApiException.conflict("Warehouse with code '" + requestDTO.getCode() + "' already exists");
        }

        // Handle default flag change
        if (Boolean.TRUE.equals(requestDTO.getIsDefault()) && !Boolean.TRUE.equals(warehouse.getIsDefault())) {
            warehouseRepository.findByIsDefaultTrue().ifPresent(existing -> {
                existing.setIsDefault(false);
                warehouseRepository.save(existing);
            });
        }

        inventoryMapper.updateWarehouseFromDTO(warehouse, requestDTO);
        Warehouse updated = warehouseRepository.save(warehouse);
        log.info("Updated warehouse: {}", updated.getId());
        return inventoryMapper.toWarehouseDTO(updated);
    }

    @Transactional
    public void deleteWarehouse(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Warehouse", id));
        
        if (!warehouse.getInventories().isEmpty()) {
            throw ApiException.conflict("Cannot delete warehouse with inventory items. Please transfer or remove inventory first.");
        }
        
        warehouseRepository.delete(warehouse);
        log.info("Deleted warehouse: {}", id);
    }

    // ==================== INVENTORY OPERATIONS ====================

    public PageResponse<InventoryDTO> getInventory(InventoryFilterDTO filter, Pageable pageable) {
        Specification<Inventory> spec = InventorySpecification.withFilter(filter);
        
        // Apply sorting
        if (filter.getSortBy() != null && !filter.getSortBy().isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(filter.getSortDirection()) 
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), 
                    Sort.by(direction, filter.getSortBy()));
        }

        Page<Inventory> page = inventoryRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(inventoryMapper::toInventoryDTO));
    }

    public InventoryDTO getInventoryById(String id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Inventory", id));
        return inventoryMapper.toInventoryDTO(inventory);
    }

    public InventoryDTO getInventoryByProductAndWarehouse(String productId, String warehouseId) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ApiException("Inventory not found for product and warehouse", HttpStatus.NOT_FOUND));
        return inventoryMapper.toInventoryDTO(inventory);
    }

    public List<InventoryDTO> getInventoryByProduct(String productId) {
        return inventoryMapper.toInventoryDTOList(inventoryRepository.findByProductId(productId));
    }

    public List<InventoryDTO> getInventoryByWarehouse(String warehouseId) {
        return inventoryMapper.toInventoryDTOList(inventoryRepository.findByWarehouseId(warehouseId));
    }

    @Transactional
    public InventoryDTO createInventory(InventoryRequestDTO requestDTO) {
        // Validate product exists
        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> ApiException.notFound("Product", requestDTO.getProductId()));

        // Validate warehouse exists
        Warehouse warehouse = warehouseRepository.findById(requestDTO.getWarehouseId())
                .orElseThrow(() -> ApiException.notFound("Warehouse", requestDTO.getWarehouseId()));

        // Check if inventory already exists
        if (inventoryRepository.existsByProductIdAndWarehouseId(requestDTO.getProductId(), requestDTO.getWarehouseId())) {
            throw ApiException.conflict("Inventory already exists for this product and warehouse");
        }

        Inventory inventory = inventoryMapper.toInventoryEntity(requestDTO, product, warehouse);
        
        // Create initial stock movement
        if (inventory.getQuantity() > 0) {
            StockMovement movement = StockMovement.builder()
                    .inventory(inventory)
                    .product(product)
                    .warehouse(warehouse)
                    .type(StockMovement.MovementType.IN)
                    .quantity(inventory.getQuantity())
                    .previousQuantity(0)
                    .newQuantity(inventory.getQuantity())
                    .reason("Initial stock")
                    .createdBy("system")
                    .build();
            inventory.getMovements().add(movement);
        }

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Created inventory: {} for product: {} in warehouse: {}", 
                saved.getId(), product.getId(), warehouse.getId());
        
        // Update product stock
        updateProductStock(product);
        
        return inventoryMapper.toInventoryDTO(saved);
    }

    @Transactional
    public InventoryDTO updateInventory(String id, InventoryRequestDTO requestDTO) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Inventory", id));

        int oldQuantity = inventory.getQuantity();
        inventoryMapper.updateInventoryFromDTO(inventory, requestDTO);
        inventory.updateStatus();

        Inventory updated = inventoryRepository.save(inventory);
        
        // Update product stock if quantity changed
        if (oldQuantity != updated.getQuantity()) {
            updateProductStock(updated.getProduct());
        }
        
        log.info("Updated inventory: {}", updated.getId());
        return inventoryMapper.toInventoryDTO(updated);
    }

    @Transactional
    public void deleteInventory(String id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Inventory", id));
        
        Product product = inventory.getProduct();
        inventoryRepository.delete(inventory);
        
        // Update product stock
        updateProductStock(product);
        
        log.info("Deleted inventory: {}", id);
    }

    // ==================== STOCK OPERATIONS ====================

    @Transactional
    public InventoryDTO adjustStock(String inventoryId, StockUpdateRequestDTO requestDTO) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> ApiException.notFound("Inventory", inventoryId));

        int newQuantity;
        if (Boolean.TRUE.equals(requestDTO.getAdjust())) {
            newQuantity = inventory.getQuantity() + requestDTO.getQuantity();
        } else {
            newQuantity = requestDTO.getQuantity();
        }

        if (newQuantity < 0) {
            throw ApiException.badRequest("Stock quantity cannot be negative");
        }

        // Create stock movement
        StockMovement movement = StockMovement.builder()
                .inventory(inventory)
                .product(inventory.getProduct())
                .warehouse(inventory.getWarehouse())
                .type(newQuantity > inventory.getQuantity() ? StockMovement.MovementType.IN : StockMovement.MovementType.ADJUSTMENT)
                .quantity(Math.abs(newQuantity - inventory.getQuantity()))
                .previousQuantity(inventory.getQuantity())
                .newQuantity(newQuantity)
                .reason(requestDTO.getReason())
                .notes(requestDTO.getNotes())
                .reference(requestDTO.getReference())
                .referenceType(requestDTO.getReferenceType() != null ? 
                        StockMovement.ReferenceType.valueOf(requestDTO.getReferenceType()) : null)
                .createdBy("system")
                .build();

        inventory.setQuantity(newQuantity);
        inventory.getMovements().add(movement);
        inventory.updateStatus();

        Inventory saved = inventoryRepository.save(inventory);
        
        // Update product stock
        updateProductStock(saved.getProduct());
        
        log.info("Adjusted stock for inventory: {} to {}", saved.getId(), newQuantity);
        return inventoryMapper.toInventoryDTO(saved);
    }

    @Transactional
    public void transferStock(StockTransferRequestDTO requestDTO) {
        // Get source inventory
        Inventory sourceInventory = inventoryRepository.findByProductIdAndWarehouseId(
                requestDTO.getProductId(), requestDTO.getFromWarehouseId())
                .orElseThrow(() -> new ApiException("Source inventory not found", HttpStatus.NOT_FOUND));

        if (sourceInventory.getAvailable() < requestDTO.getQuantity()) {
            throw ApiException.badRequest("Insufficient available stock for transfer");
        }

        // Get or create destination inventory
        Inventory destInventory = inventoryRepository.findByProductIdAndWarehouseId(
                requestDTO.getProductId(), requestDTO.getToWarehouseId())
                .orElseGet(() -> {
                    Warehouse destWarehouse = warehouseRepository.findById(requestDTO.getToWarehouseId())
                            .orElseThrow(() -> ApiException.notFound("Destination warehouse", requestDTO.getToWarehouseId()));
                    
                    Inventory newInv = Inventory.builder()
                            .product(sourceInventory.getProduct())
                            .warehouse(destWarehouse)
                            .quantity(0)
                            .reserved(0)
                            .lowStockThreshold(sourceInventory.getLowStockThreshold())
                            .trackInventory(sourceInventory.getTrackInventory())
                            .build();
                    return inventoryRepository.save(newInv);
                });

        // Create transfer out movement
        StockMovement outMovement = StockMovement.builder()
                .inventory(sourceInventory)
                .product(sourceInventory.getProduct())
                .warehouse(sourceInventory.getWarehouse())
                .type(StockMovement.MovementType.TRANSFER_OUT)
                .quantity(requestDTO.getQuantity())
                .previousQuantity(sourceInventory.getQuantity())
                .newQuantity(sourceInventory.getQuantity() - requestDTO.getQuantity())
                .reason("Transfer to warehouse: " + destInventory.getWarehouse().getName())
                .notes(requestDTO.getNotes())
                .reference(requestDTO.getReference())
                .createdBy("system")
                .build();

        // Create transfer in movement
        StockMovement inMovement = StockMovement.builder()
                .inventory(destInventory)
                .product(destInventory.getProduct())
                .warehouse(destInventory.getWarehouse())
                .type(StockMovement.MovementType.TRANSFER_IN)
                .quantity(requestDTO.getQuantity())
                .previousQuantity(destInventory.getQuantity())
                .newQuantity(destInventory.getQuantity() + requestDTO.getQuantity())
                .reason("Transfer from warehouse: " + sourceInventory.getWarehouse().getName())
                .notes(requestDTO.getNotes())
                .reference(requestDTO.getReference())
                .createdBy("system")
                .build();

        // Update quantities
        sourceInventory.setQuantity(sourceInventory.getQuantity() - requestDTO.getQuantity());
        sourceInventory.getMovements().add(outMovement);
        sourceInventory.updateStatus();

        destInventory.setQuantity(destInventory.getQuantity() + requestDTO.getQuantity());
        destInventory.getMovements().add(inMovement);
        destInventory.updateStatus();

        inventoryRepository.save(sourceInventory);
        inventoryRepository.save(destInventory);

        // Update product stock
        updateProductStock(sourceInventory.getProduct());

        log.info("Transferred {} units of product: {} from warehouse: {} to warehouse: {}",
                requestDTO.getQuantity(), requestDTO.getProductId(), 
                requestDTO.getFromWarehouseId(), requestDTO.getToWarehouseId());
    }

    // ==================== STOCK MOVEMENTS ====================

    public List<StockMovementDTO> getStockMovements(String inventoryId) {
        return inventoryMapper.toStockMovementDTOList(
                stockMovementRepository.findByInventoryIdOrderByCreatedAtDesc(inventoryId));
    }

    public List<StockMovementDTO> getProductStockMovements(String productId) {
        return inventoryMapper.toStockMovementDTOList(
                stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId));
    }

    // ==================== LOW/OUT OF STOCK ====================

    public List<InventoryDTO> getLowStockItems() {
        return inventoryMapper.toInventoryDTOList(inventoryRepository.findLowStock());
    }

    public List<InventoryDTO> getOutOfStockItems() {
        return inventoryMapper.toInventoryDTOList(inventoryRepository.findOutOfStock());
    }

    // ==================== STATISTICS ====================

    public InventoryStatsDTO getInventoryStats() {
        List<Inventory> allInventory = inventoryRepository.findAll();
        
        long inStockCount = allInventory.stream().filter(i -> i.getStatus() == Inventory.StockStatus.IN_STOCK).count();
        long lowStockCount = allInventory.stream().filter(i -> i.getStatus() == Inventory.StockStatus.LOW_STOCK).count();
        long outOfStockCount = allInventory.stream().filter(i -> i.getStatus() == Inventory.StockStatus.OUT_OF_STOCK).count();
        long overstockCount = allInventory.stream().filter(i -> i.getStatus() == Inventory.StockStatus.OVERSTOCK).count();
        long notTrackedCount = allInventory.stream().filter(i -> i.getStatus() == Inventory.StockStatus.NOT_TRACKED).count();

        BigDecimal totalValue = allInventory.stream()
                .map(Inventory::getTotalValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalUnits = allInventory.stream().mapToInt(Inventory::getQuantity).sum();
        long totalReserved = allInventory.stream().mapToInt(Inventory::getReserved).sum();
        long totalAvailable = allInventory.stream().mapToInt(Inventory::getAvailable).sum();
        long totalIncoming = allInventory.stream().mapToInt(Inventory::getIncoming).sum();

        // Calculate average unit cost
        BigDecimal avgUnitCost = totalUnits > 0 
                ? totalValue.divide(BigDecimal.valueOf(totalUnits), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Status breakdown
        Map<String, Long> statusBreakdown = new HashMap<>();
        statusBreakdown.put("IN_STOCK", inStockCount);
        statusBreakdown.put("LOW_STOCK", lowStockCount);
        statusBreakdown.put("OUT_OF_STOCK", outOfStockCount);
        statusBreakdown.put("OVERSTOCK", overstockCount);
        statusBreakdown.put("NOT_TRACKED", notTrackedCount);

        // Warehouse breakdown
        Map<String, InventoryStatsDTO.WarehouseStats> warehouseBreakdown = allInventory.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getWarehouse().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> InventoryStatsDTO.WarehouseStats.builder()
                                        .warehouseName(list.get(0).getWarehouse().getName())
                                        .itemCount(list.size())
                                        .totalUnits(list.stream().mapToInt(Inventory::getQuantity).sum())
                                        .totalValue(list.stream()
                                                .map(Inventory::getTotalValue)
                                                .filter(Objects::nonNull)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                                        .build()
                        )
                ));

        return InventoryStatsDTO.builder()
                .totalItems(allInventory.size())
                .totalProducts(allInventory.stream().map(i -> i.getProduct().getId()).distinct().count())
                .totalWarehouses(warehouseRepository.count())
                .inStockCount(inStockCount)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .overstockCount(overstockCount)
                .notTrackedCount(notTrackedCount)
                .totalInventoryValue(totalValue)
                .totalUnits(totalUnits)
                .totalReserved(totalReserved)
                .totalAvailable(totalAvailable)
                .totalIncoming(totalIncoming)
                .averageUnitCost(avgUnitCost)
                .statusBreakdown(statusBreakdown)
                .warehouseBreakdown(warehouseBreakdown)
                .build();
    }

    // ==================== PRIVATE HELPERS ====================

    private void updateProductStock(Product product) {
        Integer totalStock = inventoryRepository.getTotalQuantityByProductId(product.getId());
        product.setStock(totalStock != null ? totalStock : 0);
        product.updateStockStatus();
        productRepository.save(product);
    }

    /**
     * Execute a batch stock transfer — multiple product lines between the same two warehouses.
     * Each line calls the existing single-product transferStock logic transactionally.
     */
    @Transactional
    public void batchTransferStock(BatchStockTransferRequestDTO request) {
        for (BatchStockTransferRequestDTO.TransferLineDTO line : request.getLines()) {
            StockTransferRequestDTO single = StockTransferRequestDTO.builder()
                    .fromWarehouseId(request.getFromWarehouseId())
                    .toWarehouseId(request.getToWarehouseId())
                    .productId(line.getProductId())
                    .quantity(line.getQuantity())
                    .notes(line.getNotes() != null ? line.getNotes() : request.getNotes())
                    .reference(request.getReference())
                    .build();
            transferStock(single);
        }
    }

}