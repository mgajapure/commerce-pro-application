package com.commerce_pro_backend.analytics.dto;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Universal filter parameters that drive every report type.
 * Fields unused by a particular report type are silently ignored.
 */
@Data
@Builder
public class ReportFilterDTO {

    @NotNull
    private ReportType reportType;

    // ── Date range ────────────────────────────────────────────────────────────
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    /**
     * Convenience: "TODAY", "LAST_7_DAYS", "LAST_30_DAYS", "THIS_MONTH",
     * "LAST_MONTH", "THIS_QUARTER", "THIS_YEAR", "LAST_YEAR", "CUSTOM".
     * When set and != CUSTOM, dateFrom/dateTo are computed server-side.
     */
    private String datePreset;

    // ── Dimensional filters ───────────────────────────────────────────────────
    private List<String> productIds;
    private List<String> categoryIds;
    private List<String> brandIds;
    private List<String> warehouseIds;
    private List<String> carrierIds;
    private List<String> customerIds;
    private List<String> orderStatuses;
    private List<String> channels;           // OrderSource values

    // ── Grouping / aggregation ────────────────────────────────────────────────
    /** "DAY", "WEEK", "MONTH", "QUARTER", "YEAR" */
    private String groupBy;

    /** Columns to include in a custom report. */
    private List<String> columns;

    // ── Custom report builder ─────────────────────────────────────────────────
    /** Additional dynamic filters for custom reports (field → value). */
    private Map<String, String> customFilters;

    // ── Paging (for data-table endpoints) ────────────────────────────────────
    private Integer page;
    private Integer size;
    private String  sortBy;
    private String  sortDir;

    // ── Export ────────────────────────────────────────────────────────────────
    @Builder.Default
    private ExportFormat exportFormat = ExportFormat.EXCEL;
}
