package com.commerce_pro_backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "budget_lines", indexes = {
    @Index(name = "idx_bl_budget",   columnList = "budget_id"),
    @Index(name = "idx_bl_category", columnList = "category_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BudgetLine {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false) private Budget budget;

    @Column(name = "line_name", nullable = false, length = 200) private String lineName;
    @Column(name = "category_id", length = 36)                  private String categoryId;
    @Column(name = "account_code", length = 20)                 private String accountCode;
    @Column(name = "line_type", length = 20) @Builder.Default   private String lineType = "EXPENSE";

    @Column(name = "budgeted_amount", nullable = false, precision = 19, scale = 4) private BigDecimal budgetedAmount;
    @Column(name = "actual_amount", nullable = false, precision = 19, scale = 4)   @Builder.Default private BigDecimal actualAmount = BigDecimal.ZERO;
    @Column(name = "variance", nullable = false, precision = 19, scale = 4)        @Builder.Default private BigDecimal variance = BigDecimal.ZERO;
    @Column(name = "variance_pct", precision = 7, scale = 4)                       @Builder.Default private BigDecimal variancePct = BigDecimal.ZERO;
    @Column(name = "notes", length = 500) private String notes;
    @Column(name = "sort_order") @Builder.Default private Integer sortOrder = 0;

    @PreUpdate
    public void computeVariance() {
        this.variance = this.actualAmount.subtract(this.budgetedAmount);
        if (this.budgetedAmount.compareTo(BigDecimal.ZERO) != 0) {
            this.variancePct = this.variance.divide(this.budgetedAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }
}
