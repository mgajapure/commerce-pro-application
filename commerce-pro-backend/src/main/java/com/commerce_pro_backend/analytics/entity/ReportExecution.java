package com.commerce_pro_backend.analytics.entity;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ReportStatus;
import com.commerce_pro_backend.analytics.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * ReportExecution — immutable audit record of every report run (on-demand or scheduled).
 * The generated file path (relative to the uploads directory) is stored so the
 * file can be served through the existing FileStorageController download endpoint.
 */
@Entity
@Table(name = "report_executions", indexes = {
        @Index(name = "idx_exec_report_type",     columnList = "report_type"),
        @Index(name = "idx_exec_status",          columnList = "status"),
        @Index(name = "idx_exec_requested_by",    columnList = "requested_by"),
        @Index(name = "idx_exec_created_at",      columnList = "created_at"),
        @Index(name = "idx_exec_saved_report",    columnList = "saved_report_id"),
        @Index(name = "idx_exec_scheduled_report",columnList = "scheduled_report_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecution {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExportFormat format;

    /** The filter parameters used for this run (JSON). */
    @Column(name = "filter_params", columnDefinition = "TEXT")
    private String filterParams;

    /** Relative path inside uploads/ to the generated file. */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** Original file name presented to the user on download. */
    @Column(name = "file_name", length = 300)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Duration in milliseconds. */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** FK to SavedReport (nullable for ad-hoc runs). */
    @Column(name = "saved_report_id", length = 36)
    private String savedReportId;

    /** FK to ScheduledReport (nullable for manual runs). */
    @Column(name = "scheduled_report_id", length = 36)
    private String scheduledReportId;

    @Column(name = "requested_by", length = 36)
    private String requestedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
