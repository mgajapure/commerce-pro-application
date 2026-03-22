package com.commerce_pro_backend.payment.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Sequence table for payment reference number generation.
 * Uses PESSIMISTIC_WRITE locking to guarantee uniqueness under concurrent load —
 * consistent with OrderSequence and ShipmentSequence patterns.
 */
@Entity
@Table(name = "payment_sequences")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentSequence {

    @Id
    @Column(name = "sequence_key", length = 50)
    private String sequenceKey;

    @Column(name = "last_value", nullable = false)
    @Builder.Default
    private Long lastValue = 0L;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
