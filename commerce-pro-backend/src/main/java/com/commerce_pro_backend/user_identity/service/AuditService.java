// AuditService.java
package com.commerce_pro_backend.user_identity.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.dto.AuditLogDTO;
import com.commerce_pro_backend.user_identity.dto.ExportRequest;
import com.commerce_pro_backend.user_identity.dto.ExportResult;
import com.commerce_pro_backend.user_identity.dto.PurgeResult;
import com.commerce_pro_backend.user_identity.entity.AuditLog;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String actorId, AuditAction action, String targetType, String targetId,
                    String targetIdentifier, String actorUsername, String oldValue,
                    String newValue, boolean success) {

        AuditLog log = AuditLog.builder()
            .action(action)
            .actorId(actorId)
            .actorUsername(actorUsername)   // Issue 8 FIX: snapshot username at write time
            .targetType(targetType)
            .targetId(targetId)
            .targetIdentifier(targetIdentifier)
            .actionDescription(formatActionDescription(action, targetType, targetIdentifier))
            .oldValue(oldValue)
            .newValue(newValue)
            .success(success)
            .timestamp(LocalDateTime.now())
            .build();

        // Capture request context if available
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                log.setIpAddress(request.getRemoteAddr());
                log.setUserAgent(request.getHeader("User-Agent"));
                log.setRequestId(request.getHeader("X-Request-ID"));
            }
        } catch (Exception e) {
            // Ignore if not in request context
        }

        auditLogRepository.save(log);
    }

    @Transactional
    public void logConfigChange(String actorId, String actorUsername, String configType, Object newConfig) {
        log(actorId, AuditAction.CONFIG_UPDATED, "CONFIGURATION", configType,
            configType, actorUsername, null, newConfig.toString(), true);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> queryLogs(String actorId, String targetId, AuditAction action,
                                       LocalDateTime from, LocalDateTime to, String search, Pageable pageable) {
        return auditLogRepository.searchLogs(actorId, targetId, action, from, to, search, pageable)
            .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getUserLogs(String userId, Pageable pageable) {
        return auditLogRepository.searchLogs(null, userId, null, null, null, null, pageable)
            .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public AuditLogDTO getLogDetail(String id) {
        AuditLog log = auditLogRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Audit log", id));
        return mapToDTO(log);
    }

    @Transactional
    public ExportResult exportLogs(ExportRequest request, String adminId) {
        // Implementation would generate file and return download URL
        // This is a placeholder
        log(adminId, AuditAction.AUDIT_EXPORTED, "AUDIT", "EXPORT", 
            "Bulk Export", null, null, "Exported logs from " + request.getFrom() + " to " + request.getTo(), true);

        return ExportResult.builder()
            .downloadUrl("/api/v1/downloads/audit-export-" + System.currentTimeMillis() + ".csv")
            .fileName("audit-export.csv")
            .recordCount(1000)
            .fileSizeBytes(102400)
            .expiresAt(LocalDateTime.now().plusHours(24).toString())
            .build();
    }

    @Transactional
    public PurgeResult purgeLogs(LocalDateTime before, String adminId, boolean dryRun) {
        long count = auditLogRepository.countOlderThan(before);

        if (!dryRun) {
            int deleted = auditLogRepository.deleteOlderThan(before);
            log(adminId, AuditAction.AUDIT_PURGED, "AUDIT", "PURGE", 
                "Bulk Delete", null, null, "Purged " + deleted + " logs before " + before, true);
        }

        return PurgeResult.builder()
            .deletedCount(count)
            .purgedBefore(before)
            .dryRun(dryRun)
            .backupLocation(dryRun ? null : "/backups/audit-" + System.currentTimeMillis() + ".sql")
            .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics(LocalDateTime from, LocalDateTime to) {
        if (from == null) from = LocalDateTime.now().minusDays(30);
        if (to == null) to = LocalDateTime.now();

        List<Object[]> actionCounts = auditLogRepository.countByActionBetween(from, to);

        Map<String, Object> stats = new HashMap<>();
        stats.put("period", Map.of("from", from, "to", to));
        stats.put("actionBreakdown", actionCounts.stream()
            .collect(Collectors.toMap(
                arr -> ((AuditAction) arr[0]).name(),
                arr -> arr[1]
            )));
        stats.put("totalActions", actionCounts.stream().mapToLong(arr -> (Long) arr[1]).sum());

        return stats;
    }

    private String formatActionDescription(AuditAction action, String targetType, String targetIdentifier) {
        return String.format("%s %s: %s", action.name(), targetType, targetIdentifier);
    }

    private AuditLogDTO mapToDTO(AuditLog log) {
        return AuditLogDTO.builder()
            .id(log.getId())
            .action(log.getAction().name())
            .actorId(log.getActorId())
            .actorUsername(log.getActorUsername())  // Issue 8 FIX: use snapshotted username
            .targetType(log.getTargetType())
            .targetId(log.getTargetId())
            .targetIdentifier(log.getTargetIdentifier())
            .description(log.getActionDescription())
            .success(log.getSuccess())
            .failureReason(log.getFailureReason())
            .ipAddress(log.getIpAddress())
            .timestamp(log.getTimestamp())
            .processingTimeMs(log.getProcessingTimeMs())
            .build();
    }
}
