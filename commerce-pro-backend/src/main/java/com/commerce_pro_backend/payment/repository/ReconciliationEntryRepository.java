package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.ReconciliationEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationEntryRepository extends JpaRepository<ReconciliationEntry, String> {

    Page<ReconciliationEntry> findByReportId(String reportId, Pageable pageable);

    List<ReconciliationEntry> findByReportIdAndMatchStatus(String reportId, String matchStatus);

    long countByReportIdAndMatchStatus(String reportId, String matchStatus);
}
