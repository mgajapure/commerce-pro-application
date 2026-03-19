package com.commerce_pro_backend.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_groups",
       indexes = @Index(name = "idx_cg_name", columnList = "name", unique = true))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerGroup {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @NotBlank @Size(max = 100) @Column(nullable = false, unique = true, length = 100) private String name;
    @Size(max = 500)           @Column(length = 500)                                  private String description;
    @Column(name = "color_hex", length = 7) @Builder.Default private String colorHex = "#6366F1";
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
    @Column(name = "discount_percentage", precision = 5, scale = 2) @Builder.Default private BigDecimal discountPercentage = BigDecimal.ZERO;
    @Column(name = "internal_notes", length = 1000) private String internalNotes;
    @Column(name = "member_count", nullable = false) @Builder.Default private Integer memberCount = 0;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @Builder.Default private List<Customer> customers = new ArrayList<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false, nullable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at", nullable = false)                    private LocalDateTime updatedAt;
    @Column(name = "created_by", updatable = false, length = 36) private String createdBy;
    @Column(name = "updated_by", length = 36)                    private String updatedBy;
}
