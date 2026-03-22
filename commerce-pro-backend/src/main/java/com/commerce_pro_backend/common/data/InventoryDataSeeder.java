package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.catalog.product.dto.ProductSummaryDTO;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.inventory.dto.InventoryFilterDTO;
import com.commerce_pro_backend.inventory.dto.InventoryRequestDTO;
import com.commerce_pro_backend.inventory.dto.StockTransferRequestDTO;
import com.commerce_pro_backend.inventory.dto.StockUpdateRequestDTO;
import com.commerce_pro_backend.inventory.dto.WarehouseDTO;
import com.commerce_pro_backend.inventory.dto.WarehouseRequestDTO;
import com.commerce_pro_backend.inventory.forecast.dto.DemandForecastDto;
import com.commerce_pro_backend.inventory.forecast.service.DemandForecastService;
import com.commerce_pro_backend.inventory.service.InventoryService;
import com.commerce_pro_backend.inventory.valuation.dto.InventoryValuationDto;
import com.commerce_pro_backend.inventory.valuation.service.InventoryValuationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InventoryDataSeeder {

    private final SeedDataLoader seedDataLoader;

    @Bean
    @Profile("dev")
    @Order(2)
    CommandLineRunner seedInventory(ProductService productService,
                                    InventoryService inventoryService) {
        return args -> {
            log.info("━━━ [Seed] Inventory ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            List<ProductSummaryDTO> products = productService.getAllProducts();
            if (products.isEmpty()) {
                log.warn("[Seed] No products found — skipping inventory seed");
                return;
            }

            List<JsonNode> warehouseData = seedDataLoader.loadList(
                    "seed/warehouses.json", new TypeReference<>() {});

            WarehouseDTO mainWarehouse = createWarehouse(inventoryService, warehouseData.get(0));
            WarehouseDTO westWarehouse = createWarehouse(inventoryService, warehouseData.get(1));

            int count = 0;
            String[] zones = {"A", "B", "C", "D"};
            String[] aisles = {"01", "02", "03", "04", "05"};

            for (int i = 0; i < products.size(); i++) {
                ProductSummaryDTO product = products.get(i);
                int baseStock = product.getStock();
                BigDecimal unitCost = product.getPrice().multiply(new BigDecimal("0.60"));

                try {
                    int mainQty = proportional(baseStock, 0.70, 1.00);
                    int mainReserved = proportional(mainQty, 0.05, 0.15);
                    inventoryService.createInventory(buildInventoryRequest(
                            product.getId(), mainWarehouse.getId(),
                            mainQty, mainReserved, baseStock, unitCost,
                            zones[i % 4], aisles[i % 5], i));

                    if (i % 3 == 0) {
                        int westQty = proportional(baseStock, 0.20, 0.30);
                        inventoryService.createInventory(buildInventoryRequest(
                                product.getId(), westWarehouse.getId(),
                                westQty, 0, baseStock, unitCost,
                                zones[(i + 2) % 4], aisles[(i + 2) % 5], i));
                    }
                    count++;
                } catch (Exception e) {
                    log.error("[Seed] Inventory failed for '{}': {}", product.getName(), e.getMessage());
                }
            }

            try {
                var stats = inventoryService.getInventoryStats();
                log.info("[Seed] Inventory stats — items: {}, value: ${}, in-stock: {}, low: {}, out: {}",
                        stats.getTotalItems(), stats.getTotalInventoryValue(),
                        stats.getInStockCount(), stats.getLowStockCount(), stats.getOutOfStockCount());
            } catch (Exception e) {
                log.warn("[Seed] Could not retrieve inventory stats: {}", e.getMessage());
            }

            log.info("[Seed] Inventory complete — {} products stocked", count);
        };
    }

    @Bean
    @Profile("dev")
    @Order(7)
    CommandLineRunner seedInventoryExtended(
            InventoryService inventoryService,
            DemandForecastService forecastService,
            InventoryValuationService valuationService,
            ProductService productService,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Inventory Extended ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                seedDataLoader.authenticateAsSuperAdmin(userDetailsService);

                List<ProductSummaryDTO> products = productService.getAllProducts();
                if (products.isEmpty()) {
                    log.warn("[Seed] No products — skipping inventory extended seed");
                    return;
                }

                var inventoryPage = inventoryService.getInventory(new InventoryFilterDTO(),
                        org.springframework.data.domain.PageRequest.of(0, 100));
                var inventoryList = inventoryPage.getContent();

                if (inventoryList.isEmpty()) {
                    log.warn("[Seed] No inventory records — skipping extended seed");
                    return;
                }

                // Stock Adjustments
                List<JsonNode> adjustments = seedDataLoader.loadList(
                        "seed/stock-adjustments.json", new TypeReference<>() {});
                int adjustmentsCreated = 0;
                for (int i = 0; i < Math.min(adjustments.size(), inventoryList.size()); i++) {
                    try {
                        JsonNode adj = adjustments.get(i);
                        int qty = (i % 2 == 0) ? -(i + 1) : (i + 2);
                        inventoryService.adjustStock(inventoryList.get(i).getId(),
                                StockUpdateRequestDTO.builder()
                                        .quantity(Math.abs(qty))
                                        .adjust(qty > 0)
                                        .reason(adj.get("reason").asText())
                                        .notes(adj.get("notes").asText())
                                        .reference("ADJ-2024-" + String.format("%03d", i + 1))
                                        .referenceType(adj.get("referenceType").asText())
                                        .build());
                        adjustmentsCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] Adjustment skipped: {}", e.getMessage());
                    }
                }
                log.info("[Seed] Created {} stock adjustments", adjustmentsCreated);

                // Stock Transfers
                var warehouses = inventoryService.getAllWarehouses();
                if (warehouses.size() >= 2) {
                    String fromWarehouse = warehouses.get(0).getId();
                    String toWarehouse = warehouses.get(1).getId();

                    int transfersCreated = 0;
                    for (int i = 0; i < Math.min(3, products.size()); i++) {
                        try {
                            inventoryService.transferStock(StockTransferRequestDTO.builder()
                                    .fromWarehouseId(fromWarehouse)
                                    .toWarehouseId(toWarehouse)
                                    .productId(products.get(i).getId())
                                    .quantity(5 + i * 2)
                                    .notes("Rebalancing inventory between warehouses")
                                    .reference("TRF-2024-" + String.format("%03d", i + 1))
                                    .build());
                            transfersCreated++;
                        } catch (Exception e) {
                            log.warn("[Seed] Transfer skipped: {}", e.getMessage());
                        }
                    }
                    log.info("[Seed] Created {} stock transfers", transfersCreated);
                }

                // Demand Forecasts
                Instant now = Instant.now();
                String[] periods = {"monthly", "weekly", "quarterly"};
                String[] algorithms = {"MOVING_AVERAGE", "EXPONENTIAL_SMOOTHING", "LINEAR_REGRESSION"};

                int forecastsCreated = 0;
                for (int i = 0; i < Math.min(8, products.size()); i++) {
                    try {
                        ProductSummaryDTO p = products.get(i);
                        int baseDemand = 20 + (int) (Math.random() * 80);

                        forecastService.createForecast(DemandForecastDto.Request.builder()
                                .productId(p.getId())
                                .productName(p.getName())
                                .sku(p.getSku())
                                .period(periods[i % 3])
                                .algorithm(algorithms[i % 3])
                                .status("active")
                                .startDate(now.minus(30, ChronoUnit.DAYS))
                                .endDate(now.plus(60, ChronoUnit.DAYS))
                                .totalPredictedDemand(baseDemand * 30)
                                .averageDailyDemand((double) baseDemand)
                                .peakDemandQuantity((int) (baseDemand * 1.5))
                                .safetyStockRecommendation((int) (baseDemand * 0.3))
                                .reorderPointRecommendation((int) (baseDemand * 0.5))
                                .generatedBy("system")
                                .build());
                        forecastsCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] Forecast skipped for {}: {}", products.get(i).getName(), e.getMessage());
                    }
                }
                log.info("[Seed] Created {} demand forecasts", forecastsCreated);

                // Inventory Valuations
                String[] valuationMethods = {"fifo", "lifo", "weighted_average", "fifo"};
                String warehouseId = warehouses.isEmpty() ? null : warehouses.get(0).getId();
                String warehouseName = warehouses.isEmpty() ? "Main Warehouse" : warehouses.get(0).getName();

                int valuationsCreated = 0;
                for (int i = 0; i < Math.min(8, products.size()); i++) {
                    try {
                        ProductSummaryDTO p = products.get(i);
                        BigDecimal cost = p.getPrice().multiply(new BigDecimal("0.60"));
                        int qty = 50 + (int) (Math.random() * 150);

                        List<InventoryValuationDto.CostLayerRequest> layers = List.of(
                                InventoryValuationDto.CostLayerRequest.builder()
                                        .receiptDate(now.minus(60, ChronoUnit.DAYS))
                                        .unitCost(cost.multiply(new BigDecimal("0.95")))
                                        .originalQuantity((int) (qty * 0.6))
                                        .reference("PO-2024-" + String.format("%03d", i * 2 + 1))
                                        .build(),
                                InventoryValuationDto.CostLayerRequest.builder()
                                        .receiptDate(now.minus(15, ChronoUnit.DAYS))
                                        .unitCost(cost)
                                        .originalQuantity((int) (qty * 0.4))
                                        .reference("PO-2024-" + String.format("%03d", i * 2 + 2))
                                        .build()
                        );

                        valuationService.createValuation(InventoryValuationDto.Request.builder()
                                .productId(p.getId())
                                .productName(p.getName())
                                .sku(p.getSku())
                                .warehouseId(warehouseId)
                                .warehouseName(warehouseName)
                                .valuationMethod(valuationMethods[i % 4])
                                .status("active")
                                .totalQuantity(qty)
                                .averageUnitCost(cost)
                                .totalValue(cost.multiply(BigDecimal.valueOf(qty)))
                                .calculatedBy("system")
                                .costLayers(layers)
                                .build());
                        valuationsCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] Valuation skipped for {}: {}", products.get(i).getName(), e.getMessage());
                    }
                }
                log.info("[Seed] Created {} inventory valuations", valuationsCreated);

                log.info("[Seed] Inventory extended seed complete");

            } catch (Exception e) {
                log.error("[Seed] Inventory extended seed failed: {}", e.getMessage(), e);
            } finally {
                seedDataLoader.clearSecurityContext();
            }
        };
    }

    private WarehouseDTO createWarehouse(InventoryService inventoryService, JsonNode node) {
        return inventoryService.createWarehouse(WarehouseRequestDTO.builder()
                .name(node.get("name").asText())
                .code(node.get("code").asText())
                .address(node.get("address").asText())
                .city(node.get("city").asText())
                .state(node.get("state").asText())
                .country(node.get("country").asText())
                .postalCode(node.get("postalCode").asText())
                .managerName(node.get("managerName").asText())
                .managerEmail(node.get("managerEmail").asText())
                .managerPhone(node.get("managerPhone").asText())
                .isActive(node.get("isActive").asBoolean())
                .isDefault(node.get("isDefault").asBoolean())
                .build());
    }

    private InventoryRequestDTO buildInventoryRequest(
            String productId, String warehouseId,
            int quantity, int reserved,
            int baseStock, BigDecimal unitCost,
            String zone, String aisle, int index) {
        return InventoryRequestDTO.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(quantity)
                .reserved(reserved)
                .lowStockThreshold(Math.max(5, (int) (baseStock * 0.15)))
                .reorderPoint(Math.max(10, (int) (baseStock * 0.25)))
                .reorderQuantity((int) (baseStock * 0.50))
                .unitCost(unitCost)
                .binLocation(zone + "-" + aisle + "-" + String.format("%03d", index + 1))
                .zone(zone)
                .aisle(aisle)
                .trackInventory(true)
                .build();
    }

    private int proportional(int base, double minFraction, double maxFraction) {
        double fraction = minFraction + Math.random() * (maxFraction - minFraction);
        return Math.max(1, (int) (base * fraction));
    }
}
