package com.commerce_pro_backend.supplier.procurement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "procurement_requests",
    indexes = {
        @Index(name = "idx_pr_number", columnList = "request_number", unique = true),
        @Index(name = "idx_pr_status", columnList = "status"),
        @Index(name = "idx_pr_requested_by", columnList = "requested_by"),
        @Index(name = "idx_pr_supplier", columnList = "supplier_id")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProcurementRequest {

    @Id
    @Column(length = 36)
    private String id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "request_number", nullable = false, length = 50, unique = true)
    private String requestNumber;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Size(max = 30)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private String priority = "MEDIUM";

    @Size(max = 30)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Size(max = 100)
    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "supplier_id", length = 36)
    private String supplierId;

    @Size(max = 100)
    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 15, scale = 2)
    private BigDecimal actualCost;

    @Size(max = 10)
    @Column(length = 10, nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "required_by_date")
    private Instant requiredByDate;

    @Size(max = 2000)
    @Column(length = 2000)
    private String notes;

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
        if (!(o instanceof ProcurementRequest)) return false;
        ProcurementRequest that = (ProcurementRequest) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
