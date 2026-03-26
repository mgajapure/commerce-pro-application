package com.commerce_pro_backend.supplier.evaluation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "supplier_evaluations",
    indexes = {
        @Index(name = "idx_eval_supplier", columnList = "supplier_id"),
        @Index(name = "idx_eval_status", columnList = "status"),
        @Index(name = "idx_eval_date", columnList = "evaluation_date")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SupplierEvaluation {

    @Id
    @Column(length = 36)
    private String id;

    @NotBlank
    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Size(max = 100)
    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @Column(name = "evaluation_date")
    private Instant evaluationDate;

    @Size(max = 100)
    @Column(name = "evaluated_by", length = 100)
    private String evaluatedBy;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "delivery_score")
    private Double deliveryScore;

    @Column(name = "pricing_score")
    private Double pricingScore;

    @Column(name = "communication_score")
    private Double communicationScore;

    @Column(name = "compliance_score")
    private Double complianceScore;

    @Size(max = 50)
    @Column(length = 50)
    private String period;

    @Size(max = 2000)
    @Column(length = 2000)
    private String comments;

    @Size(max = 2000)
    @Column(length = 2000)
    private String recommendations;

    @Size(max = 30)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplierEvaluation)) return false;
        SupplierEvaluation that = (SupplierEvaluation) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
