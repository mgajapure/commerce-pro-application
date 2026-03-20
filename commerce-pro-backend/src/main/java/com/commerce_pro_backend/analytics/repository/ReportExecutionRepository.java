package com.commerce_pro_backend.analytics.repository;

import com.commerce_pro_backend.analytics.entity.ReportExecution;
import com.commerce_pro_backend.analytics.enums.ReportStatus;
import com.commerce_pro_backend.analytics.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportExecutionRepository extends JpaRepository<ReportExecution, String> {

    Page<ReportExecution> findByRequestedBy(String userId, Pageable pageable);

    Page<ReportExecution> findByReportType(ReportType type, Pageable pageable);

    List<ReportExecution> findByScheduledReportId(String scheduledReportId);

    List<ReportExecution> findBySavedReportId(String savedReportId);

    @Query("SELECT e FROM ReportExecution e WHERE e.requestedBy = :userId ORDER BY e.createdAt DESC")
    Page<ReportExecution> findRecentByUser(@Param("userId") String userId, Pageable pageable);

    /** Clean up executions older than a given date to avoid unbounded storage growth. */
    @Modifying
    @Query("DELETE FROM ReportExecution e WHERE e.createdAt < :before AND e.status IN ('COMPLETED','FAILED','CANCELLED')")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    long countByStatus(ReportStatus status);
}
