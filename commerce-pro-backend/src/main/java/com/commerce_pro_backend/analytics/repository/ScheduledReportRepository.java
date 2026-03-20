package com.commerce_pro_backend.analytics.repository;

import com.commerce_pro_backend.analytics.entity.ScheduledReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, String> {

    Page<ScheduledReport> findByCreatedBy(String userId, Pageable pageable);

    List<ScheduledReport> findByIsActiveTrueAndNextRunAtBefore(LocalDateTime cutoff);

    @Query("SELECT s FROM ScheduledReport s WHERE s.savedReportId = :savedReportId")
    List<ScheduledReport> findBySavedReportId(@Param("savedReportId") String savedReportId);

    long countByIsActiveTrue();
}
