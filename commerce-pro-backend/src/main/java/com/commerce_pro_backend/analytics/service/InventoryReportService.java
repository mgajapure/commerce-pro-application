package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.InventoryReportDTO;
import com.commerce_pro_backend.analytics.dto.InventoryReportDTO.*;
import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import com.commerce_pro_backend.inventory.entity.Inventory;
import com.commerce_pro_backend.inventory.entity.StockMovement;
import com.commerce_pro_backend.inventory.repository.InventoryRepository;
import com.commerce_pro_backend.inventory.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryReportService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    // ── Stock Value ───────────────────────────────────────────────────────────

    public InventoryReportDTO getStockValue(ReportFilterDTO filter) {
        List<Inventory> all = filter.getWarehouseIds() != null && !filter.getWarehouseIds().isEmpty()
                ? inventoryRepository.findByWarehouseIdIn(filter.getWarehouseIds())
                : inventoryRepository.findAll();

        BigDecimal totalValue = all.stream()
                .map(i -> i.getTotalValue() != null ? i.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long inStock   = all.stream().filter(i -> i.getStatus() == Inventory.StockStatus.IN_STOCK).count();
        long lowStock  = all.stream().filter(i -> i.getStatus() == Inventory.StockStatus.LOW_STOCK).count();
        long outStock  = all.stream().filter(i -> i.getStatus() == Inventory.StockStatus.OUT_OF_STOCK).count();
        long overStock = all.stream().filter(i -> i.getStatus() == Inventory.StockStatus.OVERSTOCK).count();

        BigDecimal avgCost = all.isEmpty() ? BigDecimal.ZERO
                : all.stream().filter(i -> i.getUnitCost() != null)
                    .map(Inventory::getUnitCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(Math.max(1, all.stream().filter(i -> i.getUnitCost() != null).count())), 4, RoundingMode.HALF_UP);

        List<StockValueRowDTO> rows = all.stream()
                .sorted(Comparator.comparing(i -> i.getTotalValue() != null ? i.getTotalValue() : BigDecimal.ZERO,
                        Comparator.reverseOrder()))
                .map(i -> StockValueRowDTO.builder()
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .sku(i.getProduct().getSku())
                        .category(i.getProduct().getCategory())
                        .warehouseName(i.getWarehouse().getName())
                        .quantityOnHand(i.getQuantity())
                        .reserved(i.getReserved())
                        .available(i.getAvailable())
                        .unitCost(i.getUnitCost())
                        .totalValue(i.getTotalValue())
                        .stockStatus(i.getStatus().name())
                        .binLocation(i.getBinLocation())
                        .build())
                .toList();

        return InventoryReportDTO.builder()
                .totalStockValue(totalValue)
                .totalSkus((long) all.size())
                .inStockSkus(inStock)
                .lowStockSkus(lowStock)
                .outOfStockSkus(outStock)
                .overstockSkus(overStock)
                .averageUnitCost(avgCost)
                .stockValueRows(rows)
                .build();
    }

    // ── Movement ─────────────────────────────────────────────────────────────

    public InventoryReportDTO getMovement(ReportFilterDTO filter) {
        LocalDateTime[] range = DateRangeHelper.resolve(filter);

        List<StockMovement> movements = filter.getWarehouseIds() != null && !filter.getWarehouseIds().isEmpty()
                ? stockMovementRepository.findByWarehouseIdInAndCreatedAtBetween(filter.getWarehouseIds(), range[0], range[1])
                : stockMovementRepository.findByCreatedAtBetween(range[0], range[1]);

        long inbound     = movements.stream().filter(m -> m.getType() == StockMovement.MovementType.IN || m.getType() == StockMovement.MovementType.TRANSFER_IN).count();
        long outbound    = movements.stream().filter(m -> m.getType() == StockMovement.MovementType.OUT || m.getType() == StockMovement.MovementType.TRANSFER_OUT).count();
        long adjustments = movements.stream().filter(m -> m.getType() == StockMovement.MovementType.ADJUSTMENT).count();
        long returns     = movements.stream().filter(m -> m.getType() == StockMovement.MovementType.RETURN).count();

        List<StockMovementRowDTO> rows = movements.stream()
                .sorted(Comparator.comparing(StockMovement::getCreatedAt).reversed())
                .map(m -> StockMovementRowDTO.builder()
                        .movementId(m.getId())
                        .productName(m.getProduct().getName())
                        .sku(m.getProduct().getSku())
                        .warehouseName(m.getWarehouse().getName())
                        .movementType(m.getType().name())
                        .quantity(m.getQuantity())
                        .previousQuantity(m.getPreviousQuantity())
                        .newQuantity(m.getNewQuantity())
                        .reason(m.getReason())
                        .reference(m.getReference())
                        .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                        .createdBy(m.getCreatedBy())
                        .build())
                .toList();

        return InventoryReportDTO.builder()
                .totalMovements((long) movements.size())
                .inboundMovements(inbound)
                .outboundMovements(outbound)
                .adjustmentMovements(adjustments)
                .returnMovements(returns)
                .movementRows(rows)
                .build();
    }

    // ── Ageing ────────────────────────────────────────────────────────────────

    public InventoryReportDTO getAgeing(ReportFilterDTO filter) {
        List<Inventory> all = filter.getWarehouseIds() != null && !filter.getWarehouseIds().isEmpty()
                ? inventoryRepository.findByWarehouseIdIn(filter.getWarehouseIds())
                : inventoryRepository.findAll();

        LocalDate today = LocalDate.now();

        List<StockAgeingRowDTO> rows = all.stream()
                .filter(i -> i.getQuantity() > 0)
                .map(i -> {
                    // Last restocked date (prefer lastRestocked, fallback to createdAt)
                    LocalDate lastMoved = i.getLastRestocked() != null
                            ? i.getLastRestocked().toLocalDate()
                            : (i.getCreatedAt() != null ? i.getCreatedAt().toLocalDate() : today);

                    long days = ChronoUnit.DAYS.between(lastMoved, today);
                    String bucket = days <= 30 ? "0-30"
                            : days <= 60 ? "31-60"
                            : days <= 90 ? "61-90"
                            : "90+";

                    return StockAgeingRowDTO.builder()
                            .productId(i.getProduct().getId())
                            .productName(i.getProduct().getName())
                            .sku(i.getProduct().getSku())
                            .warehouseName(i.getWarehouse().getName())
                            .quantityOnHand(i.getQuantity())
                            .totalValue(i.getTotalValue())
                            .daysSinceLastMovement((int) days)
                            .ageingBucket(bucket)
                            .lastMovementDate(lastMoved.toString())
                            .build();
                })
                .sorted(Comparator.comparingInt(StockAgeingRowDTO::getDaysSinceLastMovement).reversed())
                .toList();

        return InventoryReportDTO.builder()
                .totalSkus((long) all.size())
                .ageingRows(rows)
                .build();
    }
}
