package com.commerce_pro_backend.catalog.attribute.mapper;

import com.commerce_pro_backend.catalog.attribute.dto.AttributeDto;
import com.commerce_pro_backend.catalog.attribute.entity.Attribute;
import com.commerce_pro_backend.catalog.attribute.entity.Attribute.AttributeType;
import com.commerce_pro_backend.catalog.attribute.entity.AttributeOption;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AttributeMapper {

    public Attribute toEntity(AttributeDto.Request dto) {
        if (dto == null) return null;

        Attribute attribute = Attribute.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .type(AttributeType.valueOf(dto.getType()))
                .description(dto.getDescription())
                .isRequired(dto.getIsRequired() != null ? dto.getIsRequired() : false)
                .isFilterable(dto.getIsFilterable() != null ? dto.getIsFilterable() : false)
                .isSearchable(dto.getIsSearchable() != null ? dto.getIsSearchable() : false)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .options(new ArrayList<>())
                .build();

        if (dto.getOptions() != null) {
            dto.getOptions().forEach(optDto -> {
                AttributeOption option = toOptionEntity(optDto);
                attribute.addOption(option);
            });
        }

        return attribute;
    }

    public AttributeOption toOptionEntity(AttributeDto.OptionRequest dto) {
        if (dto == null) return null;

        return AttributeOption.builder()
                .value(dto.getValue())
                .label(dto.getLabel())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .colorHex(dto.getColorHex())
                .build();
    }

    public AttributeDto.Response toResponse(Attribute entity) {
        if (entity == null) return null;

        return AttributeDto.Response.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .type(entity.getType().name())
                .description(entity.getDescription())
                .isRequired(entity.getIsRequired())
                .isFilterable(entity.getIsFilterable())
                .isSearchable(entity.getIsSearchable())
                .isActive(entity.getIsActive())
                .sortOrder(entity.getSortOrder())
                .options(entity.getOptions() != null
                        ? entity.getOptions().stream().map(this::toOptionResponse).collect(Collectors.toList())
                        : List.of())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AttributeDto.OptionResponse toOptionResponse(AttributeOption entity) {
        if (entity == null) return null;

        return AttributeDto.OptionResponse.builder()
                .id(entity.getId())
                .value(entity.getValue())
                .label(entity.getLabel())
                .sortOrder(entity.getSortOrder())
                .colorHex(entity.getColorHex())
                .build();
    }

    public AttributeDto.ListResponse toListResponse(Attribute entity) {
        if (entity == null) return null;

        return AttributeDto.ListResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .type(entity.getType().name())
                .isRequired(entity.getIsRequired())
                .isFilterable(entity.getIsFilterable())
                .isSearchable(entity.getIsSearchable())
                .isActive(entity.getIsActive())
                .sortOrder(entity.getSortOrder())
                .optionCount(entity.getOptions() != null ? entity.getOptions().size() : 0)
                .build();
    }

    public void updateEntityFromDto(AttributeDto.Request dto, Attribute entity) {
        if (dto == null || entity == null) return;

        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getCode() != null) entity.setCode(dto.getCode());
        if (dto.getType() != null) entity.setType(AttributeType.valueOf(dto.getType()));
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getIsRequired() != null) entity.setIsRequired(dto.getIsRequired());
        if (dto.getIsFilterable() != null) entity.setIsFilterable(dto.getIsFilterable());
        if (dto.getIsSearchable() != null) entity.setIsSearchable(dto.getIsSearchable());
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());

        if (dto.getOptions() != null) {
            entity.clearOptions();
            dto.getOptions().forEach(optDto -> {
                AttributeOption option = toOptionEntity(optDto);
                entity.addOption(option);
            });
        }
    }
}
