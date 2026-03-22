package com.commerce_pro_backend.inventory.valuation.repository;

import com.commerce_pro_backend.inventory.valuation.entity.CostLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CostLayer entities.
 */
@Repository
public interface CostLayerRepository extends JpaRepository<CostLayer, String> {

    // ==================== FINDERS BY VALUATION ====================

    /** All layers for a valuation ordered oldest-first (used for FIFO consumption). */
    List<CostLayer> findByValuationIdOrderByReceiptDateAsc(String valuationId);

    /** All layers for a valuation ordered newest-first (used for LIFO consumption). */
    List<CostLayer> findByValuationIdOrderByReceiptDateDesc(String valuationId);

    /** Active (remaining > 0) layers for FIFO. */
    @Query("SELECT c FROM CostLayer c WHERE c.valuation.id = :valuationId " +
           "AND c.remainingQuantity > 0 ORDER BY c.receiptDate ASC")
    List<CostLayer> findActiveFifoLayers(@Param("valuationId") String valuationId);

    /** Active (remaining > 0) layers for LIFO. */
    @Query("SELECT c FROM CostLayer c WHERE c.valuation.id = :valuationId " +
           "AND c.remainingQuantity > 0 ORDER BY c.receiptDate DESC")
    List<CostLayer> findActiveLifoLayers(@Param("valuationId") String valuationId);

    // ==================== DELETE HELPERS ====================

    @Modifying
    @Query("DELETE FROM CostLayer c WHERE c.valuation.id = :valuationId")
    void deleteByValuationId(@Param("valuationId") String valuationId);
}
