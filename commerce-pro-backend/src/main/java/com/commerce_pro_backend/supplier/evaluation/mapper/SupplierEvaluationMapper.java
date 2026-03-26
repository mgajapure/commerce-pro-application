package com.commerce_pro_backend.supplier.evaluation.mapper;

import com.commerce_pro_backend.supplier.evaluation.dto.SupplierEvaluationDto;
import com.commerce_pro_backend.supplier.evaluation.entity.SupplierEvaluation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SupplierEvaluationMapper {

    public SupplierEvaluation toEntity(SupplierEvaluationDto.Request dto) {
        if (dto == null) {
            return null;
        }

        return SupplierEvaluation.builder()
                .supplierId(dto.getSupplierId())
                .supplierName(dto.getSupplierName())
                .evaluationDate(dto.getEvaluationDate())
                .evaluatedBy(dto.getEvaluatedBy())
                .overallScore(dto.getOverallScore())
                .qualityScore(dto.getQualityScore())
                .deliveryScore(dto.getDeliveryScore())
                .pricingScore(dto.getPricingScore())
                .communicationScore(dto.getCommunicationScore())
                .complianceScore(dto.getComplianceScore())
                .period(dto.getPeriod())
                .comments(dto.getComments())
                .recommendations(dto.getRecommendations())
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .build();
    }

    public SupplierEvaluationDto.Response toResponse(SupplierEvaluation entity) {
        if (entity == null) {
            return null;
        }

        return SupplierEvaluationDto.Response.builder()
                .id(entity.getId())
                .supplierId(entity.getSupplierId())
                .supplierName(entity.getSupplierName())
                .evaluationDate(entity.getEvaluationDate())
                .evaluatedBy(entity.getEvaluatedBy())
                .overallScore(entity.getOverallScore())
                .qualityScore(entity.getQualityScore())
                .deliveryScore(entity.getDeliveryScore())
                .pricingScore(entity.getPricingScore())
                .communicationScore(entity.getCommunicationScore())
                .complianceScore(entity.getComplianceScore())
                .period(entity.getPeriod())
                .comments(entity.getComments())
                .recommendations(entity.getRecommendations())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SupplierEvaluationDto.ListResponse toListResponse(SupplierEvaluation entity) {
        if (entity == null) {
            return null;
        }

        return SupplierEvaluationDto.ListResponse.builder()
                .id(entity.getId())
                .supplierName(entity.getSupplierName())
                .evaluationDate(entity.getEvaluationDate())
                .overallScore(entity.getOverallScore())
                .qualityScore(entity.getQualityScore())
                .deliveryScore(entity.getDeliveryScore())
                .pricingScore(entity.getPricingScore())
                .communicationScore(entity.getCommunicationScore())
                .complianceScore(entity.getComplianceScore())
                .period(entity.getPeriod())
                .status(entity.getStatus())
                .build();
    }

    public List<SupplierEvaluationDto.ListResponse> toListResponseList(List<SupplierEvaluation> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toListResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(SupplierEvaluationDto.Request dto, SupplierEvaluation entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getSupplierId() != null) {
            entity.setSupplierId(dto.getSupplierId());
        }
        if (dto.getSupplierName() != null) {
            entity.setSupplierName(dto.getSupplierName());
        }
        if (dto.getEvaluationDate() != null) {
            entity.setEvaluationDate(dto.getEvaluationDate());
        }
        if (dto.getEvaluatedBy() != null) {
            entity.setEvaluatedBy(dto.getEvaluatedBy());
        }
        if (dto.getOverallScore() != null) {
            entity.setOverallScore(dto.getOverallScore());
        }
        if (dto.getQualityScore() != null) {
            entity.setQualityScore(dto.getQualityScore());
        }
        if (dto.getDeliveryScore() != null) {
            entity.setDeliveryScore(dto.getDeliveryScore());
        }
        if (dto.getPricingScore() != null) {
            entity.setPricingScore(dto.getPricingScore());
        }
        if (dto.getCommunicationScore() != null) {
            entity.setCommunicationScore(dto.getCommunicationScore());
        }
        if (dto.getComplianceScore() != null) {
            entity.setComplianceScore(dto.getComplianceScore());
        }
        if (dto.getPeriod() != null) {
            entity.setPeriod(dto.getPeriod());
        }
        if (dto.getComments() != null) {
            entity.setComments(dto.getComments());
        }
        if (dto.getRecommendations() != null) {
            entity.setRecommendations(dto.getRecommendations());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }
}
