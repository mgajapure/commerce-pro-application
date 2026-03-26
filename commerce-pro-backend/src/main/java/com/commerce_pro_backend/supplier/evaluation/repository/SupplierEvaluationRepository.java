package com.commerce_pro_backend.supplier.evaluation.repository;

import com.commerce_pro_backend.supplier.evaluation.entity.SupplierEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierEvaluationRepository extends JpaRepository<SupplierEvaluation, String>, JpaSpecificationExecutor<SupplierEvaluation> {

    List<SupplierEvaluation> findBySupplierIdOrderByEvaluationDateDesc(String supplierId);

    Page<SupplierEvaluation> findBySupplierIdOrderByEvaluationDateDesc(String supplierId, Pageable pageable);

    List<SupplierEvaluation> findByStatus(String status);

    @Query("SELECT COALESCE(AVG(e.overallScore), 0) FROM SupplierEvaluation e WHERE e.supplierId = :supplierId")
    Double averageOverallScoreBySupplierId(@Param("supplierId") String supplierId);
}
