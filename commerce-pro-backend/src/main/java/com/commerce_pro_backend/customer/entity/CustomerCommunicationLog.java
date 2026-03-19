package com.commerce_pro_backend.customer.entity;

import com.commerce_pro_backend.customer.enums.CommunicationChannel;
import com.commerce_pro_backend.customer.enums.CommunicationDirection;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_communication_logs", indexes = {
        @Index(name = "idx_ccl_customer",  columnList = "customer_id"),
        @Index(name = "idx_ccl_channel",   columnList = "channel"),
        @Index(name = "idx_ccl_logged_at", columnList = "logged_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerCommunicationLog {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CommunicationChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private CommunicationDirection direction;

    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200) private String subject;
    @Column(columnDefinition = "TEXT") private String body;
    @Column(name = "order_id",     length = 36) private String orderId;
    @Column(name = "order_number", length = 30) private String orderNumber;
    @Column(name = "logged_by",    length = 36) private String loggedBy;
    @Column(name = "logged_by_name", length = 100) private String loggedByName;
    @Column(name = "logged_at",    nullable = false) private LocalDateTime loggedAt;
    @Column(name = "requires_follow_up", nullable = false) @Builder.Default private Boolean requiresFollowUp = false;
    @Column(name = "follow_up_at") private LocalDateTime followUpAt;
    @Column(name = "is_resolved",  nullable = false) @Builder.Default private Boolean isResolved = false;

    @CreationTimestamp @Column(name = "created_at", updatable = false, nullable = false) private LocalDateTime createdAt;

    @PrePersist protected void onPersist() { if (loggedAt == null) loggedAt = LocalDateTime.now(); }
}
