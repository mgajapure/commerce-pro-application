package com.commerce_pro_backend.order.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Sequence table for order reference number generation.
 * Uses PESSIMISTIC_WRITE locking to guarantee uniqueness under concurrent load —
 * consistent with the shipment and payment module patterns.
 */
@Entity
@Table(name = "order_sequences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSequence {

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
