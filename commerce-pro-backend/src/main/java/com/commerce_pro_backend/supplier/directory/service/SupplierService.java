package com.commerce_pro_backend.supplier.directory.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.directory.dto.SupplierDto;
import com.commerce_pro_backend.supplier.directory.entity.Supplier;
import com.commerce_pro_backend.supplier.directory.mapper.SupplierMapper;
import com.commerce_pro_backend.supplier.directory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public Page<SupplierDto.ListResponse> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable)
                .map(supplierMapper::toListResponse);
    }

    @Cacheable(value = "suppliers", key = "'all-active'")
    public List<SupplierDto.ListResponse> getAllActiveSuppliers() {
        return supplierMapper.toListResponseList(
                supplierRepository.findByIsActiveTrueOrderByNameAsc());
    }

    @Cacheable(value = "suppliers", key = "'preferred'")
    public List<SupplierDto.ListResponse> getPreferredSuppliers() {
        return supplierMapper.toListResponseList(
                supplierRepository.findByIsPreferredTrueAndIsActiveTrueOrderByNameAsc());
    }

    public SupplierDto.Response getSupplier(String id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier", id));
        return supplierMapper.toResponse(supplier);
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public SupplierDto.Response createSupplier(SupplierDto.Request request) {
        log.info("Creating supplier: {}", request.getName());

        validateCodeUniqueness(request.getCode(), null);

        Supplier supplier = supplierMapper.toEntity(request);
        supplier.setId(UUID.randomUUID().toString());

        Supplier saved = supplierRepository.save(supplier);
        log.info("Created supplier with id: {}", saved.getId());
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public SupplierDto.Response updateSupplier(String id, SupplierDto.Request request) {
        log.info("Updating supplier: {}", id);

        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier", id));

        if (!existing.getCode().equals(request.getCode())) {
            validateCodeUniqueness(request.getCode(), id);
        }

        supplierMapper.updateEntityFromDto(request, existing);

        Supplier updated = supplierRepository.save(existing);
        log.info("Updated supplier with id: {}", updated.getId());
        return supplierMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public void deleteSupplier(String id) {
        log.info("Deleting supplier: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier", id));
        supplierRepository.delete(supplier);
        log.info("Deleted supplier with id: {}", id);
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public void toggleActive(String id, boolean active) {
        log.info("Toggling supplier active status: id={}, active={}", id, active);
        supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier", id));
        supplierRepository.updateActiveStatus(id, active);
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public void togglePreferred(String id, boolean preferred) {
        log.info("Toggling supplier preferred status: id={}, preferred={}", id, preferred);
        supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier", id));
        supplierRepository.updatePreferredStatus(id, preferred);
    }

    public Map<String, Object> getSupplierStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = supplierRepository.count();
        long active = supplierRepository.countByIsActiveTrue();
        long preferred = supplierRepository.countByIsPreferredTrueAndIsActiveTrue();

        stats.put("total", total);
        stats.put("active", active);
        stats.put("preferred", preferred);
        return stats;
    }

    private void validateCodeUniqueness(String code, String excludeId) {
        supplierRepository.findByCode(code).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw ApiException.conflict("Supplier code already exists: " + code);
            }
        });
    }
}
