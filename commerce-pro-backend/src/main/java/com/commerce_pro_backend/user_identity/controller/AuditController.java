package com.commerce_pro_backend.user_identity.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.user_identity.dto.AuditLogDTO;
import com.commerce_pro_backend.user_identity.dto.ExportRequest;
import com.commerce_pro_backend.user_identity.dto.ExportResult;
import com.commerce_pro_backend.user_identity.dto.PurgeResult;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/identity/audit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit & Compliance", description = "View and manage audit logs")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('identity:audit:read')")
    @Operation(summary = "Query audit logs with filters")
    public ApiResponse<Page<AuditLogDTO>> queryAuditLogs(
            @Parameter(description = "Filter by actor (admin) ID")
            @RequestParam(name = "actorId", required = false) String actorId,
            @Parameter(description = "Filter by target user ID")
            @RequestParam(name = "targetId", required = false) String targetId,
            @Parameter(description = "Filter by action type")
            @RequestParam(name = "action", required = false) AuditAction action,
            @Parameter(description = "Start date (ISO format)")
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End date (ISO format)")
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Full-text search")
            @RequestParam(name = "search", required = false) String search,
            Pageable pageable) {
        return ApiResponse.success(auditService.queryLogs(actorId, targetId, action, from, to, search, pageable));
    }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasAuthority('identity:audit:read')")
    @Operation(summary = "Get detailed audit log entry")
    public ApiResponse<AuditLogDTO> getAuditLog(@PathVariable String id) {
        return ApiResponse.success(auditService.getLogDetail(id));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('identity:audit:export')")
    @Operation(summary = "Export audit logs")
    public ApiResponse<ExportResult> exportAuditLogs(
            @RequestBody ExportRequest request,
            @RequestHeader("X-Admin-Id") String adminId) {
        return ApiResponse.success(auditService.exportLogs(request, adminId));
    }

    @DeleteMapping("/purge")
    @PreAuthorize("hasAuthority('identity:audit:purge')")
    @Operation(summary = "Purge old audit logs (use with caution)")
    public ApiResponse<PurgeResult> purgeAuditLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestHeader("X-Admin-Id") String adminId,
            @RequestParam(name = "dryRun", required = false, defaultValue = "false") boolean dryRun) {
        return ApiResponse.success(auditService.purgeLogs(before, adminId, dryRun));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('identity:audit:read')")
    @Operation(summary = "Get audit statistics")
    public ApiResponse<Map<String, Object>> getAuditStats(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.success(auditService.getStatistics(from, to));
    }

    @GetMapping("/actions")
    @PreAuthorize("hasAuthority('identity:audit:read')")
    @Operation(summary = "List available audit action types")
    public ApiResponse<List<AuditAction>> getActionTypes() {
        return ApiResponse.success(List.of(AuditAction.values()));
    }
}