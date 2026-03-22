package com.commerce_pro_backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Shared sequence table for all finance reference numbers.
 * Uses PESSIMISTIC_WRITE locking — consistent with OrderSequence, ShipmentSequence patterns.
 *
 * Sequence keys used:
 *   INV_DAILY_YYYYMMDD   → INV-YYYYMMDD-NNNNNN (customer invoices)
 *   VINV_DAILY_YYYYMMDD  → VINV-YYYYMMDD-NNNNNN (vendor invoices)
 *   EXP_DAILY_YYYYMMDD   → EXP-YYYYMMDD-NNNNNN (expenses)
 *   JRN_DAILY_YYYYMMDD   → JRN-YYYYMMDD-NNNNNN (journal entries)
 *   TRPT_DAILY_YYYYMMDD  → TRPT-YYYYMMDD-NNNNNN (tax reports)
 *   VEN_GLOBAL           → VEN-NNNNNN (vendors — not date-keyed)
 */
@Entity
@Table(name = "invoice_sequences")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InvoiceSequence {

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
