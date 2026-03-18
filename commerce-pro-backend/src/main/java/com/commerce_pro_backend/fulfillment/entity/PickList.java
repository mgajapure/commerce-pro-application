package com.commerce_pro_backend.fulfillment.entity;

import com.commerce_pro_backend.fulfillment.enums.PickListStatus;
import com.commerce_pro_backend.fulfillment.enums.PickListType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pick_lists", indexes = {
        @Index(name = "idx_picklist_number",   columnList = "pick_list_number", unique = true),
        @Index(name = "idx_picklist_status",   columnList = "status"),
        @Index(name = "idx_picklist_assigned", columnList = "assigned_to_user_id"),
        @Index(name = "idx_picklist_created",  columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickList {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "pick_list_number", nullable = false, unique = true, length = 30)
    private String pickListNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PickListType type = PickListType.BATCH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PickListStatus status = PickListStatus.GENERATED;

    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    @Column(name = "warehouse_name", length = 200)
    private String warehouseName;

    @Column(name = "assigned_to_user_id", length = 36)
    private String assignedToUserId;

    @Column(name = "assigned_to_name", length = 200)
    private String assignedToName;

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_items", nullable = false)
    @Builder.Default
    private Integer totalItems = 0;

    @Column(name = "completed_items", nullable = false)
    @Builder.Default
    private Integer completedItems = 0;

    @Column(length = 1000)
    private String notes;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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

    @OneToMany(mappedBy = "pickList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sku ASC")
    @Builder.Default
    private List<PickListItem> items = new ArrayList<>();

    // ── Business logic ────────────────────────────────────────────────────────

    @PrePersist
    public void prePersist() {
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }

    public void refreshCounts() {
        this.totalItems    = items.size();
        this.completedItems = (int) items.stream().filter(PickListItem::getIsPicked).count();
    }

    public boolean isAllPicked() {
        return !items.isEmpty() && items.stream().allMatch(PickListItem::getIsPicked);
    }

    public boolean isCancellable() {
        return status == PickListStatus.GENERATED || status == PickListStatus.ASSIGNED;
    }
}
