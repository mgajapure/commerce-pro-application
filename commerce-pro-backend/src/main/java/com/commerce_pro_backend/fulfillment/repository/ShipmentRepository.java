package com.commerce_pro_backend.fulfillment.repository;

import com.commerce_pro_backend.fulfillment.entity.Shipment;
import com.commerce_pro_backend.fulfillment.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByShipmentNumber(String shipmentNumber);

    List<Shipment> findByOrderId(String orderId);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    List<Shipment> findByTrackingNumber(String trackingNumber);

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status = :status")
    long countByStatus(@Param("status") ShipmentStatus status);

    @Query("SELECT s.status, COUNT(s) FROM Shipment s GROUP BY s.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status = :status AND s.estimatedDeliveryDate < CURRENT_DATE")
    long countOverdue(@Param("status") ShipmentStatus status);
    

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Query("SELECT s FROM Shipment s WHERE s.createdAt BETWEEN :from AND :to")
    List<Shipment> findByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT s FROM Shipment s WHERE s.carrierId IN :carrierIds AND s.createdAt BETWEEN :from AND :to")
    List<Shipment> findByCarrierIdInAndCreatedAtBetween(
            @Param("carrierIds") List<String> carrierIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT s.carrierId, s.carrierName, COUNT(s), SUM(s.shippingCost) FROM Shipment s WHERE s.createdAt BETWEEN :from AND :to GROUP BY s.carrierId, s.carrierName")
    List<Object[]> aggregateByCarrier(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
