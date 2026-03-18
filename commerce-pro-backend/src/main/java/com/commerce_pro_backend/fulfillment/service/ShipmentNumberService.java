package com.commerce_pro_backend.fulfillment.service;

import com.commerce_pro_backend.fulfillment.entity.ShipmentSequence;
import com.commerce_pro_backend.fulfillment.repository.ShipmentSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates unique shipment reference numbers.
 *
 * Format:  SHP-YYYYMMDD-NNNNNN
 * Example: SHP-20240315-000003
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentNumberService {

    private static final String SEQUENCE_KEY = "SHIPMENT_DAILY";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ShipmentSequenceRepository sequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        String today    = LocalDate.now().format(DATE_FORMAT);
        String dailyKey = SEQUENCE_KEY + "_" + today;

        ShipmentSequence sequence = sequenceRepository
                .findByKeyWithLock(dailyKey)
                .orElseGet(() -> sequenceRepository.save(
                        ShipmentSequence.builder()
                                .sequenceKey(dailyKey)
                                .lastValue(0L)
                                .build()
                ));

        long nextValue = sequence.getLastValue() + 1;
        sequence.setLastValue(nextValue);
        sequenceRepository.save(sequence);

        return String.format("SHP-%s-%06d", today, nextValue);
    }
}
