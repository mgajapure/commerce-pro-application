package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.ScheduledReportRequestDTO;
import com.commerce_pro_backend.analytics.dto.ScheduledReportResponseDTO;
import com.commerce_pro_backend.analytics.entity.SavedReport;
import com.commerce_pro_backend.analytics.entity.ScheduledReport;
import com.commerce_pro_backend.analytics.enums.ScheduleFrequency;
import com.commerce_pro_backend.analytics.repository.SavedReportRepository;
import com.commerce_pro_backend.analytics.repository.ScheduledReportRepository;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScheduledReportService {

    private final ScheduledReportRepository scheduledRepo;
    private final SavedReportRepository     savedReportRepo;
    private final CurrentUserService        currentUserService;

    @Transactional
    public ScheduledReportResponseDTO create(ScheduledReportRequestDTO req) {
        String userId = currentUserService.getCurrentUserId();
        SavedReport saved = savedReportRepo.findById(req.getSavedReportId())
                .orElseThrow(() -> ApiException.notFound("SavedReport", req.getSavedReportId()));

        LocalTime runAt = req.getRunAtTime() != null ? req.getRunAtTime() : LocalTime.of(6, 0);
        LocalDateTime nextRun = computeNextRun(req.getFrequency(), req.getDayOfPeriod(), runAt);

        ScheduledReport sr = ScheduledReport.builder()
                .savedReportId(req.getSavedReportId())
                .name(req.getName())
                .frequency(req.getFrequency())
                .dayOfPeriod(req.getDayOfPeriod())
                .runAtTime(runAt)
                .recipientEmails(req.getRecipientEmails())
                .format(req.getFormat())
                .emailSubject(req.getEmailSubject() != null ? req.getEmailSubject()
                        : "Scheduled Report: " + req.getName())
                .emailBody(req.getEmailBody())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .nextRunAt(nextRun)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        return toDTO(scheduledRepo.save(sr), saved.getName());
    }

    public PageResponse<ScheduledReportResponseDTO> list(Pageable pageable) {
        String userId = currentUserService.getCurrentUserId();
        Page<ScheduledReport> page = scheduledRepo.findByCreatedBy(userId, pageable);
        return PageResponse.from(page, page.getContent().stream().map(s -> {
            String savedName = savedReportRepo.findById(s.getSavedReportId())
                    .map(SavedReport::getName).orElse("Deleted");
            return toDTO(s, savedName);
        }).toList());
    }

    public ScheduledReportResponseDTO getById(String id) {
        ScheduledReport sr = findOrThrow(id);
        String savedName = savedReportRepo.findById(sr.getSavedReportId())
                .map(SavedReport::getName).orElse("Deleted");
        return toDTO(sr, savedName);
    }

    @Transactional
    public ScheduledReportResponseDTO update(String id, ScheduledReportRequestDTO req) {
        String userId = currentUserService.getCurrentUserId();
        ScheduledReport sr = findOrThrow(id);
        checkOwnership(sr);

        LocalTime runAt = req.getRunAtTime() != null ? req.getRunAtTime() : sr.getRunAtTime();
        sr.setName(req.getName());
        sr.setFrequency(req.getFrequency());
        sr.setDayOfPeriod(req.getDayOfPeriod());
        sr.setRunAtTime(runAt);
        sr.setRecipientEmails(req.getRecipientEmails());
        sr.setFormat(req.getFormat());
        sr.setEmailSubject(req.getEmailSubject());
        sr.setEmailBody(req.getEmailBody());
        if (req.getIsActive() != null) sr.setIsActive(req.getIsActive());
        sr.setNextRunAt(computeNextRun(req.getFrequency(), req.getDayOfPeriod(), runAt));
        sr.setUpdatedBy(userId);

        String savedName = savedReportRepo.findById(sr.getSavedReportId())
                .map(SavedReport::getName).orElse("Deleted");
        return toDTO(scheduledRepo.save(sr), savedName);
    }

    @Transactional
    public void delete(String id) {
        ScheduledReport sr = findOrThrow(id);
        checkOwnership(sr);
        scheduledRepo.delete(sr);
    }

    @Transactional
    public ScheduledReportResponseDTO toggleActive(String id) {
        ScheduledReport sr = findOrThrow(id);
        checkOwnership(sr);
        sr.setIsActive(!sr.getIsActive());
        if (sr.getIsActive()) {
            sr.setNextRunAt(computeNextRun(sr.getFrequency(), sr.getDayOfPeriod(), sr.getRunAtTime()));
        }
        String savedName = savedReportRepo.findById(sr.getSavedReportId())
                .map(SavedReport::getName).orElse("Deleted");
        return toDTO(scheduledRepo.save(sr), savedName);
    }

    // ── Internal: compute next run datetime ───────────────────────────────────

    public LocalDateTime computeNextRun(ScheduleFrequency freq, Integer dayOfPeriod, LocalTime runAt) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime time = runAt != null ? runAt : LocalTime.of(6, 0);
        return switch (freq) {
            case DAILY -> {
                LocalDateTime candidate = now.toLocalDate().atTime(time);
                yield candidate.isAfter(now) ? candidate : candidate.plusDays(1);
            }
            case WEEKLY -> {
                int dow = dayOfPeriod != null && dayOfPeriod >= 1 && dayOfPeriod <= 7 ? dayOfPeriod : 1;
                DayOfWeek target = DayOfWeek.of(dow);
                LocalDateTime candidate = now.toLocalDate()
                        .with(TemporalAdjusters.nextOrSame(target)).atTime(time);
                yield candidate.isAfter(now) ? candidate : candidate.plusWeeks(1);
            }
            case MONTHLY -> {
                int dom = dayOfPeriod != null ? Math.min(dayOfPeriod, 28) : 1;
                LocalDateTime candidate = now.toLocalDate().withDayOfMonth(dom).atTime(time);
                yield candidate.isAfter(now) ? candidate : candidate.plusMonths(1);
            }
            case QUARTERLY -> {
                // First day of next quarter
                int quarter = (now.getMonthValue() - 1) / 3;
                int firstMonthOfNextQuarter = (quarter + 1) * 3 + 1;
                LocalDateTime candidate = now.toLocalDate()
                        .withMonth(firstMonthOfNextQuarter).withDayOfMonth(1).atTime(time);
                if (candidate.isBefore(now)) candidate = candidate.plusMonths(3);
                yield candidate;
            }
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ScheduledReport findOrThrow(String id) {
        return scheduledRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("ScheduledReport", id));
    }

    private void checkOwnership(ScheduledReport sr) {
        if (!sr.getCreatedBy().equals(currentUserService.getCurrentUserId())) {
            throw ApiException.forbidden("You can only modify your own scheduled reports");
        }
    }

    private ScheduledReportResponseDTO toDTO(ScheduledReport sr, String savedName) {
        List<String> emails = sr.getRecipientEmails() != null
                ? Arrays.asList(sr.getRecipientEmails().split(","))
                : List.of();
        return ScheduledReportResponseDTO.builder()
                .id(sr.getId())
                .savedReportId(sr.getSavedReportId())
                .savedReportName(savedName)
                .name(sr.getName())
                .frequency(sr.getFrequency())
                .dayOfPeriod(sr.getDayOfPeriod())
                .runAtTime(sr.getRunAtTime())
                .recipientEmails(emails.stream().map(String::trim).toList())
                .format(sr.getFormat())
                .emailSubject(sr.getEmailSubject())
                .isActive(sr.getIsActive())
                .lastRunAt(sr.getLastRunAt())
                .nextRunAt(sr.getNextRunAt())
                .runCount(sr.getRunCount())
                .lastRunStatus(sr.getLastRunStatus())
                .createdAt(sr.getCreatedAt())
                .createdBy(sr.getCreatedBy())
                .build();
    }
}
