package com.commerce_pro_backend.supplier.directory.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.directory.entity.Supplier;
import com.commerce_pro_backend.supplier.directory.repository.SupplierRepository;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    public Page<Supplier> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable);
    }

    @Cacheable(value = "suppliers", key = "'all-active'")
    public List<Supplier> getAllActiveSuppliers() {
        return supplierRepository.findByIsActiveTrueOrderByNameAsc();
    }

    @Cacheable(value = "suppliers", key = "'preferred'")
    public List<Supplier> getPreferredSuppliers() {
        return supplierRepository.findByIsPreferredTrueAndIsActiveTrueOrderByNameAsc();
    }

    public Supplier getSupplier(String id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier", id));
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public Supplier createSupplier(Supplier request) {
        log.info("Creating supplier: {}", request.getName());

        validateCodeUniqueness(request.getCode(), null);

        request.setId(UUID.randomUUID().toString());

        Supplier saved = supplierRepository.save(request);
        log.info("Created supplier with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "suppliers", allEntries = true)
    public Supplier updateSupplier(String id, Supplier request) {
        log.info("Updating supplier: {}", id);

        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Supplier", id));

        if (!existing.getCode().equals(request.getCode())) {
            validateCodeUniqueness(request.getCode(), id);
        }

        existing.setName(request.getName());
        existing.setCode(request.getCode());
        existing.setContactPerson(request.getContactPerson());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setAddress(request.getAddress());
        existing.setCity(request.getCity());
        existing.setState(request.getState());
        existing.setCountry(request.getCountry());
        existing.setPostalCode(request.getPostalCode());
        existing.setWebsite(request.getWebsite());
        existing.setDescription(request.getDescription());
        existing.setPaymentTerms(request.getPaymentTerms());
        existing.setPaymentTermsDays(request.getPaymentTermsDays());
        existing.setLeadTimeDays(request.getLeadTimeDays());
        existing.setRating(request.getRating());
        existing.setMinOrderValue(request.getMinOrderValue());
        existing.setPreferredCurrency(request.getPreferredCurrency());
        existing.setCertifications(request.getCertifications());
        existing.setNotes(request.getNotes());

        Supplier updated = supplierRepository.save(existing);
        log.info("Updated supplier with id: {}", updated.getId());
        return updated;
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
        long active = supplierRepository.findByIsActiveTrueOrderByNameAsc().size();
        long preferred = supplierRepository.findByIsPreferredTrueAndIsActiveTrueOrderByNameAsc().size();

        List<Supplier> allSuppliers = supplierRepository.findAll();
        double avgRating = allSuppliers.stream()
                .mapToDouble(s -> s.getRating() != null ? s.getRating() : 0.0)
                .average()
                .orElse(0.0);

        stats.put("total", total);
        stats.put("active", active);
        stats.put("preferred", preferred);
        stats.put("avgRating", Math.round(avgRating * 100.0) / 100.0);
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
