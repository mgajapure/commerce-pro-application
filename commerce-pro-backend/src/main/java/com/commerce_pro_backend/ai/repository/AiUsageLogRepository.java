package com.commerce_pro_backend.ai.repository;

import com.commerce_pro_backend.ai.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, String> {

    /**
     * Cost + token summary grouped by feature — used by the admin cost dashboard.
     * Returns: [featureType, totalCostUsd, totalTokensInput, totalTokensOutput, callCount, avgLatencyMs]
     */
    @Query("""
        SELECT a.featureType, SUM(a.totalCostUsd), SUM(a.tokensInput), SUM(a.tokensOutput),
               COUNT(a), AVG(a.latencyMs)
        FROM AiUsageLog a
        WHERE a.calledAt BETWEEN :from AND :to
        GROUP BY a.featureType
        ORDER BY SUM(a.totalCostUsd) DESC
        """)
    List<Object[]> costSummaryByFeature(LocalDateTime from, LocalDateTime to);

    /** SLA monitoring — find slow calls exceeding a latency threshold */
    List<AiUsageLog> findByLatencyMsGreaterThanAndCalledAtAfter(Long latencyMs, LocalDateTime since);

    /** Error rate monitoring */
    long countBySuccessAndCalledAtAfter(Boolean success, LocalDateTime since);
}
