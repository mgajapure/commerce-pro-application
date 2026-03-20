package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.entity.ScheduledReport;
import com.commerce_pro_backend.analytics.repository.ScheduledReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs every 5 minutes and fires any scheduled reports whose nextRunAt is in the past.
 *
 * Enable the scheduler by adding @EnableScheduling to your main application class
 * or a @Configuration class.  It is left opt-in to avoid unexpected executions
 * in fresh dev environments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportSchedulerJob {

    private final ScheduledReportRepository scheduledRepo;
    private final ReportRunnerService       runnerService;

    /**
     * Every 5 minutes: find active scheduled reports whose nextRunAt has passed
     * and execute them one by one.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)   // 5 minutes
    public void processDueReports() {
        List<ScheduledReport> due = scheduledRepo
                .findByIsActiveTrueAndNextRunAtBefore(LocalDateTime.now());

        if (due.isEmpty()) return;

        log.info("Processing {} due scheduled report(s)", due.size());
        for (ScheduledReport sr : due) {
            try {
                log.info("Running scheduled report: {} ({})", sr.getName(), sr.getId());
                runnerService.runScheduled(sr.getId());
            } catch (Exception ex) {
                log.error("Scheduled report {} failed", sr.getId(), ex);
                // nextRunAt is still advanced inside runScheduled even on failure,
                // preventing a tight retry loop.
            }
        }
    }
}
