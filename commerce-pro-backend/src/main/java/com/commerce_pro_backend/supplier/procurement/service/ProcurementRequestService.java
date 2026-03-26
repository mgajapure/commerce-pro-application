package com.commerce_pro_backend.supplier.procurement.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.procurement.entity.ProcurementRequest;
import com.commerce_pro_backend.supplier.procurement.repository.ProcurementRequestRepository;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProcurementRequestService {

    private final ProcurementRequestRepository procurementRequestRepository;
    private final CurrentUserService currentUserService;

    public Page<ProcurementRequest> getAllProcurementRequests(Pageable pageable) {
        return procurementRequestRepository.findAll(pageable);
    }

    public Page<ProcurementRequest> getByStatus(String status, Pageable pageable) {
        return procurementRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public ProcurementRequest getProcurementRequest(String id) {
        return procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));
    }

    @Transactional
    @CacheEvict(value = "procurement-requests", allEntries = true)
    public ProcurementRequest createProcurementRequest(ProcurementRequest request) {
        log.info("Creating procurement request: {}", request.getTitle());

        request.setId(UUID.randomUUID().toString());
        request.setRequestNumber(generateRequestNumber());
        request.setRequestedBy(currentUserService.getCurrentUserId());

        ProcurementRequest saved = procurementRequestRepository.save(request);
        log.info("Created procurement request with id: {}, requestNumber: {}", saved.getId(), saved.getRequestNumber());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "procurement-requests", allEntries = true)
    public ProcurementRequest updateProcurementRequest(String id, ProcurementRequest request) {
        log.info("Updating procurement request: {}", id);

        ProcurementRequest existing = procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setPriority(request.getPriority());
        existing.setSupplierId(request.getSupplierId());
        existing.setSupplierName(request.getSupplierName());
        existing.setEstimatedCost(request.getEstimatedCost());
        existing.setActualCost(request.getActualCost());
        existing.setCurrency(request.getCurrency());
        existing.setRequiredByDate(request.getRequiredByDate());
        existing.setNotes(request.getNotes());

        ProcurementRequest updated = procurementRequestRepository.save(existing);
        log.info("Updated procurement request with id: {}", updated.getId());
        return updated;
    }

    @Transactional
    @CacheEvict(value = "procurement-requests", allEntries = true)
    public void deleteProcurementRequest(String id) {
        log.info("Deleting procurement request: {}", id);
        ProcurementRequest procurementRequest = procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));

        if (!"DRAFT".equals(procurementRequest.getStatus())) {
            throw ApiException.badRequest("Can only delete procurement requests in DRAFT status");
        }

        procurementRequestRepository.delete(procurementRequest);
        log.info("Deleted procurement request with id: {}", id);
    }

    @Transactional
    @CacheEvict(value = "procurement-requests", allEntries = true)
    public ProcurementRequest approveProcurementRequest(String id) {
        log.info("Approving procurement request: {}", id);

        ProcurementRequest procurementRequest = procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));

        procurementRequest.setStatus("APPROVED");

        ProcurementRequest updated = procurementRequestRepository.save(procurementRequest);
        log.info("Approved procurement request with id: {}", id);
        return updated;
    }

    @Transactional
    @CacheEvict(value = "procurement-requests", allEntries = true)
    public ProcurementRequest rejectProcurementRequest(String id, String reason) {
        log.info("Rejecting procurement request: {}, reason: {}", id, reason);

        ProcurementRequest procurementRequest = procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));

        procurementRequest.setStatus("REJECTED");
        procurementRequest.setNotes(reason);

        ProcurementRequest updated = procurementRequestRepository.save(procurementRequest);
        log.info("Rejected procurement request with id: {}", id);
        return updated;
    }

    public Map<String, Object> getProcurementStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("draft", procurementRequestRepository.countByStatus("DRAFT"));
        stats.put("submitted", procurementRequestRepository.countByStatus("SUBMITTED"));
        stats.put("approved", procurementRequestRepository.countByStatus("APPROVED"));
        stats.put("rejected", procurementRequestRepository.countByStatus("REJECTED"));
        stats.put("completed", procurementRequestRepository.countByStatus("COMPLETED"));
        stats.put("total", procurementRequestRepository.count());
        return stats;
    }

    private String generateRequestNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        String requestNumber = "PR-" + datePart + "-" + randomPart;

        while (procurementRequestRepository.existsByRequestNumber(requestNumber)) {
            randomPart = String.format("%04d", (int) (Math.random() * 10000));
            requestNumber = "PR-" + datePart + "-" + randomPart;
        }

        return requestNumber;
    }
}
