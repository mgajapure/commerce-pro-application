package com.commerce_pro_backend.fulfillment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "pick_list_sequences", indexes = {
        @Index(name = "idx_pls_key", columnList = "sequence_key", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickListSequence {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "sequence_key", nullable = false, unique = true, length = 50)
    private String sequenceKey;

    @Column(name = "last_value", nullable = false)
    @Builder.Default
    private Long lastValue = 0L;
}
