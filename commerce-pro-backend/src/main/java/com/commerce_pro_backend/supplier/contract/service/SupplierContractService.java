package com.commerce_pro_backend.supplier.contract.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.contract.entity.SupplierContract;
import com.commerce_pro_backend.supplier.contract.repository.SupplierContractRepository;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupplierContractService {

    private final SupplierContractRepository supplierContractRepository;
    private final CurrentUserService currentUserService;

    public Page<SupplierContract> getAllContracts(Pageable pageable) {
        return supplierContractRepository.findAll(pageable);
    }

    public List<SupplierContract> getBySupplier(String supplierId) {
        return supplierContractRepository.findBySupplierIdOrderByStartDateDesc(supplierId);
    }

    public Page<SupplierContract> getByStatus(String status, Pageable pageable) {
        return supplierContractRepository.findByStatusOrderByStartDateDesc(status, pageable);
    }

    public SupplierContract getContract(String id) {
        return supplierContractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierContract", id));
    }

    @Transactional
    @CacheEvict(value = "supplier-contracts", allEntries = true)
    public SupplierContract createContract(SupplierContract request) {
        log.info("Creating supplier contract for supplier: {}", request.getSupplierId());

        request.setId(UUID.randomUUID().toString());
        request.setContractNumber(generateContractNumber());

        SupplierContract saved = supplierContractRepository.save(request);
        log.info("Created supplier contract with id: {}, contractNumber: {}", saved.getId(), saved.getContractNumber());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "supplier-contracts", allEntries = true)
    public SupplierContract updateContract(String id, SupplierContract request) {
        log.info("Updating supplier contract: {}", id);

        SupplierContract existing = supplierContractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierContract", id));

        existing.setSupplierId(request.getSupplierId());
        existing.setSupplierName(request.getSupplierName());
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setContractType(request.getContractType());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setTotalValue(request.getTotalValue());
        existing.setCurrency(request.getCurrency());
        existing.setPaymentTerms(request.getPaymentTerms());
        existing.setAutoRenew(request.getAutoRenew());
        existing.setRenewalNoticeDays(request.getRenewalNoticeDays());
        existing.setDocumentUrl(request.getDocumentUrl());
        existing.setTerms(request.getTerms());

        SupplierContract updated = supplierContractRepository.save(existing);
        log.info("Updated supplier contract with id: {}", updated.getId());
        return updated;
    }

    @Transactional
    @CacheEvict(value = "supplier-contracts", allEntries = true)
    public void deleteContract(String id) {
        log.info("Deleting supplier contract: {}", id);
        SupplierContract contract = supplierContractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierContract", id));

        if (!"DRAFT".equals(contract.getStatus())) {
            throw ApiException.badRequest("Can only delete contracts in DRAFT status");
        }

        supplierContractRepository.delete(contract);
        log.info("Deleted supplier contract with id: {}", id);
    }

    @Transactional
    @CacheEvict(value = "supplier-contracts", allEntries = true)
    public SupplierContract activateContract(String id) {
        log.info("Activating supplier contract: {}", id);

        SupplierContract contract = supplierContractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierContract", id));

        contract.setStatus("ACTIVE");

        SupplierContract updated = supplierContractRepository.save(contract);
        log.info("Activated supplier contract with id: {}", id);
        return updated;
    }

    @Transactional
    @CacheEvict(value = "supplier-contracts", allEntries = true)
    public SupplierContract terminateContract(String id) {
        log.info("Terminating supplier contract: {}", id);

        SupplierContract contract = supplierContractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SupplierContract", id));

        contract.setStatus("TERMINATED");

        SupplierContract updated = supplierContractRepository.save(contract);
        log.info("Terminated supplier contract with id: {}", id);
        return updated;
    }

    public List<SupplierContract> getExpiringContracts(int days) {
        Instant now = Instant.now();
        Instant deadline = now.plus(Duration.ofDays(days));

        return supplierContractRepository.findByEndDateBeforeAndStatusNot(deadline, "TERMINATED")
                .stream()
                .filter(c -> c.getEndDate() != null && c.getEndDate().isAfter(now))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getContractStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("draft", supplierContractRepository.countByStatus("DRAFT"));
        stats.put("active", supplierContractRepository.countByStatus("ACTIVE"));
        stats.put("expired", supplierContractRepository.countByStatus("EXPIRED"));
        stats.put("terminated", supplierContractRepository.countByStatus("TERMINATED"));
        stats.put("total", supplierContractRepository.count());

        List<SupplierContract> allContracts = supplierContractRepository.findAll();
        BigDecimal totalValue = allContracts.stream()
                .filter(c -> c.getTotalValue() != null)
                .map(SupplierContract::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalValue", totalValue);

        return stats;
    }

    private String generateContractNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        String contractNumber = "CON-" + datePart + "-" + randomPart;

        while (supplierContractRepository.existsByContractNumber(contractNumber)) {
            randomPart = String.format("%04d", (int) (Math.random() * 10000));
            contractNumber = "CON-" + datePart + "-" + randomPart;
        }

        return contractNumber;
    }
}
