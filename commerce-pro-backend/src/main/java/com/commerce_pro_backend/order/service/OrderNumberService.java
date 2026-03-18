package com.commerce_pro_backend.order.service;

import com.commerce_pro_backend.order.entity.OrderSequence;
import com.commerce_pro_backend.order.repository.OrderSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates unique, human-readable order reference numbers.
 *
 * Format:  ORD-YYYYMMDD-NNNNNN
 * Example: ORD-20240315-000042
 *
 * Uses a sequence table with PESSIMISTIC_WRITE locking — the same pattern
 * established in the shipment and payment modules — to guarantee uniqueness
 * under high concurrency without relying on DB auto-increment IDs.
 *
 * Runs in a REQUIRES_NEW transaction so the sequence row is committed
 * immediately, even if the caller transaction rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNumberService {

    private static final String SEQUENCE_KEY = "ORDER_DAILY";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderSequenceRepository sequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        String today = LocalDate.now().format(DATE_FORMAT);
        String dailyKey = SEQUENCE_KEY + "_" + today;

        OrderSequence sequence = sequenceRepository
                .findByKeyWithLock(dailyKey)
                .orElseGet(() -> sequenceRepository.save(
                        OrderSequence.builder()
                                .sequenceKey(dailyKey)
                                .lastValue(0L)
                                .build()
                ));

        long nextValue = sequence.getLastValue() + 1;
        sequence.setLastValue(nextValue);
        sequenceRepository.save(sequence);

        return String.format("ORD-%s-%06d", today, nextValue);
    }
}
