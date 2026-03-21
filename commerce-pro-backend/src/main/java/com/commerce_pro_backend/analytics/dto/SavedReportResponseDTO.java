package com.commerce_pro_backend.analytics.dto;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ReportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SavedReportResponseDTO {
    private String id;
    private String name;
    private String description;
    private ReportType reportType;
    private ReportFilterDTO filterParams;
    private ExportFormat defaultFormat;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
