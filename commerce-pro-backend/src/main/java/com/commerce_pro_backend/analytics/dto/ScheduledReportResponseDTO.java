package com.commerce_pro_backend.analytics.dto;

import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ScheduleFrequency;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class ScheduledReportResponseDTO {
    private String id;
    private String savedReportId;
    private String savedReportName;
    private String name;
    private ScheduleFrequency frequency;
    private Integer dayOfPeriod;
    private LocalTime runAtTime;
    private List<String> recipientEmails;
    private ExportFormat format;
    private String emailSubject;
    private Boolean isActive;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private Integer runCount;
    private String lastRunStatus;
    private LocalDateTime createdAt;
    private String createdBy;
}
