package com.commerce_pro_backend.inventory.valuation.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.inventory.valuation.dto.InventoryValuationDto;
import com.commerce_pro_backend.inventory.valuation.entity.CostLayer;
import com.commerce_pro_backend.inventory.valuation.entity.InventoryValuation;
import com.commerce_pro_backend.inventory.valuation.repository.CostLayerRepository;
import com.commerce_pro_backend.inventory.valuation.repository.InventoryValuationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for Inventory Valuation.
 * Supports FIFO, LIFO, Weighted Average, and Specific Identification costing methods.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryValuationService {

    private final InventoryValuationRepository valuationRepository;
    private final CostLayerRepository costLayerRepository;

    // ==================== READ OPERATIONS ====================

    public PageResponse<InventoryValuationDto.Response> getAllValuations(Pageable pageable) {
        Page<InventoryValuation> page = valuationRepository.findAll(pageable);
        return PageResponse.from(page, page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    public InventoryValuationDto.Response getValuationById(String id) {
        InventoryValuation valuation = findOrThrow(id);
        return toResponseWithLayers(valuation);
    }

    public List<InventoryValuationDto.Response> getValuationsByProduct(String productId) {
        return valuationRepository.findByProductId(productId).stream()
                .map(this::toResponseWithLayers)
                .collect(Collectors.toList());
    }

    public List<InventoryValuationDto.Response> getValuationsByWarehouse(String warehouseId) {
        return valuationRepository.findByWarehouseId(warehouseId).stream()
                .map(this::toResponseWithLayers)
                .collect(Collectors.toList());
    }

    // ==================== WRITE OPERATIONS ====================

    @Transactional
    public InventoryValuationDto.Response createValuation(InventoryValuationDto.Request request) {
        // Prevent duplicate valuations for the same product+warehouse
        if (request.getWarehouseId() != null
                && valuationRepository.existsByProductIdAndWarehouseId(
                        request.getProductId(), request.getWarehouseId())) {
            throw ApiException.conflict(
                    "Valuation already exists for product '" + request.getProductId()
                    + "' in warehouse '" + request.getWarehouseId() + "'");
        }

        InventoryValuation valuation = InventoryValuation.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .sku(request.getSku())
                .warehouseId(request.getWarehouseId())
                .warehouseName(request.getWarehouseName())
                .valuationMethod(request.getValuationMethod())
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .totalQuantity(request.getTotalQuantity() != null ? request.getTotalQuantity() : 0)
                .averageUnitCost(request.getAverageUnitCost() != null ? request.getAverageUnitCost() : BigDecimal.ZERO)
                .totalValue(request.getTotalValue() != null ? request.getTotalValue() : BigDecimal.ZERO)
                .calculatedAt(Instant.now())
                .calculatedBy(request.getCalculatedBy())
                .build();

        final InventoryValuation saved = valuationRepository.save(valuation);

        // Persist seed cost layers if provided
        if (request.getCostLayers() != null && !request.getCostLayers().isEmpty()) {
            List<CostLayer> layers = request.getCostLayers().stream()
                    .map(lr -> buildCostLayer(saved, lr))
                    .collect(Collectors.toList());
            costLayerRepository.saveAll(layers);
            saved.getCostLayers().addAll(layers);

            // Derive totals from the seed layers
            recalculateFromLayers(saved);
            valuationRepository.save(saved);
        }

        log.info("Created inventory valuation: {} for product: {}", saved.getId(), saved.getProductId());
        return toResponseWithLayers(saved);
    }

    @Transactional
    public InventoryValuationDto.Response updateValuation(String id, InventoryValuationDto.Request request) {
        InventoryValuation valuation = findOrThrow(id);

        if (request.getProductName() != null) valuation.setProductName(request.getProductName());
        if (request.getSku() != null) valuation.setSku(request.getSku());
        if (request.getWarehouseName() != null) valuation.setWarehouseName(request.getWarehouseName());
        if (request.getValuationMethod() != null) valuation.setValuationMethod(request.getValuationMethod());
        if (request.getStatus() != null) valuation.setStatus(request.getStatus());
        if (request.getTotalQuantity() != null) valuation.setTotalQuantity(request.getTotalQuantity());
        if (request.getAverageUnitCost() != null) valuation.setAverageUnitCost(request.getAverageUnitCost());
        if (request.getTotalValue() != null) valuation.setTotalValue(request.getTotalValue());
        if (request.getCalculatedBy() != null) valuation.setCalculatedBy(request.getCalculatedBy());

        valuation.setLastAdjustmentAt(Instant.now());

        InventoryValuation updated = valuationRepository.save(valuation);
        log.info("Updated inventory valuation: {}", updated.getId());
        return toResponseWithLayers(updated);
    }

    @Transactional
    public void deleteValuation(String id) {
        InventoryValuation valuation = findOrThrow(id);
        costLayerRepository.deleteByValuationId(id);
        valuationRepository.delete(valuation);
        log.info("Deleted inventory valuation: {}", id);
    }

    // ==================== RECALCULATION ====================

    /**
     * Recalculates one or more valuations.
     * When {@code request.valuationIds} is null/empty every active valuation is processed.
     *
     * @return list of recalculated responses
     */
    @Transactional
    public List<InventoryValuationDto.Response> recalculateValuations(
            InventoryValuationDto.RecalculateRequest request) {

        List<InventoryValuation> targets;
        if (request.getValuationIds() != null && !request.getValuationIds().isEmpty()) {
            targets = valuationRepository.findAllById(request.getValuationIds());
        } else {
            targets = valuationRepository.findByStatus("active");
        }

        List<InventoryValuationDto.Response> results = new ArrayList<>();
        for (InventoryValuation valuation : targets) {
            String method = request.getValuationMethod() != null
                    ? request.getValuationMethod()
                    : valuation.getValuationMethod();

            recalculate(valuation, method);
            if (request.getRecalculatedBy() != null) {
                valuation.setCalculatedBy(request.getRecalculatedBy());
            }
            InventoryValuation saved = valuationRepository.save(valuation);
            results.add(toResponseWithLayers(saved));
        }

        log.info("Recalculated {} inventory valuations", results.size());
        return results;
    }

    // ==================== CHANGE METHOD ====================

    @Transactional
    public InventoryValuationDto.Response changeValuationMethod(
            String id, InventoryValuationDto.ChangeMethodRequest request) {

        InventoryValuation valuation = findOrThrow(id);
        String oldMethod = valuation.getValuationMethod();

        valuation.setValuationMethod(request.getNewMethod());
        if (request.getChangedBy() != null) {
            valuation.setCalculatedBy(request.getChangedBy());
        }
        valuation.setLastAdjustmentAt(Instant.now());

        // Recalculate using the new method
        recalculate(valuation, request.getNewMethod());

        InventoryValuation saved = valuationRepository.save(valuation);
        log.info("Changed valuation method for {}: {} -> {}", id, oldMethod, request.getNewMethod());
        return toResponseWithLayers(saved);
    }

    // ==================== SUMMARY REPORT ====================

    public InventoryValuationDto.SummaryResponse getSummary() {
        long totalValuations = valuationRepository.findByStatus("active").size();
        long totalProducts = valuationRepository.countDistinctActiveProducts();
        long totalWarehouses = valuationRepository.countDistinctActiveWarehouses();

        BigDecimal totalValue = valuationRepository.getTotalActiveInventoryValue();
        if (totalValue == null) totalValue = BigDecimal.ZERO;

        // By method breakdown
        List<Object[]> methodRows = valuationRepository.getBreakdownByMethod();
        Map<String, InventoryValuationDto.SummaryResponse.MethodBreakdown> byMethod = new LinkedHashMap<>();
        for (Object[] row : methodRows) {
            String method = (String) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal val = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            byMethod.put(method, InventoryValuationDto.SummaryResponse.MethodBreakdown.builder()
                    .method(method)
                    .count(count)
                    .totalValue(val)
                    .build());
        }

        // By warehouse breakdown
        List<Object[]> warehouseRows = valuationRepository.getBreakdownByWarehouse();
        Map<String, InventoryValuationDto.SummaryResponse.WarehouseBreakdown> byWarehouse = new LinkedHashMap<>();
        for (Object[] row : warehouseRows) {
            String warehouseId = (String) row[0];
            String warehouseName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            BigDecimal val = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
            byWarehouse.put(warehouseId, InventoryValuationDto.SummaryResponse.WarehouseBreakdown.builder()
                    .warehouseId(warehouseId)
                    .warehouseName(warehouseName)
                    .count(count)
                    .totalValue(val)
                    .build());
        }

        return InventoryValuationDto.SummaryResponse.builder()
                .totalValuations(totalValuations)
                .totalProducts(totalProducts)
                .totalWarehouses(totalWarehouses)
                .totalInventoryValue(totalValue)
                .byMethod(byMethod)
                .byWarehouse(byWarehouse)
                .generatedAt(Instant.now())
                .build();
    }

    // ==================== PRIVATE HELPERS ====================

    private InventoryValuation findOrThrow(String id) {
        return valuationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("InventoryValuation", id));
    }

    /**
     * Core recalculation dispatcher – delegates to method-specific logic.
     */
    private void recalculate(InventoryValuation valuation, String method) {
        List<CostLayer> layers = costLayerRepository
                .findByValuationIdOrderByReceiptDateAsc(valuation.getId());

        switch (method.toLowerCase()) {
            case "fifo" -> recalculateFifo(valuation, layers);
            case "lifo" -> recalculateLifo(valuation, layers);
            case "weighted_average" -> recalculateWeightedAverage(valuation, layers);
            case "specific_identification" -> recalculateSpecificIdentification(valuation, layers);
            default -> throw ApiException.badRequest("Unsupported valuation method: " + method);
        }

        valuation.setCalculatedAt(Instant.now());
    }

    /**
     * FIFO – oldest layers are consumed first.
     * The remaining stock value equals the cost of the most recently received layers.
     */
    private void recalculateFifo(InventoryValuation valuation, List<CostLayer> layers) {
        // layers is already sorted oldest-first
        computeLayerTotals(valuation, layers);
    }

    /**
     * LIFO – most-recently received layers are consumed first.
     * The remaining stock value equals the cost of the oldest layers still on hand.
     */
    private void recalculateLifo(InventoryValuation valuation, List<CostLayer> layers) {
        // Reverse to newest-first for LIFO consumption, then compute
        List<CostLayer> reversed = new ArrayList<>(layers);
        Collections.reverse(reversed);
        computeLayerTotals(valuation, reversed);
    }

    /**
     * Weighted Average – all units carry the same average cost.
     */
    private void recalculateWeightedAverage(InventoryValuation valuation, List<CostLayer> layers) {
        int totalQty = layers.stream().mapToInt(CostLayer::getRemainingQuantity).sum();
        BigDecimal totalCost = layers.stream()
                .map(CostLayer::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgCost = totalQty > 0
                ? totalCost.divide(BigDecimal.valueOf(totalQty), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        valuation.setTotalQuantity(totalQty);
        valuation.setAverageUnitCost(avgCost);
        valuation.setTotalValue(avgCost.multiply(BigDecimal.valueOf(totalQty))
                .setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * Specific Identification – each unit retains its actual purchase cost.
     * The total value is the sum of (remainingQty * unitCost) for every layer.
     */
    private void recalculateSpecificIdentification(InventoryValuation valuation, List<CostLayer> layers) {
        int totalQty = layers.stream().mapToInt(CostLayer::getRemainingQuantity).sum();
        BigDecimal totalVal = layers.stream()
                .map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getRemainingQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal avgCost = totalQty > 0
                ? totalVal.divide(BigDecimal.valueOf(totalQty), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        valuation.setTotalQuantity(totalQty);
        valuation.setAverageUnitCost(avgCost);
        valuation.setTotalValue(totalVal);
    }

    /**
     * Shared helper that sums remaining quantities and total costs across the
     * (potentially reordered) layer list and updates the valuation entity.
     */
    private void computeLayerTotals(InventoryValuation valuation, List<CostLayer> layers) {
        int totalQty = layers.stream().mapToInt(CostLayer::getRemainingQuantity).sum();
        BigDecimal totalVal = layers.stream()
                .map(CostLayer::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal avgCost = totalQty > 0
                ? totalVal.divide(BigDecimal.valueOf(totalQty), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        valuation.setTotalQuantity(totalQty);
        valuation.setAverageUnitCost(avgCost);
        valuation.setTotalValue(totalVal);
    }

    /**
     * Re-derives valuation totals from the in-memory cost layer list
     * (used immediately after saving seed layers at creation time).
     */
    private void recalculateFromLayers(InventoryValuation valuation) {
        recalculate(valuation, valuation.getValuationMethod());
    }

    /** Builds a CostLayer entity from a request DTO. */
    private CostLayer buildCostLayer(InventoryValuation valuation,
                                     InventoryValuationDto.CostLayerRequest lr) {
        BigDecimal unitCost = lr.getUnitCost() != null ? lr.getUnitCost() : BigDecimal.ZERO;
        int qty = lr.getOriginalQuantity() != null ? lr.getOriginalQuantity() : 0;
        BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(qty))
                .setScale(4, RoundingMode.HALF_UP);

        return CostLayer.builder()
                .valuation(valuation)
                .receiptDate(lr.getReceiptDate() != null ? lr.getReceiptDate() : Instant.now())
                .unitCost(unitCost)
                .originalQuantity(qty)
                .remainingQuantity(qty)
                .totalCost(totalCost)
                .reference(lr.getReference())
                .expiryDate(lr.getExpiryDate())
                .build();
    }

    // ==================== MAPPERS ====================

    /** Maps entity to response without loading cost layers (used for list endpoints). */
    private InventoryValuationDto.Response toResponse(InventoryValuation v) {
        return InventoryValuationDto.Response.builder()
                .id(v.getId())
                .productId(v.getProductId())
                .productName(v.getProductName())
                .sku(v.getSku())
                .warehouseId(v.getWarehouseId())
                .warehouseName(v.getWarehouseName())
                .valuationMethod(v.getValuationMethod())
                .status(v.getStatus())
                .totalQuantity(v.getTotalQuantity())
                .averageUnitCost(v.getAverageUnitCost())
                .totalValue(v.getTotalValue())
                .calculatedAt(v.getCalculatedAt())
                .calculatedBy(v.getCalculatedBy())
                .lastAdjustmentAt(v.getLastAdjustmentAt())
                .createdAt(v.getCreatedAt())
                .build();
    }

    /** Maps entity to response including cost layers (used for detail endpoints). */
    private InventoryValuationDto.Response toResponseWithLayers(InventoryValuation v) {
        List<CostLayer> layers = costLayerRepository
                .findByValuationIdOrderByReceiptDateAsc(v.getId());

        List<InventoryValuationDto.CostLayerResponse> layerResponses = layers.stream()
                .map(l -> InventoryValuationDto.CostLayerResponse.builder()
                        .id(l.getId())
                        .valuationId(v.getId())
                        .receiptDate(l.getReceiptDate())
                        .unitCost(l.getUnitCost())
                        .originalQuantity(l.getOriginalQuantity())
                        .remainingQuantity(l.getRemainingQuantity())
                        .totalCost(l.getTotalCost())
                        .reference(l.getReference())
                        .expiryDate(l.getExpiryDate())
                        .build())
                .collect(Collectors.toList());

        InventoryValuationDto.Response response = toResponse(v);
        response.setCostLayers(layerResponses);
        return response;
    }
}
