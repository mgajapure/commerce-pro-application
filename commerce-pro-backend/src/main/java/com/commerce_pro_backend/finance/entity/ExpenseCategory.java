package com.commerce_pro_backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_categories", indexes = {
    @Index(name = "idx_ec_code", columnList = "category_code", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExpenseCategory {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;
    @Column(name = "category_code", unique = true, nullable = false, length = 20) private String categoryCode;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "description", length = 500)            private String description;
    @Column(name = "parent_id", length = 36)               private String parentId;
    @Column(name = "gl_account_code", length = 20)         private String glAccountCode;
    @Column(name = "is_active") @Builder.Default           private Boolean isActive = true;
    @Column(name = "budget_default", precision = 19, scale = 4) private BigDecimal budgetDefault;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "created_by", updatable = false, length = 36)       private String createdBy;
}
