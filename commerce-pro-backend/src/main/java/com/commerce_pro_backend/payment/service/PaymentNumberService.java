package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.payment.entity.PaymentSequence;
import com.commerce_pro_backend.payment.repository.PaymentSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates unique reference numbers for payment entities.
 *
 * Formats:
 *   PAY-YYYYMMDD-NNNNNN  → payment transactions
 *   REF-YYYYMMDD-NNNNNN  → refund requests
 *   DSP-YYYYMMDD-NNNNNN  → chargeback disputes
 *   RCN-YYYYMMDD-NNNNNN  → reconciliation reports
 *
 * Uses the sequence-table + PESSIMISTIC_WRITE pattern established by
 * OrderNumberService and ShipmentNumberService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNumberService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final PaymentSequenceRepository sequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generatePaymentRef() {
        return generate("PAY", "PAYMENT");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateRefundRef() {
        return generate("REF", "REFUND");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateDisputeRef() {
        return generate("DSP", "DISPUTE");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateReconRef() {
        return generate("RCN", "RECON");
    }

    private String generate(String prefix, String type) {
        String today = LocalDate.now().format(DATE_FORMAT);
        String key = type + "_DAILY_" + today;

        PaymentSequence seq = sequenceRepository.findByKeyWithLock(key)
                .orElseGet(() -> sequenceRepository.save(
                        PaymentSequence.builder().sequenceKey(key).lastValue(0L).build()));

        long next = seq.getLastValue() + 1;
        seq.setLastValue(next);
        sequenceRepository.save(seq);

        return String.format("%s-%s-%06d", prefix, today, next);
    }
}
