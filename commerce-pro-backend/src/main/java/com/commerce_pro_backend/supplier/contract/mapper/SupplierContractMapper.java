package com.commerce_pro_backend.supplier.contract.mapper;

import com.commerce_pro_backend.supplier.contract.dto.SupplierContractDto;
import com.commerce_pro_backend.supplier.contract.entity.SupplierContract;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SupplierContractMapper {

    public SupplierContract toEntity(SupplierContractDto.Request dto) {
        if (dto == null) {
            return null;
        }

        return SupplierContract.builder()
                .supplierId(dto.getSupplierId())
                .supplierName(dto.getSupplierName())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .contractType(dto.getContractType())
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .totalValue(dto.getTotalValue())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .paymentTerms(dto.getPaymentTerms())
                .autoRenew(dto.getAutoRenew() != null ? dto.getAutoRenew() : false)
                .renewalNoticeDays(dto.getRenewalNoticeDays())
                .documentUrl(dto.getDocumentUrl())
                .terms(dto.getTerms())
                .build();
    }

    public SupplierContractDto.Response toResponse(SupplierContract entity) {
        if (entity == null) {
            return null;
        }

        return SupplierContractDto.Response.builder()
                .id(entity.getId())
                .contractNumber(entity.getContractNumber())
                .supplierId(entity.getSupplierId())
                .supplierName(entity.getSupplierName())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .contractType(entity.getContractType())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalValue(entity.getTotalValue())
                .currency(entity.getCurrency())
                .paymentTerms(entity.getPaymentTerms())
                .autoRenew(entity.getAutoRenew())
                .renewalNoticeDays(entity.getRenewalNoticeDays())
                .documentUrl(entity.getDocumentUrl())
                .terms(entity.getTerms())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SupplierContractDto.ListResponse toListResponse(SupplierContract entity) {
        if (entity == null) {
            return null;
        }

        return SupplierContractDto.ListResponse.builder()
                .id(entity.getId())
                .contractNumber(entity.getContractNumber())
                .supplierName(entity.getSupplierName())
                .title(entity.getTitle())
                .contractType(entity.getContractType())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalValue(entity.getTotalValue())
                .build();
    }

    public List<SupplierContractDto.ListResponse> toListResponseList(List<SupplierContract> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toListResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(SupplierContractDto.Request dto, SupplierContract entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getSupplierId() != null) {
            entity.setSupplierId(dto.getSupplierId());
        }
        if (dto.getSupplierName() != null) {
            entity.setSupplierName(dto.getSupplierName());
        }
        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getContractType() != null) {
            entity.setContractType(dto.getContractType());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getStartDate() != null) {
            entity.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            entity.setEndDate(dto.getEndDate());
        }
        if (dto.getTotalValue() != null) {
            entity.setTotalValue(dto.getTotalValue());
        }
        if (dto.getCurrency() != null) {
            entity.setCurrency(dto.getCurrency());
        }
        if (dto.getPaymentTerms() != null) {
            entity.setPaymentTerms(dto.getPaymentTerms());
        }
        if (dto.getAutoRenew() != null) {
            entity.setAutoRenew(dto.getAutoRenew());
        }
        if (dto.getRenewalNoticeDays() != null) {
            entity.setRenewalNoticeDays(dto.getRenewalNoticeDays());
        }
        if (dto.getDocumentUrl() != null) {
            entity.setDocumentUrl(dto.getDocumentUrl());
        }
        if (dto.getTerms() != null) {
            entity.setTerms(dto.getTerms());
        }
    }
}
