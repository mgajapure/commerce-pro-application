package com.commerce_pro_backend.customer.entity;

import com.commerce_pro_backend.customer.enums.AddressType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_addresses", indexes = {
        @Index(name = "idx_caddr_customer", columnList = "customer_id"),
        @Index(name = "idx_caddr_default",  columnList = "is_default")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerAddress {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "address_label", length = 50) private String addressLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    @Builder.Default private AddressType addressType = AddressType.SHIPPING;

    @NotBlank @Size(max = 200) @Column(name = "full_name",    nullable = false, length = 200) private String fullName;
    @Size(max = 30)            @Column(length = 30)                                            private String phone;
    @Size(max = 100)           @Column(name = "company",      length = 100)                   private String company;
    @NotBlank @Size(max = 300) @Column(name = "address_line1",nullable = false, length = 300) private String addressLine1;
    @Size(max = 300)           @Column(name = "address_line2",length = 300)                   private String addressLine2;
    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100)                        private String city;
    @Size(max = 100)           @Column(length = 100)                                           private String state;
    @NotBlank @Size(max = 20)  @Column(name = "postal_code",  nullable = false, length = 20)  private String postalCode;
    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100)                        private String country;

    @Column(name = "is_default",  nullable = false) @Builder.Default private Boolean isDefault  = false;
    @Column(name = "is_verified", nullable = false) @Builder.Default private Boolean isVerified = false;

    @CreationTimestamp @Column(name = "created_at", updatable = false, nullable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at", nullable = false)                    private LocalDateTime updatedAt;
}
