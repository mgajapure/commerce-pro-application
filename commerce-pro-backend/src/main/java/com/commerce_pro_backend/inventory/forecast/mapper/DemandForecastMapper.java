package com.commerce_pro_backend.inventory.forecast.mapper;

import com.commerce_pro_backend.inventory.forecast.dto.DemandForecastDto;
import com.commerce_pro_backend.inventory.forecast.entity.DemandForecast;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for DemandForecast entity and its DTOs.
 */
@Mapper(componentModel = "spring")
public interface DemandForecastMapper {

    /**
     * Maps a manual Request DTO to a DemandForecast entity.
     * Generated-at / last-updated-at fields are managed by @PrePersist / @PreUpdate.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    DemandForecast toEntity(DemandForecastDto.Request request);

    /**
     * Maps a DemandForecast entity to a full Response DTO.
     */
    DemandForecastDto.Response toResponse(DemandForecast entity);

    /**
     * Alias kept for callers that prefer the generic "toDto" name.
     */
    default DemandForecastDto.Response toDto(DemandForecast entity) {
        return toResponse(entity);
    }

    /**
     * Updates an existing entity in-place from a Request DTO.
     * Identity and audit fields are left unchanged.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    void updateEntityFromRequest(DemandForecastDto.Request request, @MappingTarget DemandForecast entity);
}
