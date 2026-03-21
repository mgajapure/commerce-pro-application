package com.commerce_pro_backend.analytics.entity;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * SavedReport — a reusable report definition created by a user.
 * Contains the filter parameters serialised as JSON so the report can be
 * re-run at any time or used as the basis for a scheduled delivery.
 */
@Entity
@Table(name = "saved_reports", indexes = {
        @Index(name = "idx_saved_report_type",       columnList = "report_type"),
        @Index(name = "idx_saved_report_created_by", columnList = "created_by"),
        @Index(name = "idx_saved_report_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedReport {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    /** JSON-serialised filter parameters (date range, grouping, filters, columns). */
    @Column(name = "filter_params", columnDefinition = "TEXT")
    private String filterParams;

    /** Default export format when this report is run. */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_format", length = 10)
    @Builder.Default
    private ExportFormat defaultFormat = ExportFormat.EXCEL;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

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
