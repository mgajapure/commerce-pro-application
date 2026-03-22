package com.commerce_pro_backend.catalog.attribute.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "attributes",
    indexes = {
        @Index(name = "idx_attribute_code", columnList = "code", unique = true),
        @Index(name = "idx_attribute_active", columnList = "is_active"),
        @Index(name = "idx_attribute_sort", columnList = "sort_order")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttributeType type;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "is_filterable", nullable = false)
    @Builder.Default
    private Boolean isFilterable = false;

    @Column(name = "is_searchable", nullable = false)
    @Builder.Default
    private Boolean isSearchable = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<AttributeOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum AttributeType {
        TEXT, NUMBER, BOOLEAN, SELECT, MULTI_SELECT, COLOR, DATE
    }

    public void addOption(AttributeOption option) {
        options.add(option);
        option.setAttribute(this);
    }

    public void removeOption(AttributeOption option) {
        options.remove(option);
        option.setAttribute(null);
    }

    public void clearOptions() {
        options.forEach(o -> o.setAttribute(null));
        options.clear();
    }
}
