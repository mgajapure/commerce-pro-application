package com.commerce_pro_backend.ai.repository;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AiInsightRepository extends JpaRepository<AiInsight, String> {

    /** Load history for a specific entity to inject as context into future prompts */
    List<AiInsight> findByEntityIdAndFeatureTypeOrderByCreatedAtDesc(
            String entityId, AiFeatureType featureType, Pageable pageable);

    /** Dashboard: all insights of a given risk level created after a point in time */
    List<AiInsight> findByFeatureTypeAndRiskLevelAndCreatedAtAfter(
            AiFeatureType featureType, String riskLevel, LocalDateTime since);

    /** Admin insight list with optional filters */
    org.springframework.data.domain.Page<AiInsight> findByFeatureTypeAndRiskLevel(
            AiFeatureType featureType, String riskLevel,
            org.springframework.data.domain.Pageable pageable);

    /** Cost reporting */
    @Query("SELECT SUM(a.estimatedCostUsd) FROM AiInsight a WHERE a.createdAt BETWEEN :from AND :to")
    BigDecimal sumCostBetween(LocalDateTime from, LocalDateTime to);

    /** Cleanup job — delete insights whose TTL has elapsed */
    int deleteByExpiresAtBefore(LocalDateTime cutoff);

    /**
     * Admin insights browser — all optional filters, ordered by createdAt DESC.
     * Pass null for any parameter to skip that filter.
     */
    @Query("""
        SELECT a FROM AiInsight a
        WHERE (:feature    IS NULL OR a.featureType = :feature)
          AND (:riskLevel  IS NULL OR a.riskLevel   = :riskLevel)
          AND (:entityType IS NULL OR a.entityType  = :entityType)
          AND (:since      IS NULL OR a.createdAt  >= :since)
        ORDER BY a.createdAt DESC
        """)
    Page<AiInsight> findByFilters(
            @Param("feature")    AiFeatureType   feature,
            @Param("riskLevel")  String          riskLevel,
            @Param("entityType") String          entityType,
            @Param("since")      LocalDateTime   since,
            Pageable             pageable);
}
