package com.commerce_pro_backend.fulfillment.service;

import com.commerce_pro_backend.fulfillment.entity.PickListSequence;
import com.commerce_pro_backend.fulfillment.repository.PickListSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates unique pick-list reference numbers.
 *
 * Format:  PKL-YYYYMMDD-NNNNNN
 * Example: PKL-20240315-000007
 *
 * Uses the same sequence-table + PESSIMISTIC_WRITE pattern as OrderNumberService.
 * Runs in REQUIRES_NEW so the sequence row is committed even if caller rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickListNumberService {

    private static final String SEQUENCE_KEY = "PICKLIST_DAILY";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PickListSequenceRepository sequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        String today    = LocalDate.now().format(DATE_FORMAT);
        String dailyKey = SEQUENCE_KEY + "_" + today;

        PickListSequence sequence = sequenceRepository
                .findByKeyWithLock(dailyKey)
                .orElseGet(() -> sequenceRepository.save(
                        PickListSequence.builder()
                                .sequenceKey(dailyKey)
                                .lastValue(0L)
                                .build()
                ));

        long nextValue = sequence.getLastValue() + 1;
        sequence.setLastValue(nextValue);
        sequenceRepository.save(sequence);

        return String.format("PKL-%s-%06d", today, nextValue);
    }
}
