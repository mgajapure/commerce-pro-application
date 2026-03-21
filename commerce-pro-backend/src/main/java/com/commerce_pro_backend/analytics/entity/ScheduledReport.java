package com.commerce_pro_backend.analytics.entity;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ScheduleFrequency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * ScheduledReport — drives the report scheduler.
 * Each row describes a recurring delivery: which saved report to run,
 * how often, what format, and where to send it.
 */
@Entity
@Table(name = "scheduled_reports", indexes = {
        @Index(name = "idx_sched_report_saved",      columnList = "saved_report_id"),
        @Index(name = "idx_sched_report_active",     columnList = "is_active"),
        @Index(name = "idx_sched_report_next_run",   columnList = "next_run_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReport {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "saved_report_id", nullable = false, length = 36)
    private String savedReportId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleFrequency frequency;

    /** Day of week (1=Mon…7=Sun) for WEEKLY; day of month for MONTHLY. */
    @Column(name = "day_of_period")
    private Integer dayOfPeriod;

    /** Local time at which the report should be generated and sent. */
    @Column(name = "run_at_time")
    private LocalTime runAtTime;

    /** Comma-separated list of recipient e-mail addresses. */
    @Column(name = "recipient_emails", columnDefinition = "TEXT", nullable = false)
    private String recipientEmails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ExportFormat format = ExportFormat.EXCEL;

    @Column(name = "email_subject", length = 300)
    private String emailSubject;

    @Column(name = "email_body", length = 2000)
    private String emailBody;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "run_count")
    @Builder.Default
    private Integer runCount = 0;

    @Column(name = "last_run_status", length = 30)
    private String lastRunStatus;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
