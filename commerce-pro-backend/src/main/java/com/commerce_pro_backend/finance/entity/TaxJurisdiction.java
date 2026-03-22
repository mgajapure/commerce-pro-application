package com.commerce_pro_backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_jurisdictions", indexes = {
    @Index(name = "idx_tj_code", columnList = "jurisdiction_code", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TaxJurisdiction {

    @Id @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "jurisdiction_code", unique = true, nullable = false, length = 20)
    private String jurisdictionCode;       // US_CA, US_NY, EU_DE, UK

    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "country", length = 2)        private String country;   // ISO 3166-1 alpha-2
    @Column(name = "state_province", length = 50) private String stateProvince;
    @Column(name = "notes", length = 500)         private String notes;

    @Column(name = "is_active")
    @Builder.Default private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;
}
