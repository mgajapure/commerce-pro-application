package com.commerce_pro_backend.analytics.dto;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ReportStatus;
import com.commerce_pro_backend.analytics.enums.ReportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportExecutionDTO {
    private String id;
    private ReportType reportType;
    private ReportStatus status;
    private ExportFormat format;
    private String fileName;
    private Long fileSizeBytes;
    private Integer rowCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    /** Pre-built download URL pointing to FileStorageController. */
    private String downloadUrl;
    private String savedReportId;
    private String scheduledReportId;
    private String requestedBy;
    private LocalDateTime createdAt;
}
