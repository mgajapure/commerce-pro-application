package com.commerce_pro_backend.catalog.seo.mapper;

import com.commerce_pro_backend.catalog.seo.dto.SeoDto;
import com.commerce_pro_backend.catalog.seo.entity.SeoMetadata;
import com.commerce_pro_backend.catalog.seo.entity.SeoMetadata.EntityType;
import org.springframework.stereotype.Component;

@Component
public class SeoMapper {

    public SeoMetadata toEntity(SeoDto.Request dto, EntityType entityType, String entityId) {
        if (dto == null) return null;

        return SeoMetadata.builder()
                .entityType(entityType)
                .entityId(entityId)
                .metaTitle(dto.getMetaTitle())
                .metaDescription(dto.getMetaDescription())
                .metaKeywords(dto.getMetaKeywords())
                .canonicalUrl(dto.getCanonicalUrl())
                .ogTitle(dto.getOgTitle())
                .ogDescription(dto.getOgDescription())
                .ogImage(dto.getOgImage())
                .robotsDirective(dto.getRobotsDirective())
                .slug(dto.getSlug())
                .customHeadHtml(dto.getCustomHeadHtml())
                .score(0)
                .build();
    }

    public SeoDto.Response toResponse(SeoMetadata entity) {
        if (entity == null) return null;

        return SeoDto.Response.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType().name())
                .entityId(entity.getEntityId())
                .metaTitle(entity.getMetaTitle())
                .metaDescription(entity.getMetaDescription())
                .metaKeywords(entity.getMetaKeywords())
                .canonicalUrl(entity.getCanonicalUrl())
                .ogTitle(entity.getOgTitle())
                .ogDescription(entity.getOgDescription())
                .ogImage(entity.getOgImage())
                .robotsDirective(entity.getRobotsDirective())
                .slug(entity.getSlug())
                .customHeadHtml(entity.getCustomHeadHtml())
                .score(entity.getScore())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SeoDto.ListResponse toListResponse(SeoMetadata entity) {
        if (entity == null) return null;

        return SeoDto.ListResponse.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType().name())
                .entityId(entity.getEntityId())
                .metaTitle(entity.getMetaTitle())
                .metaDescription(entity.getMetaDescription())
                .score(entity.getScore())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDto(SeoDto.Request dto, SeoMetadata entity) {
        if (dto == null || entity == null) return;

        if (dto.getMetaTitle() != null) entity.setMetaTitle(dto.getMetaTitle());
        if (dto.getMetaDescription() != null) entity.setMetaDescription(dto.getMetaDescription());
        if (dto.getMetaKeywords() != null) entity.setMetaKeywords(dto.getMetaKeywords());
        if (dto.getCanonicalUrl() != null) entity.setCanonicalUrl(dto.getCanonicalUrl());
        if (dto.getOgTitle() != null) entity.setOgTitle(dto.getOgTitle());
        if (dto.getOgDescription() != null) entity.setOgDescription(dto.getOgDescription());
        if (dto.getOgImage() != null) entity.setOgImage(dto.getOgImage());
        if (dto.getRobotsDirective() != null) entity.setRobotsDirective(dto.getRobotsDirective());
        if (dto.getSlug() != null) entity.setSlug(dto.getSlug());
        if (dto.getCustomHeadHtml() != null) entity.setCustomHeadHtml(dto.getCustomHeadHtml());
    }
}
