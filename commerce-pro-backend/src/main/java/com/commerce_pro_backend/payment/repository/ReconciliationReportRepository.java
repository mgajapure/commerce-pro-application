package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.ReconciliationReport;
import com.commerce_pro_backend.payment.enums.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, String>, JpaSpecificationExecutor<ReconciliationReport> {

    Optional<ReconciliationReport> findByReportRef(String reportRef);

    Page<ReconciliationReport> findByStatus(ReconciliationStatus status, Pageable pageable);

    List<ReconciliationReport> findByPeriodStartBetween(LocalDateTime from, LocalDateTime to);
}
