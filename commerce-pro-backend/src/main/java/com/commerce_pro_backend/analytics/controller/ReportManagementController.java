package com.commerce_pro_backend.analytics.controller;

import com.commerce_pro_backend.analytics.dto.*;
import com.commerce_pro_backend.analytics.repository.ReportExecutionRepository;
import com.commerce_pro_backend.analytics.service.*;
import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/analytics/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Report Management", description = "Saved reports, scheduled deliveries and execution history")
public class ReportManagementController {

    private final SavedReportService    savedReportService;
    private final ScheduledReportService scheduledService;
    private final ReportRunnerService   runnerService;
    private final ReportExecutionRepository executionRepo;
    private final CurrentUserService    currentUserService;

    // ── Saved Reports ─────────────────────────────────────────────────────────

    @PostMapping("/saved")
    @PreAuthorize("hasAuthority('analytics:reports:manage')")
    @Operation(summary = "Save a report definition", description = "Persist a named report with filter configuration for later re-use or scheduling.")
    public ResponseEntity<ApiResponse<SavedReportResponseDTO>> createSaved(@Valid @RequestBody SavedReportRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success("Report saved", savedReportService.create(req)));
    }

    @GetMapping("/saved")
    @PreAuthorize("hasAuthority('analytics:reports:read')")
    @Operation(summary = "List saved reports", description = "Returns all saved reports owned by the current user plus all public reports.")
    public ResponseEntity<ApiResponse<PageResponse<SavedReportResponseDTO>>> listSaved(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Saved reports retrieved", savedReportService.list(pageable)));
    }

    @GetMapping("/saved/{id}")
    @PreAuthorize("hasAuthority('analytics:reports:read')")
    @Operation(summary = "Get saved report by ID")
    public ResponseEntity<ApiResponse<SavedReportResponseDTO>> getSaved(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Saved report retrieved", savedReportService.getById(id)));
    }

    @PutMapping("/saved/{id}")
    @PreAuthorize("hasAuthority('analytics:reports:manage')")
    @Operation(summary = "Update saved report")
    public ResponseEntity<ApiResponse<SavedReportResponseDTO>> updateSaved(
            @PathVariable String id, @Valid @RequestBody SavedReportRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success("Report updated", savedReportService.update(id, req)));
    }

    @DeleteMapping("/saved/{id}")
    @PreAuthorize("hasAuthority('analytics:reports:manage')")
    @Operation(summary = "Delete saved report")
    public ResponseEntity<ApiResponse<Void>> deleteSaved(@PathVariable String id) {
        savedReportService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Report deleted", null));
    }

    /** Run a saved report on demand. */
    @PostMapping("/saved/{id}/run")
    @PreAuthorize("hasAuthority('analytics:reports:run')")
    @Operation(summary = "Run a saved report", description = "Executes the saved report immediately and returns an execution record with a download URL.")
    public ResponseEntity<ApiResponse<ReportExecutionDTO>> runSaved(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Report execution started", runnerService.runSaved(id)));
    }

    // ── Ad-hoc Run ────────────────────────────────────────────────────────────

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('analytics:reports:run')")
    @Operation(summary = "Run an ad-hoc report", description = "Executes a one-off report with the provided filter and returns a download URL when complete.")
    public ResponseEntity<ApiResponse<ReportExecutionDTO>> runAdHoc(@Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("Report executed", runnerService.runAdHoc(filter)));
    }

    // ── Scheduled Reports ─────────────────────────────────────────────────────

    @PostMapping("/scheduled")
    @PreAuthorize("hasAuthority('analytics:reports:manage')")
    @Operation(summary = "Create a scheduled report delivery",
               description = "Schedules a saved report to be generated and emailed to recipients at the specified frequency.")
    public ResponseEntity<ApiResponse<ScheduledReportResponseDTO>> createScheduled(
            @Valid @RequestBody ScheduledReportRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success("Scheduled report created", scheduledService.create(req)));
    }

    @GetMapping("/scheduled")
    @PreAuthorize("hasAuthority('analytics:reports:read')")
    @Operation(summary = "List scheduled reports")
    public ResponseEntity<ApiResponse<PageResponse<ScheduledReportResponseDTO>>> listScheduled(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Scheduled reports retrieved", scheduledService.list(pageable)));
    }

    @GetMapping("/scheduled/{id}")
    @PreAuthorize("hasAuthority('analytics:reports:read')")
    @Operation(summary = "Get scheduled report by ID")
    public ResponseEntity<ApiResponse<ScheduledReportResponseDTO>> getScheduled(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Scheduled report retrieved", scheduledService.getById(id)));
    }

    @PutMapping("/scheduled/{id}")
    @PreAuthorize("hasAuthority('analytics:reports:manage')")
    @Operation(summary = "Update scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportResponseDTO>> updateScheduled(
            @PathVariable String id, @Valid @RequestBody ScheduledReportRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success("Scheduled report updated", scheduledService.update(id, req)));
    }

    @DeleteMapping("/scheduled/{id}")
    @PreAuthorize("hasAuthority('analytics:reports:manage')")
    @Operation(summary = "Delete scheduled report")
    public ResponseEntity<ApiResponse<Void>> deleteScheduled(@PathVariable String id) {
        scheduledService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Scheduled report deleted", null));
    }

    @PostMapping("/scheduled/{id}/toggle")
    @PreAuthorize("hasAuthority('analytics:reports:manage')")
    @Operation(summary = "Toggle scheduled report active/inactive")
    public ResponseEntity<ApiResponse<ScheduledReportResponseDTO>> toggleScheduled(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Scheduled report toggled", scheduledService.toggleActive(id)));
    }

    @PostMapping("/scheduled/{id}/run-now")
    @PreAuthorize("hasAuthority('analytics:reports:run')")
    @Operation(summary = "Trigger a scheduled report immediately")
    public ResponseEntity<ApiResponse<ReportExecutionDTO>> runScheduledNow(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Scheduled report triggered", runnerService.runScheduled(id)));
    }

    // ── Execution History ─────────────────────────────────────────────────────

    @GetMapping("/executions")
    @PreAuthorize("hasAuthority('analytics:reports:read')")
    @Operation(summary = "List my report execution history")
    public ResponseEntity<ApiResponse<PageResponse<ReportExecutionDTO>>> listExecutions(
            @PageableDefault(size = 20) Pageable pageable) {
        String userId = currentUserService.getCurrentUserId();
        var page = executionRepo.findRecentByUser(userId, pageable);
        var dtos = page.getContent().stream().map(e -> {
            String dl = e.getFilePath() != null ? "/api/files/download/" + e.getFilePath() : null;
            return ReportExecutionDTO.builder()
                    .id(e.getId()).reportType(e.getReportType()).status(e.getStatus())
                    .format(e.getFormat()).fileName(e.getFileName())
                    .fileSizeBytes(e.getFileSizeBytes()).rowCount(e.getRowCount())
                    .errorMessage(e.getErrorMessage()).startedAt(e.getStartedAt())
                    .completedAt(e.getCompletedAt()).durationMs(e.getDurationMs())
                    .downloadUrl(dl).savedReportId(e.getSavedReportId())
                    .scheduledReportId(e.getScheduledReportId())
                    .requestedBy(e.getRequestedBy()).createdAt(e.getCreatedAt())
                    .build();
        }).toList();
        return ResponseEntity.ok(ApiResponse.success("Executions retrieved",
                PageResponse.from(page, dtos)));
    }

    @GetMapping("/executions/{id}")
    @PreAuthorize("hasAuthority('analytics:reports:read')")
    @Operation(summary = "Get execution by ID")
    public ResponseEntity<ApiResponse<ReportExecutionDTO>> getExecution(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Execution retrieved", runnerService.getExecution(id)));
    }
}
