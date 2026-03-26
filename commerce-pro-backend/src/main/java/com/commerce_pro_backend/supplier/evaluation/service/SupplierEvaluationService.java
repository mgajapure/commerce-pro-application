package com.commerce_pro_backend.supplier.evaluation.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.evaluation.entity.SupplierEvaluation;
import com.commerce_pro_backend.supplier.evaluation.repository.SupplierEvaluationRepository;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupplierEvaluationService {

    private final SupplierEvaluationRepository supplierEvaluationRepository;
    private final CurrentUserService currentUserService;

    public Page<SupplierEvaluation> getAllEvaluations(Pageable pageable) {
        return supplierEvaluationRepository.findAll(pageable);
    }

    public Page<SupplierEvaluation> getBySupplier(String supplierId, Pageable pageable) {
        return supplierEvaluationRepository.findBySupplierIdOrderByEvaluationDateDesc(supplierId, pageable);
    }

    public SupplierEvaluation getEvaluation(String id) {
        return supplierEvaluationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierEvaluation", id));
    }

    @Transactional
    @CacheEvict(value = "supplier-evaluations", allEntries = true)
    public SupplierEvaluation createEvaluation(SupplierEvaluation request) {
        log.info("Creating supplier evaluation for supplier: {}", request.getSupplierId());

        request.setId(UUID.randomUUID().toString());
        request.setEvaluatedBy(currentUserService.getCurrentUserId());
        request.setEvaluationDate(Instant.now());

        if (request.getOverallScore() == null) {
            request.setOverallScore(calculateOverallScore(request));
        }

        SupplierEvaluation saved = supplierEvaluationRepository.save(request);
        log.info("Created supplier evaluation with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "supplier-evaluations", allEntries = true)
    public SupplierEvaluation updateEvaluation(String id, SupplierEvaluation request) {
        log.info("Updating supplier evaluation: {}", id);

        SupplierEvaluation existing = supplierEvaluationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierEvaluation", id));

        existing.setSupplierId(request.getSupplierId());
        existing.setSupplierName(request.getSupplierName());
        existing.setQualityScore(request.getQualityScore());
        existing.setDeliveryScore(request.getDeliveryScore());
        existing.setPricingScore(request.getPricingScore());
        existing.setCommunicationScore(request.getCommunicationScore());
        existing.setComplianceScore(request.getComplianceScore());
        existing.setPeriod(request.getPeriod());
        existing.setComments(request.getComments());
        existing.setRecommendations(request.getRecommendations());

        if (request.getOverallScore() != null) {
            existing.setOverallScore(request.getOverallScore());
        } else {
            existing.setOverallScore(calculateOverallScore(existing));
        }

        SupplierEvaluation updated = supplierEvaluationRepository.save(existing);
        log.info("Updated supplier evaluation with id: {}", updated.getId());
        return updated;
    }

    @Transactional
    @CacheEvict(value = "supplier-evaluations", allEntries = true)
    public void deleteEvaluation(String id) {
        log.info("Deleting supplier evaluation: {}", id);
        SupplierEvaluation evaluation = supplierEvaluationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierEvaluation", id));
        supplierEvaluationRepository.delete(evaluation);
        log.info("Deleted supplier evaluation with id: {}", id);
    }

    @Transactional
    @CacheEvict(value = "supplier-evaluations", allEntries = true)
    public SupplierEvaluation submitEvaluation(String id) {
        log.info("Submitting supplier evaluation: {}", id);

        SupplierEvaluation evaluation = supplierEvaluationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierEvaluation", id));

        evaluation.setStatus("SUBMITTED");

        SupplierEvaluation updated = supplierEvaluationRepository.save(evaluation);
        log.info("Submitted supplier evaluation with id: {}", id);
        return updated;
    }

    public Map<String, Object> getSupplierAverageScores(String supplierId) {
        List<SupplierEvaluation> evaluations = supplierEvaluationRepository
                .findBySupplierIdOrderByEvaluationDateDesc(supplierId);

        Map<String, Object> averages = new HashMap<>();

        if (evaluations.isEmpty()) {
            averages.put("overallScore", 0.0);
            averages.put("qualityScore", 0.0);
            averages.put("deliveryScore", 0.0);
            averages.put("pricingScore", 0.0);
            averages.put("communicationScore", 0.0);
            averages.put("complianceScore", 0.0);
            averages.put("evaluationCount", 0);
            return averages;
        }

        averages.put("overallScore", round(evaluations.stream()
                .mapToDouble(e -> e.getOverallScore() != null ? e.getOverallScore() : 0.0).average().orElse(0.0)));
        averages.put("qualityScore", round(evaluations.stream()
                .mapToDouble(e -> e.getQualityScore() != null ? e.getQualityScore() : 0.0).average().orElse(0.0)));
        averages.put("deliveryScore", round(evaluations.stream()
                .mapToDouble(e -> e.getDeliveryScore() != null ? e.getDeliveryScore() : 0.0).average().orElse(0.0)));
        averages.put("pricingScore", round(evaluations.stream()
                .mapToDouble(e -> e.getPricingScore() != null ? e.getPricingScore() : 0.0).average().orElse(0.0)));
        averages.put("communicationScore", round(evaluations.stream()
                .mapToDouble(e -> e.getCommunicationScore() != null ? e.getCommunicationScore() : 0.0).average().orElse(0.0)));
        averages.put("complianceScore", round(evaluations.stream()
                .mapToDouble(e -> e.getComplianceScore() != null ? e.getComplianceScore() : 0.0).average().orElse(0.0)));
        averages.put("evaluationCount", evaluations.size());

        return averages;
    }

    private Double calculateOverallScore(SupplierEvaluation evaluation) {
        double count = 0;
        double total = 0;

        if (evaluation.getQualityScore() != null) { total += evaluation.getQualityScore(); count++; }
        if (evaluation.getDeliveryScore() != null) { total += evaluation.getDeliveryScore(); count++; }
        if (evaluation.getPricingScore() != null) { total += evaluation.getPricingScore(); count++; }
        if (evaluation.getCommunicationScore() != null) { total += evaluation.getCommunicationScore(); count++; }
        if (evaluation.getComplianceScore() != null) { total += evaluation.getComplianceScore(); count++; }

        return count > 0 ? round(total / count) : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
