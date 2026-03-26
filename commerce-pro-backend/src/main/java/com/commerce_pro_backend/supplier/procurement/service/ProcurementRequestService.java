package com.commerce_pro_backend.supplier.procurement.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.supplier.procurement.dto.ProcurementRequestDto;
import com.commerce_pro_backend.supplier.procurement.entity.ProcurementRequest;
import com.commerce_pro_backend.supplier.procurement.mapper.ProcurementRequestMapper;
import com.commerce_pro_backend.supplier.procurement.repository.ProcurementRequestRepository;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProcurementRequestMapper procurementRequestMapper;
    private final CurrentUserService currentUserService;

    public Page<ProcurementRequestDto.ListResponse> getAllProcurementRequests(Pageable pageable) {
        return procurementRequestRepository.findAll(pageable)
                .map(procurementRequestMapper::toListResponse);
    }

    public Page<ProcurementRequestDto.ListResponse> getByStatus(String status, Pageable pageable) {
        return procurementRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(procurementRequestMapper::toListResponse);
    }

    public ProcurementRequestDto.Response getProcurementRequest(String id) {
        ProcurementRequest entity = procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));
        return procurementRequestMapper.toResponse(entity);
    }

    @Transactional
    @CacheEvict(value = "procurement-requests", allEntries = true)
    public ProcurementRequestDto.Response createProcurementRequest(ProcurementRequestDto.Request request) {
        log.info("Creating procurement request: {}", request.getTitle());

        ProcurementRequest entity = procurementRequestMapper.toEntity(request);
        entity.setId(UUID.randomUUID().toString());
        entity.setRequestNumber(generateRequestNumber());
        entity.setRequestedBy(currentUserService.getCurrentUserId());

        ProcurementRequest saved = procurementRequestRepository.save(entity);
        log.info("Created procurement request with id: {}, requestNumber: {}", saved.getId(), saved.getRequestNumber());
        return procurementRequestMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "procurement-requests", allEntries = true)
    public ProcurementRequestDto.Response updateProcurementRequest(String id, ProcurementRequestDto.Request request) {
        log.info("Updating procurement request: {}", id);

        ProcurementRequest existing = procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));

        procurementRequestMapper.updateEntityFromDto(request, existing);

        ProcurementRequest updated = procurementRequestRepository.save(existing);
        log.info("Updated procurement request with id: {}", updated.getId());
        return procurementRequestMapper.toResponse(updated);
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
    public ProcurementRequestDto.Response approveProcurementRequest(String id) {
        log.info("Approving procurement request: {}", id);

        ProcurementRequest procurementRequest = procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));

        procurementRequest.setStatus("APPROVED");

        ProcurementRequest updated = procurementRequestRepository.save(procurementRequest);
        log.info("Approved procurement request with id: {}", id);
        return procurementRequestMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "procurement-requests", allEntries = true)
    public ProcurementRequestDto.Response rejectProcurementRequest(String id, String reason) {
        log.info("Rejecting procurement request: {}, reason: {}", id, reason);

        ProcurementRequest procurementRequest = procurementRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ProcurementRequest", id));

        procurementRequest.setStatus("REJECTED");
        String existingNotes = procurementRequest.getNotes();
        procurementRequest.setNotes(existingNotes != null ? existingNotes + " | Rejection reason: " + reason : "Rejection reason: " + reason);

        ProcurementRequest updated = procurementRequestRepository.save(procurementRequest);
        log.info("Rejected procurement request with id: {}", id);
        return procurementRequestMapper.toResponse(updated);
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
