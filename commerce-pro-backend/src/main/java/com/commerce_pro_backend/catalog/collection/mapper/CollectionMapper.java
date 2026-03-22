package com.commerce_pro_backend.catalog.collection.mapper;

import com.commerce_pro_backend.catalog.collection.dto.CollectionDto;
import com.commerce_pro_backend.catalog.collection.entity.Collection;
import com.commerce_pro_backend.catalog.collection.entity.Collection.CollectionStatus;
import com.commerce_pro_backend.catalog.collection.entity.Collection.CollectionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class CollectionMapper {

    public Collection toEntity(CollectionDto.Request dto) {
        if (dto == null) return null;

        return Collection.builder()
                .name(dto.getName())
                .slug(dto.getSlug())
                .description(dto.getDescription())
                .type(dto.getType() != null ? CollectionType.valueOf(dto.getType()) : CollectionType.MANUAL)
                .status(dto.getStatus() != null ? CollectionStatus.valueOf(dto.getStatus()) : CollectionStatus.DRAFT)
                .imageUrl(dto.getImageUrl())
                .conditions(dto.getConditions())
                .productIds(dto.getProductIds() != null ? new ArrayList<>(dto.getProductIds()) : new ArrayList<>())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isFeatured(dto.getIsFeatured() != null ? dto.getIsFeatured() : false)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
    }

    public CollectionDto.Response toResponse(Collection entity) {
        if (entity == null) return null;

        return CollectionDto.Response.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .type(entity.getType().name())
                .status(entity.getStatus().name())
                .imageUrl(entity.getImageUrl())
                .conditions(entity.getConditions())
                .productIds(entity.getProductIds())
                .sortOrder(entity.getSortOrder())
                .isFeatured(entity.getIsFeatured())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CollectionDto.ListResponse toListResponse(Collection entity) {
        if (entity == null) return null;

        return CollectionDto.ListResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .type(entity.getType().name())
                .status(entity.getStatus().name())
                .imageUrl(entity.getImageUrl())
                .isFeatured(entity.getIsFeatured())
                .sortOrder(entity.getSortOrder())
                .productCount(entity.getProductIds() != null ? entity.getProductIds().size() : 0)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public void updateEntityFromDto(CollectionDto.Request dto, Collection entity) {
        if (dto == null || entity == null) return;

        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getSlug() != null) entity.setSlug(dto.getSlug());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getType() != null) entity.setType(CollectionType.valueOf(dto.getType()));
        if (dto.getStatus() != null) entity.setStatus(CollectionStatus.valueOf(dto.getStatus()));
        if (dto.getImageUrl() != null) entity.setImageUrl(dto.getImageUrl());
        if (dto.getConditions() != null) entity.setConditions(dto.getConditions());
        if (dto.getProductIds() != null) entity.setProductIds(new ArrayList<>(dto.getProductIds()));
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
        if (dto.getIsFeatured() != null) entity.setIsFeatured(dto.getIsFeatured());
        if (dto.getStartDate() != null) entity.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) entity.setEndDate(dto.getEndDate());
    }
}
