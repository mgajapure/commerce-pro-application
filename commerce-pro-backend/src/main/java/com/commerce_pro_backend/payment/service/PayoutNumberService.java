package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.payment.entity.PayoutSequence;
import com.commerce_pro_backend.payment.repository.PayoutSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutNumberService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final PayoutSequenceRepository sequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generatePayoutRef() {
        String today = LocalDate.now().format(DATE_FORMAT);
        String key = "PAYOUT_DAILY_" + today;

        PayoutSequence seq = sequenceRepository.findByKeyWithLock(key)
                .orElseGet(() -> sequenceRepository.save(
                        PayoutSequence.builder().sequenceKey(key).lastValue(0L).build()));

        long next = seq.getLastValue() + 1;
        seq.setLastValue(next);
        sequenceRepository.save(seq);

        return String.format("POT-%s-%06d", today, next);
    }
}
