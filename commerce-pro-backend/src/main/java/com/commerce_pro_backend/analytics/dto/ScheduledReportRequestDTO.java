package com.commerce_pro_backend.analytics.dto;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ScheduleFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ScheduledReportRequestDTO {

    @NotBlank
    private String savedReportId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private ScheduleFrequency frequency;

    /** Day of week (1-7) for WEEKLY; day of month (1-28) for MONTHLY. */
    private Integer dayOfPeriod;

    private LocalTime runAtTime;

    /** Comma-separated recipient e-mail addresses. */
    @NotBlank
    private String recipientEmails;

    private ExportFormat format = ExportFormat.EXCEL;

    @Size(max = 300)
    private String emailSubject;

    @Size(max = 2000)
    private String emailBody;

    private Boolean isActive = true;
}
