package com.commerce_pro_backend.catalog.attribute.service;

import com.commerce_pro_backend.catalog.attribute.dto.AttributeDto;
import com.commerce_pro_backend.catalog.attribute.entity.Attribute;
import com.commerce_pro_backend.catalog.attribute.mapper.AttributeMapper;
import com.commerce_pro_backend.catalog.attribute.repository.AttributeRepository;
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
public class AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeMapper attributeMapper;

    public Page<AttributeDto.ListResponse> getAllAttributes(Pageable pageable) {
        return attributeRepository.findAll(pageable)
                .map(attributeMapper::toListResponse);
    }

    public List<AttributeDto.Response> getAllActiveAttributes() {
        return attributeRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(attributeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public AttributeDto.Response getAttribute(String id) {
        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute not found: " + id));
        return attributeMapper.toResponse(attribute);
    }

    @Transactional
    public AttributeDto.Response createAttribute(AttributeDto.Request request) {
        log.info("Creating attribute: {}", request.getName());

        validateCodeUniqueness(request.getCode(), null);

        Attribute attribute = attributeMapper.toEntity(request);
        Attribute saved = attributeRepository.save(attribute);

        log.info("Created attribute with id: {}", saved.getId());
        return attributeMapper.toResponse(saved);
    }

    @Transactional
    public AttributeDto.Response updateAttribute(String id, AttributeDto.Request request) {
        log.info("Updating attribute: {}", id);

        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute not found: " + id));

        if (!attribute.getCode().equals(request.getCode())) {
            validateCodeUniqueness(request.getCode(), id);
        }

        attributeMapper.updateEntityFromDto(request, attribute);
        Attribute updated = attributeRepository.save(attribute);
        return attributeMapper.toResponse(updated);
    }

    @Transactional
    public void deleteAttribute(String id) {
        log.info("Deleting attribute: {}", id);
        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute not found: " + id));
        attributeRepository.delete(attribute);
    }

    @Transactional
    public void toggleActive(String id, boolean active) {
        log.info("Toggling attribute {} active status to: {}", id, active);
        int updated = attributeRepository.updateActiveStatus(id, active);
        if (updated == 0) {
            throw new RuntimeException("Attribute not found: " + id);
        }
    }

    private void validateCodeUniqueness(String code, String excludeId) {
        attributeRepository.findByCode(code).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new RuntimeException("Attribute code already exists: " + code);
            }
        });
    }
}
