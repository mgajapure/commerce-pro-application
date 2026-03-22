package com.commerce_pro_backend.finance.entity;

import com.commerce_pro_backend.finance.enums.JournalEntryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Double-entry bookkeeping journal entry.
 * Every financial event auto-creates a balanced debit/credit pair.
 * Manual entries allowed only with matching debit/credit totals.
 */
@Entity
@Table(name = "journal_entries", indexes = {
    @Index(name = "idx_je_ref",    columnList = "entry_ref",  unique = true),
    @Index(name = "idx_je_type",   columnList = "entry_type"),
    @Index(name = "idx_je_period", columnList = "period_id"),
    @Index(name = "idx_je_status", columnList = "status"),
    @Index(name = "idx_je_date",   columnList = "entry_date"),
    @Index(name = "idx_je_source", columnList = "source_type, source_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntry {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;
    @Column(name = "entry_ref", unique = true, nullable = false, length = 30) private String entryRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id") private FinancialPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 30)
    private JournalEntryType entryType;

    @Column(name = "status", length = 20) @Builder.Default private String status = "POSTED";
    @Column(name = "entry_date", nullable = false) private LocalDate entryDate;
    @Column(name = "description", nullable = false, length = 500) private String description;
    @Column(name = "reference", length = 100)  private String reference;
    @Column(name = "currency", length = 3)     @Builder.Default private String currency = "USD";
    @Column(name = "source_type", length = 50) private String sourceType;
    @Column(name = "source_id", length = 36)   private String sourceId;
    @Column(name = "posted_by", length = 36)   private String postedBy;
    @Column(name = "reversed_at")              private LocalDateTime reversedAt;
    @Column(name = "reversed_by", length = 36) private String reversedBy;
    @Column(name = "reversal_reason", length = 500) private String reversalReason;
    @Column(name = "notes", length = 2000)     private String notes;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")                       private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default private List<JournalEntryLine> lines = new ArrayList<>();

    public boolean isBalanced() {
        BigDecimal debits  = lines.stream().filter(l -> l.getAccountType() == com.commerce_pro_backend.finance.enums.AccountType.DEBIT)
                .map(JournalEntryLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = lines.stream().filter(l -> l.getAccountType() == com.commerce_pro_backend.finance.enums.AccountType.CREDIT)
                .map(JournalEntryLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return debits.compareTo(credits) == 0;
    }
}
