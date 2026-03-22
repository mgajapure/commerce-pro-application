package com.commerce_pro_backend.finance.entity;

import com.commerce_pro_backend.finance.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;

@Entity
@Table(name = "journal_entry_lines", indexes = {
    @Index(name = "idx_jel_entry", columnList = "journal_entry_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryLine {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private AccountType accountType;

    @Column(name = "account_code", nullable = false, length = 20)  private String accountCode;
    @Column(name = "account_name", nullable = false, length = 100) private String accountName;
    @Column(name = "amount", nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(name = "currency", length = 3) @Builder.Default private String currency = "USD";
    @Column(name = "description", length = 300) private String description;
    @Column(name = "sort_order") @Builder.Default private Integer sortOrder = 0;
}
