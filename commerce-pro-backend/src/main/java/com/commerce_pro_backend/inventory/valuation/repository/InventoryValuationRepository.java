package com.commerce_pro_backend.inventory.valuation.repository;

import com.commerce_pro_backend.inventory.valuation.entity.InventoryValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for InventoryValuation entities.
 */
@Repository
public interface InventoryValuationRepository
        extends JpaRepository<InventoryValuation, String>, JpaSpecificationExecutor<InventoryValuation> {

    // ==================== BASIC FINDERS ====================

    List<InventoryValuation> findByProductId(String productId);

    List<InventoryValuation> findByWarehouseId(String warehouseId);

    List<InventoryValuation> findByValuationMethod(String valuationMethod);

    List<InventoryValuation> findByStatus(String status);

    // ==================== COMBINED FINDERS ====================

    Optional<InventoryValuation> findByProductIdAndWarehouseId(String productId, String warehouseId);

    List<InventoryValuation> findByProductIdAndStatus(String productId, String status);

    List<InventoryValuation> findByWarehouseIdAndStatus(String warehouseId, String status);

    List<InventoryValuation> findByValuationMethodAndStatus(String valuationMethod, String status);

    boolean existsByProductIdAndWarehouseId(String productId, String warehouseId);

    // ==================== AGGREGATE QUERIES ====================

    @Query("SELECT SUM(v.totalValue) FROM InventoryValuation v WHERE v.status = 'active'")
    BigDecimal getTotalActiveInventoryValue();

    @Query("SELECT SUM(v.totalValue) FROM InventoryValuation v WHERE v.warehouseId = :warehouseId AND v.status = 'active'")
    BigDecimal getTotalValueByWarehouse(@Param("warehouseId") String warehouseId);

    @Query("SELECT SUM(v.totalValue) FROM InventoryValuation v WHERE v.productId = :productId AND v.status = 'active'")
    BigDecimal getTotalValueByProduct(@Param("productId") String productId);

    @Query("SELECT COUNT(DISTINCT v.productId) FROM InventoryValuation v WHERE v.status = 'active'")
    long countDistinctActiveProducts();

    @Query("SELECT COUNT(DISTINCT v.warehouseId) FROM InventoryValuation v WHERE v.status = 'active'")
    long countDistinctActiveWarehouses();

    @Query("SELECT v.valuationMethod, COUNT(v), SUM(v.totalValue) FROM InventoryValuation v " +
           "WHERE v.status = 'active' GROUP BY v.valuationMethod")
    List<Object[]> getBreakdownByMethod();

    @Query("SELECT v.warehouseId, v.warehouseName, COUNT(v), SUM(v.totalValue) FROM InventoryValuation v " +
           "WHERE v.status = 'active' GROUP BY v.warehouseId, v.warehouseName")
    List<Object[]> getBreakdownByWarehouse();
}
