package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.payment.dto.request.GenerateReconciliationRequest;
import com.commerce_pro_backend.payment.dto.response.ReconciliationReportDTO;
import com.commerce_pro_backend.payment.dto.response.ReconciliationSummaryDTO;
import com.commerce_pro_backend.payment.entity.PaymentTransaction;
import com.commerce_pro_backend.payment.entity.ReconciliationEntry;
import com.commerce_pro_backend.payment.entity.ReconciliationReport;
import com.commerce_pro_backend.payment.enums.ReconciliationStatus;
import com.commerce_pro_backend.payment.enums.TransactionStatus;
import com.commerce_pro_backend.payment.mapper.PaymentMapper;
import com.commerce_pro_backend.payment.repository.PaymentTransactionRepository;
import com.commerce_pro_backend.payment.repository.ReconciliationReportRepository;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReconciliationService {

    private final ReconciliationReportRepository reportRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentNumberService numberService;
    private final PaymentMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public PageResponse<ReconciliationSummaryDTO> list(Pageable pageable) {
        Page<ReconciliationReport> page = reportRepository.findAll(pageable);
        return PageResponse.from(page, mapper.toReconSummaryList(page.getContent()));
    }

    public ReconciliationReportDTO getById(String id) {
        return mapper.toReconDTO(findById(id));
    }

    @Transactional
    public ReconciliationReportDTO generate(GenerateReconciliationRequest req) {
        String actorId = currentUserService.getCurrentUserId();

        ReconciliationReport report = ReconciliationReport.builder()
                .reportRef(numberService.generateReconRef())
                .periodStart(req.getPeriodStart())
                .periodEnd(req.getPeriodEnd())
                .status(ReconciliationStatus.RUNNING)
                .generatedBy(actorId)
                .generatedAt(LocalDateTime.now())
                .notes(req.getNotes())
                .build();
        report = reportRepository.save(report);

        try {
            report = runReconciliation(report, actorId);
        } catch (Exception e) {
            log.error("Reconciliation failed for report {}", report.getReportRef(), e);
            report.setStatus(ReconciliationStatus.FAILED);
            reportRepository.save(report);
            throw ApiException.badRequest("Reconciliation failed: " + e.getMessage());
        }

        auditService.log(actorId, AuditAction.RECONCILIATION_GENERATED, "ReconciliationReport",
                report.getId(), report.getReportRef(), null, null, report.getStatus().name(), true);
        return mapper.toReconDTO(report);
    }

    @Transactional
    public ReconciliationReportDTO close(String id) {
        String actorId = currentUserService.getCurrentUserId();
        ReconciliationReport report = findById(id);
        report.setStatus(ReconciliationStatus.COMPLETED);
        report = reportRepository.save(report);
        auditService.log(actorId, AuditAction.RECONCILIATION_CLOSED, "ReconciliationReport",
                report.getId(), report.getReportRef(), null, null, report.getStatus().name(), true);
        return mapper.toReconDTO(report);
    }

    private ReconciliationReport runReconciliation(ReconciliationReport report, String actorId) {
        List<PaymentTransaction> transactions = transactionRepository
                .findSettledBetween(report.getPeriodStart(), report.getPeriodEnd());

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        long matched = 0, unmatched = 0;
        List<ReconciliationEntry> entries = new ArrayList<>();

        for (PaymentTransaction txn : transactions) {
            // Simulate gateway match (in production, compare against gateway settlement file)
            boolean isMatch = true; // ManualGateway always matches
            BigDecimal variance = BigDecimal.ZERO;
            String matchStatus = "MATCHED";

            if (txn.getStatus() == TransactionStatus.SETTLED) totalRevenue = totalRevenue.add(txn.getAmount());
            if (txn.getStatus() == TransactionStatus.REFUNDED) totalRefunds = totalRefunds.add(txn.getAmount());
            if (txn.getGatewayFee() != null) totalFees = totalFees.add(txn.getGatewayFee());

            if (isMatch) matched++; else { unmatched++; matchStatus = "UNMATCHED"; }

            ReconciliationEntry entry = ReconciliationEntry.builder()
                    .report(report)
                    .paymentTransactionId(txn.getId())
                    .transactionRef(txn.getTransactionRef())
                    .internalAmount(txn.getAmount())
                    .gatewayAmount(txn.getAmount()) // same in stub
                    .variance(variance)
                    .matchStatus(matchStatus)
                    .build();
            entries.add(entry);
        }

        BigDecimal netRevenue = totalRevenue.subtract(totalRefunds).subtract(totalFees);
        report.setTotalTransactions((long) transactions.size());
        report.setTotalRevenue(totalRevenue);
        report.setTotalRefunds(totalRefunds);
        report.setTotalFees(totalFees);
        report.setNetRevenue(netRevenue);
        report.setMatchedCount(matched);
        report.setUnmatchedCount(unmatched);
        report.setStatus(unmatched > 0 ? ReconciliationStatus.COMPLETED_WITH_VARIANCE : ReconciliationStatus.COMPLETED);
        report.getEntries().addAll(entries);
        return reportRepository.save(report);
    }

    private ReconciliationReport findById(String id) {
        return reportRepository.findById(id).orElseThrow(() -> ApiException.notFound("ReconciliationReport", id));
    }
}
