package com.commerce_pro_backend.catalog.attribute.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(
    name = "attribute_options",
    indexes = {
        @Index(name = "idx_attr_option_attribute", columnList = "attribute_id"),
        @Index(name = "idx_attr_option_sort", columnList = "sort_order")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "option_value", nullable = false, length = 200)
    private String value;

    @Size(max = 200)
    @Column(length = 200)
    private String label;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Size(max = 7)
    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;
}
