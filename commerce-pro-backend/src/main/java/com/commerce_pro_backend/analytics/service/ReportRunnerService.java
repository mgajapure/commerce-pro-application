package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.*;
import com.commerce_pro_backend.analytics.entity.ReportExecution;
import com.commerce_pro_backend.analytics.entity.SavedReport;
import com.commerce_pro_backend.analytics.entity.ScheduledReport;
import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ReportStatus;
import com.commerce_pro_backend.analytics.enums.ReportType;
import com.commerce_pro_backend.analytics.repository.ReportExecutionRepository;
import com.commerce_pro_backend.analytics.repository.SavedReportRepository;
import com.commerce_pro_backend.analytics.repository.ScheduledReportRepository;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Orchestrates the full report-run lifecycle:
 *  1. Create a PENDING ReportExecution record
 *  2. Call the appropriate report service
 *  3. Call ReportExportService to write the file
 *  4. Update the execution record to COMPLETED / FAILED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportRunnerService {

    private final SalesReportService      salesService;
    private final InventoryReportService  inventoryService;
    private final OrderReportService      orderService;
    private final CustomerReportService   customerService;
    private final FinancialReportService  financialService;
    private final ShippingReportService   shippingService;
    private final ReturnReportService     returnService;
    private final ReportExportService     exportService;

    private final ReportExecutionRepository executionRepo;
    private final SavedReportRepository     savedReportRepo;
    private final ScheduledReportRepository scheduledRepo;
    private final CurrentUserService        currentUserService;
    private final ScheduledReportService    scheduledReportService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ── Ad-hoc run ────────────────────────────────────────────────────────────

    @Transactional
    public ReportExecutionDTO runAdHoc(ReportFilterDTO filter) {
        String userId = currentUserService.getCurrentUserId();
        ReportExecution exec = createExecution(filter.getReportType(), filter.getExportFormat(),
                toJson(filter), userId, null, null);
        return executeAndPersist(exec, filter);
    }

    /** Run a previously saved report definition on demand. */
    @Transactional
    public ReportExecutionDTO runSaved(String savedReportId) {
        String userId = currentUserService.getCurrentUserId();
        SavedReport saved = savedReportRepo.findById(savedReportId)
                .orElseThrow(() -> ApiException.notFound("SavedReport", savedReportId));
        ReportFilterDTO filter = fromJson(saved.getFilterParams());
        if (filter == null) throw ApiException.badRequest("Saved report has no filter configuration");
        filter.setExportFormat(saved.getDefaultFormat());
        ReportExecution exec = createExecution(saved.getReportType(), saved.getDefaultFormat(),
                saved.getFilterParams(), userId, savedReportId, null);
        return executeAndPersist(exec, filter);
    }

    /** Called by the scheduler for scheduled deliveries. */
    @Transactional
    public ReportExecutionDTO runScheduled(String scheduledReportId) {
        ScheduledReport sr = scheduledRepo.findById(scheduledReportId)
                .orElseThrow(() -> ApiException.notFound("ScheduledReport", scheduledReportId));
        SavedReport saved = savedReportRepo.findById(sr.getSavedReportId())
                .orElseThrow(() -> ApiException.notFound("SavedReport", sr.getSavedReportId()));

        ReportFilterDTO filter = fromJson(saved.getFilterParams());
        if (filter == null) throw ApiException.badRequest("Saved report has no filter configuration");
        filter.setExportFormat(sr.getFormat());

        ReportExecution exec = createExecution(saved.getReportType(), sr.getFormat(),
                saved.getFilterParams(), sr.getCreatedBy(), saved.getId(), scheduledReportId);
        ReportExecutionDTO result = executeAndPersist(exec, filter);

        // Update schedule
        sr.setLastRunAt(LocalDateTime.now());
        sr.setRunCount(sr.getRunCount() + 1);
        sr.setLastRunStatus(result.getStatus().name());
        sr.setNextRunAt(scheduledReportService.computeNextRun(sr.getFrequency(), sr.getDayOfPeriod(), sr.getRunAtTime()));
        scheduledRepo.save(sr);

        return result;
    }

    // ── History ───────────────────────────────────────────────────────────────

    public ReportExecutionDTO getExecution(String id) {
        ReportExecution exec = executionRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("ReportExecution", id));
        return toDTO(exec);
    }

    // ── Core execution logic ──────────────────────────────────────────────────

    private ReportExecutionDTO executeAndPersist(ReportExecution exec, ReportFilterDTO filter) {
        exec.setStatus(ReportStatus.RUNNING);
        exec.setStartedAt(LocalDateTime.now());
        executionRepo.save(exec);

        long start = System.currentTimeMillis();
        try {
            ReportExportService.ExportResult result = dispatch(filter, exec.getFormat());
            exec.setStatus(ReportStatus.COMPLETED);
            exec.setFilePath(result.relativePath());
            exec.setFileName(result.fileName());
            exec.setFileSizeBytes(result.fileSizeBytes());
            exec.setRowCount(result.rowCount());
        } catch (Exception e) {
            log.error("Report execution {} failed", exec.getId(), e);
            exec.setStatus(ReportStatus.FAILED);
            exec.setErrorMessage(e.getMessage());
        } finally {
            exec.setCompletedAt(LocalDateTime.now());
            exec.setDurationMs(System.currentTimeMillis() - start);
            executionRepo.save(exec);
        }

        return toDTO(exec);
    }

    private ReportExportService.ExportResult dispatch(ReportFilterDTO filter, ExportFormat format) {
        ReportType type = filter.getReportType();
        return switch (type) {
            case SALES_OVERVIEW, SALES_BY_DATE ->
                    exportService.exportSales(salesService.getByDate(filter), format, type);
            case SALES_BY_PRODUCT ->
                    exportService.exportSales(salesService.getByProduct(filter), format, type);
            case SALES_BY_CATEGORY ->
                    exportService.exportSales(salesService.getByCategory(filter), format, type);
            case SALES_BY_CHANNEL ->
                    exportService.exportSales(salesService.getByChannel(filter), format, type);
            case INVENTORY_STOCK_VALUE ->
                    exportService.exportInventory(inventoryService.getStockValue(filter), format, type);
            case INVENTORY_MOVEMENT ->
                    exportService.exportInventoryMovement(inventoryService.getMovement(filter), format, type);
            case INVENTORY_AGEING ->
                    exportService.exportInventoryAgeing(inventoryService.getAgeing(filter), format, type);
            case ORDER_SUMMARY, ORDER_FULFILLMENT_RATE, ORDER_CANCELLATION_RATE ->
                    exportService.exportOrders(orderService.getReport(filter), format, type);
            case CUSTOMER_LTV, CUSTOMER_COHORT, CUSTOMER_ACQUISITION ->
                    exportService.exportCustomers(customerService.getLtvReport(filter), format, type);
            case CUSTOMER_CHURN ->
                    exportService.exportCustomers(customerService.getChurnReport(filter), format, type);
            case FINANCIAL_REVENUE, FINANCIAL_MARGIN, FINANCIAL_TAX_SUMMARY ->
                    exportService.exportFinancial(financialService.getReport(filter), format, type);
            case SHIPPING_ON_TIME_DELIVERY, SHIPPING_CARRIER_PERFORMANCE ->
                    exportService.exportShipping(shippingService.getReport(filter), format, type);
            case RETURN_RATE, RETURN_REASON_ANALYSIS ->
                    exportService.exportReturns(returnService.getReport(filter), format, type);
            case CUSTOM ->
                    exportService.exportSales(salesService.getByDate(filter), format, type);
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReportExecution createExecution(ReportType type, ExportFormat format,
                                             String filterJson, String userId,
                                             String savedId, String scheduledId) {
        ReportExecution exec = ReportExecution.builder()
                .reportType(type)
                .format(format != null ? format : ExportFormat.EXCEL)
                .status(ReportStatus.PENDING)
                .filterParams(filterJson)
                .savedReportId(savedId)
                .scheduledReportId(scheduledId)
                .requestedBy(userId)
                .build();
        return executionRepo.save(exec);
    }

    private ReportExecutionDTO toDTO(ReportExecution e) {
        String downloadUrl = e.getFilePath() != null
                ? "/api/files/download/" + e.getFilePath()
                : null;
        return ReportExecutionDTO.builder()
                .id(e.getId())
                .reportType(e.getReportType())
                .status(e.getStatus())
                .format(e.getFormat())
                .fileName(e.getFileName())
                .fileSizeBytes(e.getFileSizeBytes())
                .rowCount(e.getRowCount())
                .errorMessage(e.getErrorMessage())
                .startedAt(e.getStartedAt())
                .completedAt(e.getCompletedAt())
                .durationMs(e.getDurationMs())
                .downloadUrl(downloadUrl)
                .savedReportId(e.getSavedReportId())
                .scheduledReportId(e.getScheduledReportId())
                .requestedBy(e.getRequestedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return null; }
    }

    private ReportFilterDTO fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readValue(json, ReportFilterDTO.class); }
        catch (Exception e) { return null; }
    }
}
