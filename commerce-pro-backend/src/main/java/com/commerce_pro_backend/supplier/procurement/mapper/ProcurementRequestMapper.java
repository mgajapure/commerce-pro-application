package com.commerce_pro_backend.supplier.procurement.mapper;

import com.commerce_pro_backend.supplier.procurement.dto.ProcurementRequestDto;
import com.commerce_pro_backend.supplier.procurement.entity.ProcurementRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ProcurementRequestMapper {

    public ProcurementRequest toEntity(ProcurementRequestDto.Request dto) {
        if (dto == null) {
            return null;
        }

        return ProcurementRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .priority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM")
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .requestedBy(dto.getRequestedBy())
                .supplierId(dto.getSupplierId())
                .supplierName(dto.getSupplierName())
                .estimatedCost(dto.getEstimatedCost())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .requiredByDate(dto.getRequiredByDate())
                .notes(dto.getNotes())
                .build();
    }

    public ProcurementRequestDto.Response toResponse(ProcurementRequest entity) {
        if (entity == null) {
            return null;
        }

        return ProcurementRequestDto.Response.builder()
                .id(entity.getId())
                .requestNumber(entity.getRequestNumber())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .requestedBy(entity.getRequestedBy())
                .supplierId(entity.getSupplierId())
                .supplierName(entity.getSupplierName())
                .estimatedCost(entity.getEstimatedCost())
                .actualCost(entity.getActualCost())
                .currency(entity.getCurrency())
                .requiredByDate(entity.getRequiredByDate())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ProcurementRequestDto.ListResponse toListResponse(ProcurementRequest entity) {
        if (entity == null) {
            return null;
        }

        return ProcurementRequestDto.ListResponse.builder()
                .id(entity.getId())
                .requestNumber(entity.getRequestNumber())
                .title(entity.getTitle())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .requestedBy(entity.getRequestedBy())
                .estimatedCost(entity.getEstimatedCost())
                .actualCost(entity.getActualCost())
                .requiredByDate(entity.getRequiredByDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<ProcurementRequestDto.ListResponse> toListResponseList(List<ProcurementRequest> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toListResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(ProcurementRequestDto.Request dto, ProcurementRequest entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getPriority() != null) {
            entity.setPriority(dto.getPriority());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getRequestedBy() != null) {
            entity.setRequestedBy(dto.getRequestedBy());
        }
        if (dto.getSupplierId() != null) {
            entity.setSupplierId(dto.getSupplierId());
        }
        if (dto.getSupplierName() != null) {
            entity.setSupplierName(dto.getSupplierName());
        }
        if (dto.getEstimatedCost() != null) {
            entity.setEstimatedCost(dto.getEstimatedCost());
        }
        if (dto.getCurrency() != null) {
            entity.setCurrency(dto.getCurrency());
        }
        if (dto.getRequiredByDate() != null) {
            entity.setRequiredByDate(dto.getRequiredByDate());
        }
        if (dto.getNotes() != null) {
            entity.setNotes(dto.getNotes());
        }
    }
}
