package com.commerce_pro_backend.supplier.procurement.repository;

import com.commerce_pro_backend.supplier.procurement.entity.ProcurementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementRequestRepository extends JpaRepository<ProcurementRequest, String>, JpaSpecificationExecutor<ProcurementRequest> {

    Optional<ProcurementRequest> findByRequestNumber(String requestNumber);

    boolean existsByRequestNumber(String requestNumber);

    List<ProcurementRequest> findByStatusOrderByCreatedAtDesc(String status);

    Page<ProcurementRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<ProcurementRequest> findByRequestedByOrderByCreatedAtDesc(String requestedBy);

    long countByStatus(String status);
}
