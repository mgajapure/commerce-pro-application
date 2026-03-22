package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.finance.entity.InvoiceSequence;
import com.commerce_pro_backend.finance.repository.InvoiceSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates unique reference numbers for all Finance module entities.
 *
 * Formats:
 *   INV-YYYYMMDD-NNNNNN   → Customer invoices
 *   VINV-YYYYMMDD-NNNNNN  → Vendor invoices
 *   EXP-YYYYMMDD-NNNNNN   → Expenses
 *   JRN-YYYYMMDD-NNNNNN   → Journal entries
 *   TRPT-YYYYMMDD-NNNNNN  → Tax reports
 *   VEN-NNNNNN            → Vendors (global sequence, not date-keyed)
 *
 * Uses the sequence-table + PESSIMISTIC_WRITE pattern established by
 * OrderNumberService, ShipmentNumberService, and PaymentNumberService.
 * REQUIRES_NEW transaction guarantees the sequence row commits immediately,
 * even if the caller transaction rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceNumberService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final InvoiceSequenceRepository sequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateInvoiceNumber() {
        return generateDaily("INV", "INV");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateVendorInvoiceNumber() {
        return generateDaily("VINV", "VINV");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateExpenseRef() {
        return generateDaily("EXP", "EXP");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateJournalEntryRef() {
        return generateDaily("JRN", "JRN");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateTaxReportRef() {
        return generateDaily("TRPT", "TRPT");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateVendorCode() {
        return generateGlobal("VEN", "VEN_GLOBAL");
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private String generateDaily(String prefix, String type) {
        String today   = LocalDate.now().format(DATE_FORMAT);
        String key     = type + "_DAILY_" + today;
        long   next    = incrementSequence(key);
        return String.format("%s-%s-%06d", prefix, today, next);
    }

    private String generateGlobal(String prefix, String key) {
        long next = incrementSequence(key);
        return String.format("%s-%06d", prefix, next);
    }

    private long incrementSequence(String key) {
        InvoiceSequence seq = sequenceRepository.findByKeyWithLock(key)
                .orElseGet(() -> sequenceRepository.save(
                        InvoiceSequence.builder().sequenceKey(key).lastValue(0L).build()));
        long next = seq.getLastValue() + 1;
        seq.setLastValue(next);
        sequenceRepository.save(seq);
        return next;
    }
}
