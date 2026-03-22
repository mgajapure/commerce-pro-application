package com.commerce_pro_backend.finance.entity;

import com.commerce_pro_backend.finance.enums.TaxApplicability;
import com.commerce_pro_backend.finance.enums.TaxType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Configurable tax rate — supports multiple tax systems (US Sales Tax, EU VAT, GST, etc.).
 * Rate is stored as decimal: 0.085 = 8.5%
 */
@Entity
@Table(name = "tax_rates", indexes = {
    @Index(name = "idx_tr_code",   columnList = "rate_code",  unique = true),
    @Index(name = "idx_tr_type",   columnList = "tax_type"),
    @Index(name = "idx_tr_active", columnList = "is_active"),
    @Index(name = "idx_tr_jurisdiction", columnList = "jurisdiction_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TaxRate {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "rate_code", nullable = false, unique = true, length = 20)
    private String rateCode;               // e.g. "US_CA_SALES", "EU_VAT_STD"

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private TaxType taxType;

    @Column(name = "rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal rate;               // e.g. 0.085000 for 8.5%

    @Column(name = "jurisdiction_id", length = 36)
    private String jurisdictionId;

    @Column(name = "country", length = 2)
    private String country;               // ISO 3166-1 alpha-2

    @Column(name = "state_province", length = 50)
    private String stateProvince;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicability", nullable = false, length = 20)
    @Builder.Default
    private TaxApplicability applicability = TaxApplicability.ALL;

    /** True if this tax is applied on top of another base tax */
    @Column(name = "is_compound")
    @Builder.Default private Boolean isCompound = false;

    /** True if listed price already includes the tax */
    @Column(name = "is_inclusive")
    @Builder.Default private Boolean isInclusive = false;

    @Column(name = "is_active")
    @Builder.Default private Boolean isActive = true;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    // ── Business Methods ──────────────────────────────────────────────────────

    public boolean isEffective(LocalDate date) {
        if (!isActive) return false;
        if (effectiveFrom != null && date.isBefore(effectiveFrom)) return false;
        if (effectiveTo != null && date.isAfter(effectiveTo)) return false;
        return true;
    }

    /** Returns rate as percentage string e.g. "8.50%" */
    public String getRateDisplay() {
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }
}
