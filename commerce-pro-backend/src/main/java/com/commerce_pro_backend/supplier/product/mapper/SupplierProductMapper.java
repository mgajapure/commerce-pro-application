package com.commerce_pro_backend.supplier.product.mapper;

import com.commerce_pro_backend.supplier.product.dto.SupplierProductDto;
import com.commerce_pro_backend.supplier.product.entity.SupplierProduct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SupplierProductMapper {

    public SupplierProduct toEntity(SupplierProductDto.Request dto) {
        if (dto == null) {
            return null;
        }

        return SupplierProduct.builder()
                .supplierId(dto.getSupplierId())
                .supplierName(dto.getSupplierName())
                .productId(dto.getProductId())
                .productName(dto.getProductName())
                .sku(dto.getSku())
                .supplierSku(dto.getSupplierSku())
                .unitCost(dto.getUnitCost())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .minOrderQuantity(dto.getMinOrderQuantity() != null ? dto.getMinOrderQuantity() : 1)
                .leadTimeDays(dto.getLeadTimeDays())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false)
                .notes(dto.getNotes())
                .build();
    }

    public SupplierProductDto.Response toResponse(SupplierProduct entity) {
        if (entity == null) {
            return null;
        }

        return SupplierProductDto.Response.builder()
                .id(entity.getId())
                .supplierId(entity.getSupplierId())
                .supplierName(entity.getSupplierName())
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .sku(entity.getSku())
                .supplierSku(entity.getSupplierSku())
                .unitCost(entity.getUnitCost())
                .currency(entity.getCurrency())
                .minOrderQuantity(entity.getMinOrderQuantity())
                .leadTimeDays(entity.getLeadTimeDays())
                .isActive(entity.getIsActive())
                .isPrimary(entity.getIsPrimary())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SupplierProductDto.ListResponse toListResponse(SupplierProduct entity) {
        if (entity == null) {
            return null;
        }

        return SupplierProductDto.ListResponse.builder()
                .id(entity.getId())
                .supplierName(entity.getSupplierName())
                .productName(entity.getProductName())
                .sku(entity.getSku())
                .supplierSku(entity.getSupplierSku())
                .unitCost(entity.getUnitCost())
                .isActive(entity.getIsActive())
                .isPrimary(entity.getIsPrimary())
                .build();
    }

    public List<SupplierProductDto.ListResponse> toListResponseList(List<SupplierProduct> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toListResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(SupplierProductDto.Request dto, SupplierProduct entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getSupplierId() != null) {
            entity.setSupplierId(dto.getSupplierId());
        }
        if (dto.getSupplierName() != null) {
            entity.setSupplierName(dto.getSupplierName());
        }
        if (dto.getProductId() != null) {
            entity.setProductId(dto.getProductId());
        }
        if (dto.getProductName() != null) {
            entity.setProductName(dto.getProductName());
        }
        if (dto.getSku() != null) {
            entity.setSku(dto.getSku());
        }
        if (dto.getSupplierSku() != null) {
            entity.setSupplierSku(dto.getSupplierSku());
        }
        if (dto.getUnitCost() != null) {
            entity.setUnitCost(dto.getUnitCost());
        }
        if (dto.getCurrency() != null) {
            entity.setCurrency(dto.getCurrency());
        }
        if (dto.getMinOrderQuantity() != null) {
            entity.setMinOrderQuantity(dto.getMinOrderQuantity());
        }
        if (dto.getLeadTimeDays() != null) {
            entity.setLeadTimeDays(dto.getLeadTimeDays());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
        if (dto.getIsPrimary() != null) {
            entity.setIsPrimary(dto.getIsPrimary());
        }
        if (dto.getNotes() != null) {
            entity.setNotes(dto.getNotes());
        }
    }
}
