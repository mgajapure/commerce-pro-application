package com.commerce_pro_backend.inventory.repository;

import com.commerce_pro_backend.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * StockMovement Repository
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, String> {

    List<StockMovement> findByInventoryIdOrderByCreatedAtDesc(String inventoryId);

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(String productId);

    List<StockMovement> findByWarehouseIdOrderByCreatedAtDesc(String warehouseId);

    Page<StockMovement> findByProductId(String productId, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.createdAt BETWEEN :startDate AND :endDate ORDER BY sm.createdAt DESC")
    List<StockMovement> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.type = :type ORDER BY sm.createdAt DESC")
    List<StockMovement> findByType(@Param("type") StockMovement.MovementType type);

    @Query("SELECT sm.type, COUNT(sm), SUM(sm.quantity) FROM StockMovement sm WHERE sm.createdAt >= :since GROUP BY sm.type")
    List<Object[]> getMovementStatsSince(@Param("since") LocalDateTime since);


    // ── Analytics ────────────────────────────────────────────────────────────

    @Query("SELECT sm FROM StockMovement sm WHERE sm.createdAt BETWEEN :from AND :to ORDER BY sm.createdAt DESC")
    List<StockMovement> findByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.warehouse.id IN :warehouseIds AND sm.createdAt BETWEEN :from AND :to ORDER BY sm.createdAt DESC")
    List<StockMovement> findByWarehouseIdInAndCreatedAtBetween(
            @Param("warehouseIds") List<String> warehouseIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

}
