package com.commerce_pro_backend.analytics.dto;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ReportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SavedReportRequestDTO {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    private ReportType reportType;

    @Valid
    private ReportFilterDTO filterParams;

    private ExportFormat defaultFormat = ExportFormat.EXCEL;

    private Boolean isPublic = false;
}
