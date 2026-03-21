package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * Resolves the dateFrom / dateTo from either explicit values or a preset string.
 * All methods return a two-element array [from, to] as LocalDateTime (start-of-day / end-of-day).
 */
@UtilityClass
public class DateRangeHelper {

    public LocalDateTime[] resolve(ReportFilterDTO filter) {
        LocalDate from;
        LocalDate to;

        if (filter.getDatePreset() != null && !filter.getDatePreset().equalsIgnoreCase("CUSTOM")) {
            LocalDate today = LocalDate.now();
            switch (filter.getDatePreset().toUpperCase()) {
                case "TODAY" -> {
                    from = today;
                    to   = today;
                }
                case "YESTERDAY" -> {
                    from = today.minusDays(1);
                    to   = today.minusDays(1);
                }
                case "LAST_7_DAYS" -> {
                    from = today.minusDays(6);
                    to   = today;
                }
                case "LAST_30_DAYS" -> {
                    from = today.minusDays(29);
                    to   = today;
                }
                case "THIS_MONTH" -> {
                    from = today.withDayOfMonth(1);
                    to   = today.with(TemporalAdjusters.lastDayOfMonth());
                }
                case "LAST_MONTH" -> {
                    LocalDate firstOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                    from = firstOfLastMonth;
                    to   = firstOfLastMonth.with(TemporalAdjusters.lastDayOfMonth());
                }
                case "THIS_QUARTER" -> {
                    from = today.with(IsoFields.DAY_OF_QUARTER, 1);
                    to   = today;
                }
                case "THIS_YEAR" -> {
                    from = today.withDayOfYear(1);
                    to   = today;
                }
                case "LAST_YEAR" -> {
                    LocalDate firstOfLastYear = today.minusYears(1).withDayOfYear(1);
                    from = firstOfLastYear;
                    to   = firstOfLastYear.with(TemporalAdjusters.lastDayOfYear());
                }
                default -> {
                    from = today.minusDays(29);
                    to   = today;
                }
            }
        } else {
            from = filter.getDateFrom() != null ? filter.getDateFrom() : LocalDate.now().minusDays(29);
            to   = filter.getDateTo()   != null ? filter.getDateTo()   : LocalDate.now();
        }

        return new LocalDateTime[]{
                from.atStartOfDay(),
                to.atTime(23, 59, 59)
        };
    }

    /** Compute the equivalent previous period of the same length. */
    public LocalDateTime[] previousPeriod(LocalDateTime[] current) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(current[0].toLocalDate(), current[1].toLocalDate()) + 1;
        return new LocalDateTime[]{
                current[0].minusDays(days),
                current[1].minusDays(days)
        };
    }
}
