package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.TaxReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxReportRepository extends JpaRepository<TaxReport, String> {
    Optional<TaxReport> findByReportRef(String reportRef);
    Page<TaxReport> findByPeriodId(String periodId, Pageable pageable);
    List<TaxReport> findByPeriodIdAndReportType(String periodId, String reportType);
}
