package com.commerce_pro_backend.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payout_sequences")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayoutSequence {

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
