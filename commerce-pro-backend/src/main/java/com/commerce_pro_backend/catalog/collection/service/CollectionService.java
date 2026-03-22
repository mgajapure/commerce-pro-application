package com.commerce_pro_backend.catalog.collection.service;

import com.commerce_pro_backend.catalog.collection.dto.CollectionDto;
import com.commerce_pro_backend.catalog.collection.entity.Collection;
import com.commerce_pro_backend.catalog.collection.entity.Collection.CollectionStatus;
import com.commerce_pro_backend.catalog.collection.entity.Collection.CollectionType;
import com.commerce_pro_backend.catalog.collection.mapper.CollectionMapper;
import com.commerce_pro_backend.catalog.collection.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionMapper collectionMapper;

    public Page<CollectionDto.ListResponse> getAllCollections(Pageable pageable) {
        return collectionRepository.findAll(pageable)
                .map(collectionMapper::toListResponse);
    }

    public CollectionDto.Response getCollection(String id) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection not found: " + id));
        return collectionMapper.toResponse(collection);
    }

    public CollectionDto.Response getCollectionBySlug(String slug) {
        Collection collection = collectionRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Collection not found: " + slug));
        return collectionMapper.toResponse(collection);
    }

    public List<CollectionDto.ListResponse> getFeaturedCollections() {
        return collectionRepository.findByIsFeaturedTrueAndStatusOrderBySortOrderAsc(CollectionStatus.ACTIVE)
                .stream()
                .map(collectionMapper::toListResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CollectionDto.Response createCollection(CollectionDto.Request request) {
        log.info("Creating collection: {}", request.getName());

        validateSlugUniqueness(request.getSlug(), null);

        Collection collection = collectionMapper.toEntity(request);
        Collection saved = collectionRepository.save(collection);

        log.info("Created collection with id: {}", saved.getId());
        return collectionMapper.toResponse(saved);
    }

    @Transactional
    public CollectionDto.Response updateCollection(String id, CollectionDto.Request request) {
        log.info("Updating collection: {}", id);

        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection not found: " + id));

        if (!collection.getSlug().equals(request.getSlug())) {
            validateSlugUniqueness(request.getSlug(), id);
        }

        collectionMapper.updateEntityFromDto(request, collection);
        Collection updated = collectionRepository.save(collection);
        return collectionMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCollection(String id) {
        log.info("Deleting collection: {}", id);
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection not found: " + id));
        collectionRepository.delete(collection);
    }

    @Transactional
    public CollectionDto.Response addProducts(String id, List<String> productIds) {
        log.info("Adding {} products to collection: {}", productIds.size(), id);
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection not found: " + id));

        if (collection.getType() != CollectionType.MANUAL) {
            throw new RuntimeException("Can only add products to manual collections");
        }

        collection.addProductIds(productIds);
        Collection saved = collectionRepository.save(collection);
        return collectionMapper.toResponse(saved);
    }

    @Transactional
    public CollectionDto.Response removeProducts(String id, List<String> productIds) {
        log.info("Removing {} products from collection: {}", productIds.size(), id);
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection not found: " + id));

        if (collection.getType() != CollectionType.MANUAL) {
            throw new RuntimeException("Can only remove products from manual collections");
        }

        collection.removeProductIds(productIds);
        Collection saved = collectionRepository.save(collection);
        return collectionMapper.toResponse(saved);
    }

    public CollectionDto.StatsResponse getStats() {
        return CollectionDto.StatsResponse.builder()
                .totalCollections(collectionRepository.count())
                .activeCollections(collectionRepository.countByStatus(CollectionStatus.ACTIVE))
                .draftCollections(collectionRepository.countByStatus(CollectionStatus.DRAFT))
                .archivedCollections(collectionRepository.countByStatus(CollectionStatus.ARCHIVED))
                .manualCollections(collectionRepository.countByType(CollectionType.MANUAL))
                .automatedCollections(collectionRepository.countByType(CollectionType.AUTOMATED))
                .featuredCollections(collectionRepository.countByIsFeaturedTrue())
                .build();
    }

    private void validateSlugUniqueness(String slug, String excludeId) {
        collectionRepository.findBySlug(slug).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new RuntimeException("Slug already exists: " + slug);
            }
        });
    }
}
