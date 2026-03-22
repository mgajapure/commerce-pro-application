package com.commerce_pro_backend.catalog.seo.repository;

import com.commerce_pro_backend.catalog.seo.entity.SeoMetadata;
import com.commerce_pro_backend.catalog.seo.entity.SeoMetadata.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeoMetadataRepository extends JpaRepository<SeoMetadata, String>, JpaSpecificationExecutor<SeoMetadata> {

    Optional<SeoMetadata> findByEntityTypeAndEntityId(EntityType entityType, String entityId);

    void deleteByEntityTypeAndEntityId(EntityType entityType, String entityId);

    long countByEntityType(EntityType entityType);

    @Query("SELECT AVG(s.score) FROM SeoMetadata s")
    Double getAverageScore();

    @Query("SELECT COUNT(s) FROM SeoMetadata s WHERE s.score >= 80")
    long countWithScore80Plus();

    @Query("SELECT COUNT(s) FROM SeoMetadata s WHERE s.score < 50")
    long countWithScoreBelow50();

    @Query("SELECT s.entityType, COUNT(s) FROM SeoMetadata s GROUP BY s.entityType")
    List<Object[]> countByEntityTypeGrouped();
}
