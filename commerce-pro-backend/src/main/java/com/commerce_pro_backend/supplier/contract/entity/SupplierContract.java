package com.commerce_pro_backend.supplier.contract.entity;

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
    name = "supplier_contracts",
    indexes = {
        @Index(name = "idx_contract_number", columnList = "contract_number", unique = true),
        @Index(name = "idx_contract_supplier", columnList = "supplier_id"),
        @Index(name = "idx_contract_status", columnList = "status"),
        @Index(name = "idx_contract_end_date", columnList = "end_date")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SupplierContract {

    @Id
    @Column(length = 36)
    private String id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "contract_number", nullable = false, length = 50, unique = true)
    private String contractNumber;

    @NotBlank
    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Size(max = 100)
    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Size(max = 50)
    @Column(name = "contract_type", length = 50)
    private String contractType;

    @Size(max = 30)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Size(max = 10)
    @Column(length = 10, nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Size(max = 100)
    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = false;

    @Column(name = "renewal_notice_days")
    private Integer renewalNoticeDays;

    @Size(max = 500)
    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Size(max = 5000)
    @Column(length = 5000)
    private String terms;

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
        if (!(o instanceof SupplierContract)) return false;
        SupplierContract that = (SupplierContract) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
