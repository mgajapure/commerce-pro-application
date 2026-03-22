package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.ChargebackDispute;
import com.commerce_pro_backend.payment.enums.ChargebackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChargebackDisputeRepository extends JpaRepository<ChargebackDispute, String>, JpaSpecificationExecutor<ChargebackDispute> {

    Optional<ChargebackDispute> findByDisputeRef(String disputeRef);

    List<ChargebackDispute> findByOrderId(String orderId);

    Page<ChargebackDispute> findByStatus(ChargebackStatus status, Pageable pageable);

    long countByStatus(ChargebackStatus status);

    @Query("SELECT c FROM ChargebackDispute c WHERE c.status = 'EVIDENCE_REQUIRED' AND c.responseDeadline BETWEEN :now AND :cutoff")
    List<ChargebackDispute> findUpcomingDeadlines(@Param("now") LocalDateTime now, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COALESCE(SUM(c.disputedAmount), 0) FROM ChargebackDispute c WHERE c.status NOT IN ('WON','WITHDRAWN')")
    BigDecimal sumActiveDisputedAmount();
}
