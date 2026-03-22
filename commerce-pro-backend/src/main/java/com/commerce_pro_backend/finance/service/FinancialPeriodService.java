package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.FinancialPeriod;
import com.commerce_pro_backend.finance.enums.PeriodStatus;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.FinancialPeriodRepository;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialPeriodService {

    private final FinancialPeriodRepository periodRepository;
    private final FinanceMapper             mapper;
    private final AuditService              auditService;
    private final CurrentUserService        currentUserService;

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<FinancialPeriodDTO> getAllPeriods() {
        return mapper.toPeriodDTOList(periodRepository.findAll());
    }

    public PageResponse<FinancialPeriodDTO> getPeriodsPage(Pageable pageable) {
        Page<FinancialPeriod> page = periodRepository.findAll(pageable);
        return PageResponse.from(page, page.getContent().stream().map(mapper::toPeriodDTO).toList());
    }

    public FinancialPeriodDTO getById(String id) {
        return mapper.toPeriodDTO(findOrThrow(id));
    }

    public FinancialPeriodDTO getCurrentPeriod() {
        return mapper.toPeriodDTO(
            periodRepository.findCurrentOpenPeriod()
                .orElseThrow(() -> ApiException.notFound("No open financial period found", "current"))
        );
    }

    public List<FinancialPeriodDTO> getByFiscalYear(Integer year) {
        return mapper.toPeriodDTOList(periodRepository.findByFiscalYearOrderByPeriodNumberAsc(year));
    }

    /**
     * Resolves the best period for a given date — used by services that need
     * to assign a period when one isn't explicitly provided.
     */
    public FinancialPeriod resolvePeriodForDate(LocalDate date) {
        return periodRepository.findPeriodForDate(date).orElse(null);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public FinancialPeriodDTO create(CreatePeriodRequest req) {
        // Prevent duplicate
        if (periodRepository.findByFiscalYearAndPeriodNumber(req.getFiscalYear(), req.getPeriodNumber()).isPresent()) {
            throw ApiException.conflict("Period " + req.getFiscalYear() + "/" + req.getPeriodNumber() + " already exists");
        }
        String actor = currentUserService.getCurrentUserId();
        FinancialPeriod period = FinancialPeriod.builder()
                .fiscalYear(req.getFiscalYear()).periodNumber(req.getPeriodNumber())
                .periodName(req.getPeriodName()).periodType(req.getPeriodType() != null ? req.getPeriodType() : "MONTHLY")
                .startDate(req.getStartDate()).endDate(req.getEndDate())
                .status(PeriodStatus.OPEN).notes(req.getNotes())
                .createdBy(actor).build();
        period = periodRepository.save(period);
        auditService.log(actor, AuditAction.FINANCE_PERIOD_CREATED, "FinancialPeriod", period.getId(),
                period.getPeriodName(), null, null, null, true);
        return mapper.toPeriodDTO(period);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Transactional
    public FinancialPeriodDTO closePeriod(String id, String notes) {
        FinancialPeriod period = findOrThrow(id);
        if (period.getStatus() != PeriodStatus.OPEN && period.getStatus() != PeriodStatus.CLOSING) {
            throw ApiException.badRequest("Period '" + period.getPeriodName() + "' cannot be closed — status: " + period.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        period.setStatus(PeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(actor);
        if (notes != null) period.setNotes(notes);
        period = periodRepository.save(period);
        auditService.log(actor, AuditAction.FINANCE_PERIOD_CLOSED, "FinancialPeriod", period.getId(),
                period.getPeriodName(), null, "OPEN", "CLOSED", true);
        log.info("Financial period '{}' closed by {}", period.getPeriodName(), actor);
        return mapper.toPeriodDTO(period);
    }

    @Transactional
    public FinancialPeriodDTO lockPeriod(String id, String notes) {
        FinancialPeriod period = findOrThrow(id);
        if (period.getStatus() == PeriodStatus.LOCKED) {
            throw ApiException.badRequest("Period is already locked");
        }
        if (period.getStatus() == PeriodStatus.OPEN) {
            throw ApiException.badRequest("Period must be CLOSED before locking");
        }
        String actor = currentUserService.getCurrentUserId();
        period.setStatus(PeriodStatus.LOCKED);
        period.setLockedAt(LocalDateTime.now());
        period.setLockedBy(actor);
        if (notes != null) period.setNotes(notes);
        period = periodRepository.save(period);
        auditService.log(actor, AuditAction.FINANCE_PERIOD_LOCKED, "FinancialPeriod", period.getId(),
                period.getPeriodName(), null, "CLOSED", "LOCKED", true);
        log.warn("Financial period '{}' LOCKED by {}", period.getPeriodName(), actor);
        return mapper.toPeriodDTO(period);
    }

    @Transactional
    public FinancialPeriodDTO reopenPeriod(String id, String notes) {
        FinancialPeriod period = findOrThrow(id);
        if (!period.isReopenable()) {
            throw ApiException.badRequest("Period '" + period.getPeriodName() + "' cannot be reopened — status: " + period.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        String prev = period.getStatus().name();
        period.setStatus(PeriodStatus.OPEN);
        period.setClosedAt(null);
        period.setClosedBy(null);
        if (notes != null) period.setNotes(notes);
        period = periodRepository.save(period);
        auditService.log(actor, AuditAction.FINANCE_PERIOD_REOPENED, "FinancialPeriod", period.getId(),
                period.getPeriodName(), null, prev, "OPEN", true);
        return mapper.toPeriodDTO(period);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    public FinancialPeriod findOrThrow(String id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("FinancialPeriod", id));
    }
}
