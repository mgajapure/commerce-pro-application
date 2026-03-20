package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import com.commerce_pro_backend.analytics.dto.SavedReportRequestDTO;
import com.commerce_pro_backend.analytics.dto.SavedReportResponseDTO;
import com.commerce_pro_backend.analytics.entity.SavedReport;
import com.commerce_pro_backend.analytics.repository.SavedReportRepository;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SavedReportService {

    private final SavedReportRepository savedReportRepository;
    private final CurrentUserService    currentUserService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public SavedReportResponseDTO create(SavedReportRequestDTO req) {
        String userId = currentUserService.getCurrentUserId();
        SavedReport report = SavedReport.builder()
                .name(req.getName())
                .description(req.getDescription())
                .reportType(req.getReportType())
                .filterParams(toJson(req.getFilterParams()))
                .defaultFormat(req.getDefaultFormat())
                .isPublic(req.getIsPublic() != null && req.getIsPublic())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        return toDTO(savedReportRepository.save(report));
    }

    public PageResponse<SavedReportResponseDTO> list(Pageable pageable) {
        String userId = currentUserService.getCurrentUserId();
        Page<SavedReport> page = savedReportRepository.findByCreatedByOrIsPublicTrue(userId, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDTO).toList());
    }

    public SavedReportResponseDTO getById(String id) {
        SavedReport report = findOrThrow(id);
        checkAccess(report);
        return toDTO(report);
    }

    @Transactional
    public SavedReportResponseDTO update(String id, SavedReportRequestDTO req) {
        String userId = currentUserService.getCurrentUserId();
        SavedReport report = findOrThrow(id);
        checkOwnership(report);
        report.setName(req.getName());
        report.setDescription(req.getDescription());
        report.setReportType(req.getReportType());
        report.setFilterParams(toJson(req.getFilterParams()));
        report.setDefaultFormat(req.getDefaultFormat());
        report.setIsPublic(req.getIsPublic() != null && req.getIsPublic());
        report.setUpdatedBy(userId);
        return toDTO(savedReportRepository.save(report));
    }

    @Transactional
    public void delete(String id) {
        SavedReport report = findOrThrow(id);
        checkOwnership(report);
        savedReportRepository.delete(report);
    }

    public ReportFilterDTO getFilters(String id) {
        SavedReport report = findOrThrow(id);
        checkAccess(report);
        return fromJson(report.getFilterParams());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SavedReport findOrThrow(String id) {
        return savedReportRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SavedReport", id));
    }

    private void checkAccess(SavedReport report) {
        String userId = currentUserService.getCurrentUserId();
        if (!report.getIsPublic() && !report.getCreatedBy().equals(userId)) {
            throw ApiException.forbidden("You do not have access to this saved report");
        }
    }

    private void checkOwnership(SavedReport report) {
        String userId = currentUserService.getCurrentUserId();
        if (!report.getCreatedBy().equals(userId)) {
            throw ApiException.forbidden("You can only modify your own saved reports");
        }
    }

    private SavedReportResponseDTO toDTO(SavedReport r) {
        return SavedReportResponseDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .reportType(r.getReportType())
                .filterParams(fromJson(r.getFilterParams()))
                .defaultFormat(r.getDefaultFormat())
                .isPublic(r.getIsPublic())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .createdBy(r.getCreatedBy())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return null; }
    }

    private ReportFilterDTO fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readValue(json, ReportFilterDTO.class); }
        catch (Exception e) { return null; }
    }
}
