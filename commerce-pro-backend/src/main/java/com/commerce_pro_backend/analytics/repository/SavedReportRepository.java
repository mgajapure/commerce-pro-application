package com.commerce_pro_backend.analytics.repository;

import com.commerce_pro_backend.analytics.entity.SavedReport;
import com.commerce_pro_backend.analytics.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedReportRepository extends JpaRepository<SavedReport, String> {

    Page<SavedReport> findByCreatedBy(String userId, Pageable pageable);

    Page<SavedReport> findByCreatedByOrIsPublicTrue(String userId, Pageable pageable);

    List<SavedReport> findByReportTypeAndCreatedBy(ReportType type, String userId);

    boolean existsByNameAndCreatedBy(String name, String userId);
}
