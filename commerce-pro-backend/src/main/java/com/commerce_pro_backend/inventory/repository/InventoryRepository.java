package com.commerce_pro_backend.inventory.repository;

import com.commerce_pro_backend.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Inventory Repository
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId);

    List<Inventory> findByProductId(String productId);

    List<Inventory> findByWarehouseId(String warehouseId);

    List<Inventory> findByStatus(Inventory.StockStatus status);

    @Query("SELECT i FROM Inventory i WHERE i.status IN ('LOW_STOCK', 'OUT_OF_STOCK')")
    List<Inventory> findLowAndOutOfStock();

    @Query("SELECT i FROM Inventory i WHERE i.status = 'LOW_STOCK'")
    List<Inventory> findLowStock();

    @Query("SELECT i FROM Inventory i WHERE i.status = 'OUT_OF_STOCK'")
    List<Inventory> findOutOfStock();

    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalQuantityByProductId(@Param("productId") String productId);

    @Query("SELECT SUM(i.totalValue) FROM Inventory i")
    java.math.BigDecimal getTotalInventoryValue();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.status = :status")
    long countByStatus(@Param("status") Inventory.StockStatus status);

    @Query("SELECT i.warehouse.id, SUM(i.quantity), SUM(i.totalValue) FROM Inventory i GROUP BY i.warehouse.id")
    List<Object[]> getStatsByWarehouse();

    boolean existsByProductIdAndWarehouseId(String productId, String warehouseId);

    // ── Analytics ────────────────────────────────────────────────────────────

    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id IN :warehouseIds")
    List<Inventory> findByWarehouseIdIn(@Param("warehouseIds") List<String> warehouseIds);
}
