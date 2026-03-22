package com.commerce_pro_backend.catalog.seo.service;

import com.commerce_pro_backend.catalog.seo.dto.SeoDto;
import com.commerce_pro_backend.catalog.seo.entity.SeoMetadata;
import com.commerce_pro_backend.catalog.seo.entity.SeoMetadata.EntityType;
import com.commerce_pro_backend.catalog.seo.mapper.SeoMapper;
import com.commerce_pro_backend.catalog.seo.repository.SeoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SeoService {

    private final SeoMetadataRepository seoMetadataRepository;
    private final SeoMapper seoMapper;

    public Page<SeoDto.ListResponse> getAllSeoEntries(Pageable pageable) {
        return seoMetadataRepository.findAll(pageable)
                .map(seoMapper::toListResponse);
    }

    public SeoDto.Response getSeoForEntity(String entityType, String entityId) {
        EntityType type = EntityType.valueOf(entityType.toUpperCase());
        SeoMetadata seo = seoMetadataRepository.findByEntityTypeAndEntityId(type, entityId)
                .orElseThrow(() -> new RuntimeException("SEO metadata not found for " + entityType + "/" + entityId));
        return seoMapper.toResponse(seo);
    }

    @Transactional
    public SeoDto.Response upsertSeoForEntity(String entityType, String entityId, SeoDto.Request request) {
        EntityType type = EntityType.valueOf(entityType.toUpperCase());
        log.info("Upserting SEO metadata for {}/{}", entityType, entityId);

        Optional<SeoMetadata> existing = seoMetadataRepository.findByEntityTypeAndEntityId(type, entityId);

        SeoMetadata seo;
        if (existing.isPresent()) {
            seo = existing.get();
            seoMapper.updateEntityFromDto(request, seo);
        } else {
            seo = seoMapper.toEntity(request, type, entityId);
        }

        // Run audit to calculate score
        SeoDto.SeoAuditResult audit = performAudit(seo);
        seo.setScore(audit.getScore());

        SeoMetadata saved = seoMetadataRepository.save(seo);
        return seoMapper.toResponse(saved);
    }

    @Transactional
    public void deleteSeoForEntity(String entityType, String entityId) {
        EntityType type = EntityType.valueOf(entityType.toUpperCase());
        log.info("Deleting SEO metadata for {}/{}", entityType, entityId);
        seoMetadataRepository.deleteByEntityTypeAndEntityId(type, entityId);
    }

    public SeoDto.SeoAuditResult auditSeoForEntity(String entityType, String entityId) {
        EntityType type = EntityType.valueOf(entityType.toUpperCase());
        SeoMetadata seo = seoMetadataRepository.findByEntityTypeAndEntityId(type, entityId)
                .orElseThrow(() -> new RuntimeException("SEO metadata not found for " + entityType + "/" + entityId));
        return performAudit(seo);
    }

    public SeoDto.StatsResponse getStats() {
        Double avgScore = seoMetadataRepository.getAverageScore();
        List<Object[]> byType = seoMetadataRepository.countByEntityTypeGrouped();

        Map<String, Long> entriesByType = new HashMap<>();
        for (Object[] row : byType) {
            entriesByType.put(((EntityType) row[0]).name(), (Long) row[1]);
        }

        return SeoDto.StatsResponse.builder()
                .totalEntries(seoMetadataRepository.count())
                .averageScore(avgScore != null ? avgScore : 0.0)
                .entriesWithScore80Plus(seoMetadataRepository.countWithScore80Plus())
                .entriesWithScoreBelow50(seoMetadataRepository.countWithScoreBelow50())
                .entriesByEntityType(entriesByType)
                .build();
    }

    private SeoDto.SeoAuditResult performAudit(SeoMetadata seo) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> passed = new ArrayList<>();
        Map<String, String> suggestions = new HashMap<>();
        int score = 0;
        int maxScore = 0;

        // Meta title check (20 points)
        maxScore += 20;
        if (seo.getMetaTitle() != null && !seo.getMetaTitle().isBlank()) {
            int len = seo.getMetaTitle().length();
            if (len >= 30 && len <= 60) {
                score += 20;
                passed.add("Meta title has optimal length (" + len + " chars)");
            } else if (len < 30) {
                score += 10;
                warnings.add("Meta title is too short (" + len + " chars, recommended 30-60)");
                suggestions.put("metaTitle", "Expand your title to 30-60 characters for better SEO");
            } else {
                score += 10;
                warnings.add("Meta title is too long (" + len + " chars, recommended 30-60)");
                suggestions.put("metaTitle", "Shorten your title to 30-60 characters");
            }
        } else {
            issues.add("Meta title is missing");
            suggestions.put("metaTitle", "Add a descriptive meta title (30-60 characters)");
        }

        // Meta description check (20 points)
        maxScore += 20;
        if (seo.getMetaDescription() != null && !seo.getMetaDescription().isBlank()) {
            int len = seo.getMetaDescription().length();
            if (len >= 120 && len <= 160) {
                score += 20;
                passed.add("Meta description has optimal length (" + len + " chars)");
            } else if (len < 120) {
                score += 10;
                warnings.add("Meta description is too short (" + len + " chars, recommended 120-160)");
                suggestions.put("metaDescription", "Expand description to 120-160 characters");
            } else {
                score += 10;
                warnings.add("Meta description is too long (" + len + " chars, recommended 120-160)");
                suggestions.put("metaDescription", "Shorten description to 120-160 characters");
            }
        } else {
            issues.add("Meta description is missing");
            suggestions.put("metaDescription", "Add a compelling meta description (120-160 characters)");
        }

        // Meta keywords check (10 points)
        maxScore += 10;
        if (seo.getMetaKeywords() != null && !seo.getMetaKeywords().isBlank()) {
            score += 10;
            passed.add("Meta keywords are defined");
        } else {
            warnings.add("Meta keywords are missing");
            suggestions.put("metaKeywords", "Add relevant keywords separated by commas");
        }

        // Canonical URL check (10 points)
        maxScore += 10;
        if (seo.getCanonicalUrl() != null && !seo.getCanonicalUrl().isBlank()) {
            score += 10;
            passed.add("Canonical URL is set");
        } else {
            warnings.add("Canonical URL is not set");
            suggestions.put("canonicalUrl", "Set a canonical URL to prevent duplicate content issues");
        }

        // Open Graph title check (10 points)
        maxScore += 10;
        if (seo.getOgTitle() != null && !seo.getOgTitle().isBlank()) {
            score += 10;
            passed.add("Open Graph title is set");
        } else {
            warnings.add("Open Graph title is missing");
            suggestions.put("ogTitle", "Add an OG title for better social sharing");
        }

        // Open Graph description check (10 points)
        maxScore += 10;
        if (seo.getOgDescription() != null && !seo.getOgDescription().isBlank()) {
            score += 10;
            passed.add("Open Graph description is set");
        } else {
            warnings.add("Open Graph description is missing");
            suggestions.put("ogDescription", "Add an OG description for better social sharing");
        }

        // Open Graph image check (10 points)
        maxScore += 10;
        if (seo.getOgImage() != null && !seo.getOgImage().isBlank()) {
            score += 10;
            passed.add("Open Graph image is set");
        } else {
            warnings.add("Open Graph image is missing");
            suggestions.put("ogImage", "Add an OG image for visual social sharing");
        }

        // Slug check (10 points)
        maxScore += 10;
        if (seo.getSlug() != null && !seo.getSlug().isBlank()) {
            if (seo.getSlug().matches("^[a-z0-9-]+$")) {
                score += 10;
                passed.add("Slug is SEO-friendly");
            } else {
                score += 5;
                warnings.add("Slug contains non-SEO-friendly characters");
                suggestions.put("slug", "Use lowercase letters, numbers, and hyphens only");
            }
        } else {
            warnings.add("Slug is not set");
            suggestions.put("slug", "Add a URL-friendly slug");
        }

        // Calculate percentage
        int finalScore = maxScore > 0 ? (int) Math.round((double) score / maxScore * 100) : 0;

        return SeoDto.SeoAuditResult.builder()
                .score(finalScore)
                .issues(issues)
                .warnings(warnings)
                .passed(passed)
                .suggestions(suggestions)
                .build();
    }
}
