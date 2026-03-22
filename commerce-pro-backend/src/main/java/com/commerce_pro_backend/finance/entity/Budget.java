package com.commerce_pro_backend.finance.entity;

import com.commerce_pro_backend.finance.enums.BudgetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets", indexes = {
    @Index(name = "idx_budget_year",   columnList = "fiscal_year"),
    @Index(name = "idx_budget_period", columnList = "period_id"),
    @Index(name = "idx_budget_status", columnList = "status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Budget {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;
    @Column(name = "name", nullable = false, length = 200) private String name;
    @Column(name = "description", length = 1000)           private String description;
    @Column(name = "fiscal_year", nullable = false)        private Integer fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id") private FinancialPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default private BudgetStatus status = BudgetStatus.DRAFT;

    @Column(name = "total_revenue_budget", precision = 19, scale = 4) @Builder.Default private BigDecimal totalRevenueBudget = BigDecimal.ZERO;
    @Column(name = "total_expense_budget", precision = 19, scale = 4) @Builder.Default private BigDecimal totalExpenseBudget = BigDecimal.ZERO;
    @Column(name = "currency", length = 3) @Builder.Default private String currency = "USD";
    @Column(name = "approved_at")              private LocalDateTime approvedAt;
    @Column(name = "approved_by", length = 36) private String approvedBy;
    @Column(name = "notes", length = 2000)     private String notes;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")                       private LocalDateTime updatedAt;
    @Column(name = "created_by", updatable = false, length = 36)       private String createdBy;
    @Column(name = "updated_by", length = 36)                          private String updatedBy;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default private List<BudgetLine> lines = new ArrayList<>();
}
