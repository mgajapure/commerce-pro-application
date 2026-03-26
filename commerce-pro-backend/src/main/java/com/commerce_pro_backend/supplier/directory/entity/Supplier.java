package com.commerce_pro_backend.supplier.directory.entity;

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
    name = "suppliers",
    indexes = {
        @Index(name = "idx_supplier_code", columnList = "code", unique = true),
        @Index(name = "idx_supplier_active", columnList = "is_active"),
        @Index(name = "idx_supplier_preferred", columnList = "is_preferred"),
        @Index(name = "idx_supplier_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Supplier {

    @Id
    @Column(length = 36)
    private String id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Size(max = 100)
    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Size(max = 255)
    @Column(length = 255)
    private String email;

    @Size(max = 50)
    @Column(length = 50)
    private String phone;

    @Size(max = 500)
    @Column(length = 500)
    private String address;

    @Size(max = 100)
    @Column(length = 100)
    private String city;

    @Size(max = 100)
    @Column(length = 100)
    private String state;

    @Size(max = 100)
    @Column(length = 100)
    private String country;

    @Size(max = 20)
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Size(max = 500)
    @Column(length = 500)
    private String website;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Size(max = 100)
    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(nullable = false)
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "min_order_value", precision = 15, scale = 2)
    private BigDecimal minOrderValue;

    @Size(max = 10)
    @Column(name = "preferred_currency", length = 10, nullable = false)
    @Builder.Default
    private String preferredCurrency = "USD";

    @Size(max = 1000)
    @Column(length = 1000)
    private String certifications;

    @Size(max = 2000)
    @Column(length = 2000)
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_preferred", nullable = false)
    @Builder.Default
    private Boolean isPreferred = false;

    @Column(name = "product_count", nullable = false)
    @Builder.Default
    private Integer productCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    // Business methods

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void markPreferred() {
        this.isPreferred = true;
    }

    public void unmarkPreferred() {
        this.isPreferred = false;
    }

    public void incrementProductCount() {
        this.productCount++;
    }

    public void decrementProductCount() {
        if (this.productCount > 0) {
            this.productCount--;
        }
    }

    public void updateProductCount(int count) {
        this.productCount = Math.max(0, count);
    }

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (this.code != null) {
            this.code = this.code.toUpperCase().trim();
        }
        if (this.name != null) {
            this.name = this.name.trim();
        }
        if (this.website != null && !this.website.isEmpty()) {
            if (!this.website.startsWith("http://") && !this.website.startsWith("https://")) {
                this.website = "https://" + this.website;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Supplier)) return false;
        Supplier supplier = (Supplier) o;
        return id != null && Objects.equals(id, supplier.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
